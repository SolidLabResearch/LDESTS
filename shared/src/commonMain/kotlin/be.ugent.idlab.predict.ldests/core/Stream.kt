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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
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
        log("Found compatible pre-existing stream on publisher `${publisher::class.simpleName}`, continuing")
        return PublisherAttachResult.SUCCESS
    }

    override fun RDFBuilder.onCreate(publisher: Publisher) {
        // writing the initial stream layout
        stream(this@Stream)
    }

    fun flush() {
        log("Flushing ${fragments.values.size} fragment(s)...")
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
        // src: https://stackoverflow.com/a/55895056
        coroutineScope {
            // iterating over all existing rules, using their queries to obtain data, and using the data's timestamp
            //  to get the appropriate fragment (creating one if necessary)
            rules.values.map { rules ->
                async {
                    query(rules.shape.query) { binding ->
                        this@coroutineScope.launch {
                            findOrCreate(rules = rules, data = binding).forEach {
                                it.append(binding)
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Finds all fragments satisfying the provided rules, and containing data compatible w/ the provided
     *  sample (timestamp/geospatial/...). All provided fragments should contain data that fits these
     *  parameters (rules & timestamp). Creates a new fragment that satisfies these parameters, if necessary.
     * Returns at least one fragment
     */
    private fun findOrCreate(rules: Rules, data: Binding) =
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
                        if (filled()) {
                            // forgetting about this fragment to save memory - reading data is always done from a publisher
                            //  anyway
                            fragments[rules]!!.remove(this)
                        }
                    }
                )
                // publishing the fragment relation as well
                publish(path = this@Stream.name) {
                    +this@Stream.uri has TREE.relation being fragmentRelation(fragment)
                }
                fragments[rules]!!.add(fragment)
                listOf(fragment)
            }

    suspend fun query(
        publisher: Publisher,
        query: Query,
        callback: (Binding) -> Unit
    ) {
        // TODO: apply query specific optimisations with `getFragments`
//        val fragments = getFragments(publisher = publisher, constraints = constraints, range = range)
        // getting the relevant fragments first, so only these get queried
        val fragments = publisher.readFragmentData()
        log("Querying over fragments `${fragments.joinToString(", ") { it.name }}`")
        coroutineScope {
            fragments
                .map { (name, _, rules) ->
                    async {
                        var data: String? = null
                        publisher.fetch(name)
                            ?.query(FRAGMENT_CONTENTS_SPARQL_QUERY(publisher.context.run { name.absolutePath })) {
                                data = it["data"]!!.value
                            }
                            ?: warn("Fragment `${name}` failed to resolve!")
                        // TODO: apply query specific optimisations with `shape.decode`
                        data?.let { rules.shape.decode(
                            publisher = publisher,
                            fragmentId = name,
                            range = 0L..Long.MAX_VALUE,
                            constraints = emptyMap(),
                            source = it.asIterable()
                        ) }
//                        data?.let { rules.shape.decode(
//                            publisher = publisher,
//                            range = range,
//                            constraints = constraints,
//                            source = it.asIterable()
//                        ) }
                    }
                }
                .awaitAll()
                .filterNotNull()
                .merge()
        }
            .toAsyncResource()
            .query(query.sparql, callback)
    }

    private suspend fun getFragments(
        publisher: Publisher,
        constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>,
        range: LongRange
    ): List<Fragment.Properties> {
        // getting all relevant rules based on the provided constraints
        // TODO: this can later be replaced with a publisher based fetch so constraint sets based on a per-publisher basis
        //  can be properly respected
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
        fetch(path = name)
            ?.query(
                FRAGMENT_RELATION_SPARQL_QUERY(context.run { uri })
            ) { binding ->
                // every binding contains a fragment that simply has to map to shape information
                properties.add(
                    Fragment.Properties(
                        name = (binding["name"]!! as NamedNodeTerm).relativePath,
                        start = (binding["start"]!! as LiteralTerm).value.toLong(),
                        rules = rules[(binding["constraints"]!! as NamedNodeTerm).relativePath.substringAfterLast('/')]!!
                    )
                )
            }
            ?: warn("Tried to read fragment data, but failed to query path `$name` on `${this@readFragmentData::class.simpleName}`.")
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
        private val onFinished: Fragment.() -> Unit
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

        fun filled() = count >= configuration.resourceSize

        val shape: Shape
            get() = properties.rules.shape

        /**
         * Returns true if this fragment can store the given data
         */
        internal fun canStore(data: Binding) = data.id() in range && !filled()

        /**
         * Adds the binding to the fragment
         */
        internal fun append(data: Binding) {
            val result = properties.rules.shape.encode(data)
            add(result)
            if (count >= configuration.resourceSize) {
                onFinished()
            }
        }

        /**
         * Flushes the existing buffer, so it can be published, even when it is small
         */
        internal fun flush() {
            if (count == 0) {
                warn("Nothing to flush for fragment `${properties.name}`.")
                return
            }
            onFinished()
        }

        companion object {

            internal fun createFragment(
                path: String,
                start: Long,
                configuration: Configuration,
                rules: Rules,
                onComplete: Fragment.() -> Unit
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

        fun create(
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
            retrieveRuleData(stream).mapKeys { it.key.removePrefix(stream.value) }

        suspend fun TripleProvider.extractShapeInformation(stream: NamedNodeTerm) =
            retrieveShapeData(stream)

    }

}
