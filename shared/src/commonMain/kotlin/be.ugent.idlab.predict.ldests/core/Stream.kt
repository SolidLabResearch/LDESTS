package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Shape.Companion.stringified
import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Companion.createFragment
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.TREE
import be.ugent.idlab.predict.ldests.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Stream private constructor(
    name: String,
    private val configuration: Configuration,
    internal val shape: Shape,
    rules: List<Rules>
): Publishable(
    name = "$name/"
) {

    internal val rules = rules.associateBy { it.name }

    // "global" lock, active while data is being added/flushed/...
    private val globalLock = Mutex()
    // "inner" lock, active while individual fragments are being updated
    private val innerLock = Mutex()
    // lock flow: data source added | data being read & processed | new fragment(s) created | done
    //  global         L            |             L               |            L            |  U
    //  inner          U            |           U & L             |          U & L          |  U
    private val fragments: MutableMap<Rules, MutableList<Fragment>> = mutableMapOf()

    override suspend fun onPublisherAttached(publisher: Publisher): PublisherAttachResult {
        val loc = publisher.fetch(name)
            ?: return PublisherAttachResult.NEW // nothing to check against, so it's new
        val existingShape = loc.extractShapeInformation(publisher.context.run { uri })
            ?: return PublisherAttachResult.NEW // shape is either non-existent, or badly configured
        if (shape != existingShape) {
            error("Publisher shape and stream shape are incompatible! Not attaching...")
            return PublisherAttachResult.FAILURE
        }
        loc.extractRuleInformation(publisher.context.run { uri })?.let { existingRules ->
            // currently only supporting exact matches, with existing rules being a subset of configured rules, requiring matching IDs;
            // TODO later publisher-specific rule mapping can be done (matching IDs based on props)
            val match = existingRules.all { (ruleId, ruleProps) ->
                // not terribly efficient, oh well
                ruleProps.map { it.key.value to it.value.toSet() }.toSet() ==
                    rules[ruleId]?.constraints?.map { it.key.value to it.value.values.toSet() }?.toSet()
            }
            if (!match) {
                error("Not all rules are compatible between to be attached publisher and existing stream! Not attaching...")
                return PublisherAttachResult.FAILURE
            }
            if (rules.size > existingRules.size) {
                log("Appending ${rules.size - existingRules.size} new rule(s) to existing publisher")
                TODO()
            }
        }
        return PublisherAttachResult.SUCCESS
    }

    override fun RDFBuilder.onCreate(publisher: Publisher) {
        // writing the initial stream layout
        stream(this@Stream)
    }

    suspend fun flush() = globalLock.withLock {
        log("Lock in `flush` acquired.")
        fragments.values.forEach { fragments ->
            fragments.forEach { fragment ->
                fragment.flush()
            }
        }
    }

    /**
     * Inserts the triples provided by the source directly into the buffer through multiple streams, does not return a
     *  new stream as it consumes directly and blocks (suspends) until finished.
     */
    internal suspend fun TripleProvider.insert() {
        // `=` syntax would return a list of completed async calls, which would be `Unit`
        globalLock.withLock {
            // src: https://stackoverflow.com/a/55895056
            coroutineScope {
                // iterating over all existing rules, using their queries to obtain data, and using the data's timestamp
                //  to get the appropriate fragment (creating one if necessary)
                rules.values.map { rules ->
                    async {
                        query(rules.shape.query)!!
                            .consume { binding ->
                                this@coroutineScope.launch {
                                    findOrCreate(rules = rules, data = binding).forEach {
                                        it.append(binding)
                                    }
                                }
                            }.join()
                    }
                }.awaitAll()
            }
        }
    }

    /**
     * Finds all fragments satisfying the provided rules, and containing data compatible w/ the provided
     *  sample (timestamp/geospatial/...). All provided fragments should contain data that fits these
     *  parameters (rules & timestamp). Creates a new fragment that satisfies these parameters, if necessary.
     * Returns at least one fragment
     */
    private suspend fun findOrCreate(rules: Rules, data: Binding) = innerLock.withLock {
        fragments
            .getOrPut(rules) { mutableListOf() }
            .filter { it.canStore(data) }
            .ifEmpty {
                // using the first sample to create a fragment id with
                val identifier = data.id()
                val fragment = createFragment(
                    path = "$name${rules.name}_${identifier}",
                    configuration = configuration,
                    rules = rules,
                    // let's hope the data is sorted
                    start = identifier,
                    onComplete = {
                        publish(path = this.name) {
                            fragment(this@createFragment)
                        }
                        publish(path = this@Stream.name) {
                            +this@Stream.uri has TREE.relation being fragmentRelation(this@createFragment)
                        }
                        // forgetting about this fragment to save memory - reading data is always done from a publisher
                        //  anyway
                        fragments[rules]!!.remove(this)
                    }
                )
                fragments[rules]!!.add(fragment)
                listOf(fragment)
            }
    }

    suspend fun query(
        publisher: Publisher,
        constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>,
        range: LongRange = 0 until Long.MAX_VALUE
    ): Flow<Triple> = coroutineScope {
        // getting the relevant fragments first, so only these get queried
        val fragments = getFragments(publisher = publisher, constraints = constraints, range = range)
        log("Querying over fragments `${fragments.joinToString(", ") { it.name }}`")
        fragments
            .map { (name, _, rules) ->
                async {
                    var data: String? = null
                    publisher.fetch(name)
                        ?.query(FRAGMENT_CONTENTS_SPARQL_QUERY(publisher.context.run { name.absolutePath }))
                        ?.consume { data = it["data"]!!.value }
                        ?.join()
                        ?: warn("Fragment `${name}` failed to resolve!")
                    data?.let { rules.shape.decode(
                        publisher = publisher,
                        range = range,
                        constraints = constraints,
                        source = it.asIterable()
                    ) }
                }
            }
            .awaitAll()
            .filterNotNull()
            .merge()
    }

    private suspend fun getFragments(
        publisher: Publisher,
        constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>,
        range: LongRange
    ): List<Fragment.Properties> {
        // getting all relevant rules based on the provided constraints
        val relevant = rules.values.filter { it.shape.complies(constraints) }.map { it.name }
        if (relevant.isEmpty()) {
            warn("The provided constraints resulted in no queryable constraint sets!")
            return listOf()
        }
        // getting all fragments from the publisher, and seeing what fragments with the relevant rules can be used
        val available = publisher.readFragmentData()
        if (available.isEmpty()) {
            warn("The provided publisher contains no fragments to query over!")
            return listOf()
        }
        // filtering for those that are relevant
        val targets = available.filter {
            val fragmentRange = it.start .. it.start + configuration.window.inWholeMilliseconds
            it.rules.name in relevant && !(range.last < fragmentRange.first || fragmentRange.last < range.first)
        }
        if (targets.isEmpty()) {
            warn("The provided publisher contains no relevant fragments to query over!")
            warn("Available constraint sets are:")
            available.joinToString(", ") { it.rules.constraints.stringified() }.apply { warn(this) }
            warn("Available time ranges are:")
            available.joinToString(", ") { "[${it.start} .. ${it.start + configuration.window.inWholeMilliseconds}]" }
        }
        return targets
    }

    /**
     * Extracts all available fragment data from the given publisher at that point
     */
    private suspend fun Publisher.readFragmentData(): List<Fragment.Properties> = with (context) {
        val properties = mutableListOf<Fragment.Properties>()
        fetch(path = name)?.query(FRAGMENT_RELATION_SPARQL_QUERY(context.run { uri }))
            ?.consume { binding ->
                // every binding contains a fragment that simply has to map to shape information
                with (context) {
                    properties.add(
                        Fragment.Properties(
                            name = (binding["name"]!! as NamedNodeTerm).relativePath,
                            start = (binding["start"]!! as LiteralTerm).value.toLong(),
                            rules = rules[(binding["constraints"]!! as NamedNodeTerm).relativePath.substringAfterLast('/')]!!
                        )
                    )
                }
            }?.join()
            ?: warn("Tried to read fragment data, but failed to query `${this@readFragmentData::class.simpleName}`.")
        properties
    }

    // TODO: generic for ranges and stuff to support more than just timestamps?
    data class Configuration(
        /** When either constraint fail for a fragment, a new one has to be made to accommodate new data **/
        /** diff between max start and end time found in this entire fragment **/
        val window: Duration = DEFAULT_WINDOW_MIN.minutes,
        /** number of samples per resource (= number of IDs contained in a single resource) **/
        val resourceSize: Int = DEFAULT_RESOURCE_SIZE
    ) {

        companion object {

            const val DEFAULT_WINDOW_MIN = 30
            const val DEFAULT_RESOURCE_SIZE = 500

        }

    }

    class Rules(
        internal val constraints: Map<NamedNodeTerm, Shape.ConstantProperty>,
        id: String = constraints.values.joinToString("") { value ->
            value.values.first().value.retainLetters().takeLast(7).lowercase()
        },
        parent: Shape
    ): RDFBuilder.Element {

        override val name = id

        /**
         * The associated shape with this rule set.
         * Also contains the generated query, used to get all data satisfying the rules (adapted query) that were
         *  applied on the base shape (UNUSED! base query)
         */
        val shape = parent.narrow(constraints)

    }

    class Fragment private constructor(
        internal val properties: Properties,
        private val configuration: Configuration,
        private val onFinished: suspend Fragment.() -> Unit
    ): RDFBuilder.Element by properties {

        data class Properties(
            override val name: String,
            // TODO: generic for timestamp, geospatial, ... support?
            internal val start: Long,
            internal val rules: Rules
        ): RDFBuilder.Element

        private val range = properties.start .. properties.start + configuration.window.inWholeMilliseconds

        private var count: Int = 0

        private val _data = StringBuilder()
        val data: String
            get() = _data.toString()

        init {
            log("Created a fragment with id `$name`")
        }

        fun add(data: String) {
            ++count
            this._data.append(data)
        }

        val shape: Shape
            get() = properties.rules.shape

        // so only 1 writer/reader is working w/ this fragment's data
        private val lock = Mutex()

        /**
         * Returns true if this fragment can store the given data
         */
        internal suspend fun canStore(data: Binding): Boolean = lock.withLock {
            data.id() in range && count < configuration.resourceSize
        }

        /**
         * Adds the binding to the fragment
         */
        internal suspend fun append(data: Binding) = lock.withLock {
            val result = properties.rules.shape.encode(data)
            add(result)
            if (count >= configuration.resourceSize) {
                onFinished()
            }
        }

        /**
         * Flushes the existing buffer, so it can be published, even when it is small
         */
        internal suspend fun flush() = lock.withLock {
            if (count == 0) {
                return@withLock
            }
            onFinished()
        }

        companion object {

            suspend fun Stream.createFragment(
                path: String,
                start: Long,
                configuration: Configuration,
                rules: Rules,
                onComplete: suspend Fragment.() -> Unit
            ): Fragment {
                return Fragment(
                    properties = Properties(
                        name = path,
                        start = start,
                        rules = rules
                    ),
                    configuration = configuration,
                    onFinished = onComplete
                )
            }

        }

    }

    companion object {

        suspend fun create(
            shape: Shape,
            rules: List<Rules>,
            name: String = "${shape.typeIdentifier.value.value.takeLast(10)}-stream",
            configuration: Configuration = Configuration()
        ): Stream {
            // TODO: builder scope for these various options
            // TODO: necessary fetches to the remote host to get relevant data
            //  maybe get the publisher here as an argument, as this can supply this information
            return Stream(
                name = name,
                configuration = configuration,
                shape = shape,
                rules = rules
            )
        }

        suspend fun TripleProvider.extractRuleInformation(stream: NamedNodeTerm) =
            query(CONSTRAINT_SPARQL_QUERY_FOR_STREAM(stream))
                ?.consumeAsRuleData()
                ?.mapKeys { it.key.removePrefix(stream.value) }

        suspend fun TripleProvider.extractShapeInformation(stream: NamedNodeTerm) =
            query(SHAPE_SPARQL_QUERY_FOR_STREAM(stream))?.consumeAsShapeInformation()

    }

}
