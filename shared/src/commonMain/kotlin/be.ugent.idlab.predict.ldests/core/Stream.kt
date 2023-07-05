package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Companion.createFragment
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.TREE
import be.ugent.idlab.predict.ldests.util.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class Stream private constructor(
    name: String,
    private val configuration: Configuration,
    internal val shape: Shape,
    internal val rules: List<Rules>
): Publishable(
    path = "$name/"
) {

    // "global" lock, active while data is being added/flushed/...
    private val globalLock = Mutex()
    // "inner" lock, active while individual fragments are being updated
    private val innerLock = Mutex()
    // lock flow: data source added | data being read & processed | new fragment(s) created | done
    //  global         L            |             L               |            L            |  U
    //  inner          U            |           U & L             |          U & L          |  U
    private val fragments: MutableMap<Rules, MutableList<Fragment>> = mutableMapOf()

    override suspend fun onPublisherAttached(publisher: Publisher): PublisherAttachResult {
        val loc = publisher.fetch(path)
            ?: return PublisherAttachResult.NEW // nothing to check against, so it's new
        val existingShape = loc.extractShapeInformation(with (publisher) { uri })
            ?: return PublisherAttachResult.NEW // shape is either non-existent, or badly configured
        if (shape != existingShape) {
            error("Publisher shape and stream shape are incompatible! Not attaching...")
            return PublisherAttachResult.FAILURE
        }
        loc.extractRuleInformation(with (publisher) { uri })?.let { existingRules ->
            // currently only supporting exact matches, with existing rules being a subset of configured rules, requiring matching IDs;
            // TODO later publisher-specific rule mapping can be done (matching IDs based on props)
            val match = existingRules.all { (ruleId, ruleProps) ->
                // not terribly efficient, oh well
                ruleProps.map { it.key.value to it.value.toSet() }.toSet() ==
                    rules.find { it.id == ruleId }?.constraints?.map { it.key.value to it.value.values.toSet() }?.toSet()
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
        // TODO: only publish when NEW (do this somewhere else as this func already returns the status?)
        return PublisherAttachResult.SUCCESS
    }

    override fun TripleBuilder.onCreate(publisher: Publisher) {
        // writing the initial stream layout
        stream(publisher, this@Stream)
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
    internal suspend fun TripleProvider.insert() = globalLock.withLock {
        // src: https://stackoverflow.com/a/55895056
        coroutineScope {
            // iterating over all existing rules, using their queries to obtain data, and using the data's timestamp
            //  to get the appropriate fragment (creating one if necessary)
            rules.map { rules ->
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
                    path = "$path${rules.id}_${identifier}/",
                    configuration = configuration,
                    rules = rules,
                    // let's hope the data is sorted
                    start = identifier
                )
                // attaching the stream's buffer (if any)
                buffer?.let { fragment.attach(it) } ?: warn("New fragment was created while no buffer is attached!")
                publish { publisher ->
                    with (publisher) {
                        +this@Stream.uri has TREE.relation being fragmentRelation(publisher, fragment)
                    }
                }
                fragments[rules]!!.add(fragment)
                listOf(fragment)
            }
    }

    // TODO: generic for ranges and stuff to support more than just timestamps?
    data class Configuration(
        /** When either constraint fail for a fragment, a new one has to be made to accommodate new data **/
        /** diff between max start and end time found in this entire fragment **/
        val window: Duration = DEFAULT_WINDOW_MIN.minutes,
        /** number of samples per resource (= number of IDs contained in a single resource) **/
        val resourceSize: Int = DEFAULT_RESOURCE_SIZE,
        /** total amount of resources per fragment **/
        val resourceCount: Int = DEFAULT_RESOURCE_COUNT
        // total amount of samples per fragment = size * count
    ) {

        companion object {

            const val DEFAULT_WINDOW_MIN = 30
            const val DEFAULT_RESOURCE_SIZE = 500
            const val DEFAULT_RESOURCE_COUNT = 10

        }

    }

    class Rules(
        internal val constraints: Map<NamedNodeTerm, Shape.ConstantProperty>,
        val id: String = constraints.values.joinToString("") { value ->
            value.values.first().value.retainLetters().takeLast(7).lowercase()
        },
        parent: Shape
    ) {

        /**
         * The associated shape with this rule set.
         * Also contains the generated query, used to get all data satisfying the rules (adapted query) that were
         *  applied on the base shape (UNUSED! base query)
         */
        val shape = parent.narrow(constraints)

    }

    class Fragment private constructor(
        path: String,
        // TODO: generic for timestamp, geospatial, ... support?
        internal val start: Long,
        private val configuration: Configuration,
        internal val rules: Rules,
        private var buf: Resource
    ): Publishable(path = path) {

        init {
            log("Created a fragment with id `$path`")
        }

        val shape: Shape
            get() = rules.shape

        class Resource(
            path: String
        ): Publishable(path = path) {

            var count: Int = 0
                private set

            var data: String = ""
                private set

            fun add(data: String) {
                ++count
                this.data += data
            }

            suspend fun publish() = publish {
                resource(it, this@Resource)
            }

        }

        // so only 1 writer/reader is working w/ this fragment's data
        private val lock = Mutex()

        // all fragment content that is COMPLETE!
        private var resourceCount = 0

        private val range = start .. start + configuration.window.inWholeMilliseconds

        /**
         * Returns true if this fragment can store the given data
         */
        internal suspend fun canStore(data: Binding): Boolean = lock.withLock {
            data.id() in range && (
                resourceCount < configuration.resourceCount ||
                buf.count < configuration.resourceSize
            )
        }

        /**
         * Adds the binding to the fragment
         */
        internal suspend fun append(data: Binding) = lock.withLock {
            val result = rules.shape.format(data)
            buf.add(result)
            if (buf.count == configuration.resourceSize) {
                buf.publish()
                // creating a new resource buffer (TODO: more efficient clearing to reuse memory would be better later)
                buf = generateResource()
            }
        }

        override suspend fun onPublisherAttached(publisher: Publisher): PublisherAttachResult {
            // TODO: actual checking
            log("Checking stream compatibility with publisher")
            publisher.fetch(path)
            publisher.publish(path) {
                fragment(publisher, this@Fragment)
            }
            return PublisherAttachResult.SUCCESS
        }

        /**
         * Flushes the existing buffer, so it can be published, even when it is small
         */
        internal suspend fun flush() = lock.withLock {
            if (buf.count == 0) {
                return@withLock
            }
            buf.publish()
            buf = generateResource()
        }

        companion object {

            suspend fun Stream.createFragment(
                path: String,
                start: Long,
                configuration: Configuration,
                rules: Rules
            ): Fragment {
                return Fragment(
                    path = path,
                    start = start,
                    configuration = configuration,
                    rules = rules,
                    buf = generateResource(path, 0, buffer)
                )
            }

            private suspend fun generateResource(
                location: String,
                id: Int,
                buffer: PublishBuffer?
            ): Resource {
                return Resource(
                    path = "${location}ID_${id}"
                ).apply {
                    buffer?.let { attach(it) } ?: warn("A new resource for a fragment was created while no buffer was attached!")
                }
            }

        }

        private suspend fun generateResource() = Companion.generateResource(path, ++resourceCount, buffer)

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
