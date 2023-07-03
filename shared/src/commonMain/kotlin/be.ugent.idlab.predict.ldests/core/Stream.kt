package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Companion.createFragment
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.SHACL
import be.ugent.idlab.predict.ldests.rdf.ontology.SHAPETS
import be.ugent.idlab.predict.ldests.rdf.ontology.TREE
import be.ugent.idlab.predict.ldests.util.consume
import be.ugent.idlab.predict.ldests.util.join
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.retainLetters
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
    rules: List<Rules>
): Publishable(
    path = "$name/"
) {

    // TODO: check the rules w/ remote stream, if present, and change them here prior to publishing if necessary
    // TODO: properly function when no rules exist, making a single entry w/ empty constraints
    private val ruleSet = rules.toMutableList()
    // "global" lock, active while data is being added/flushed/...
    private val globalLock = Mutex()
    // "inner" lock, active while individual fragments are being updated
    private val innerLock = Mutex()
    // lock flow: data source added | data being read & processed | new fragment(s) created | done
    //  global         L            |             L               |            L            |  U
    //  inner          U            |           U & L             |          U & L          |  U
    private val fragments: MutableMap<Rules, MutableList<Fragment>> = mutableMapOf()

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
            ruleSet.map { rules ->
                async {
                    query(rules.shape.query)
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
                val new = createFragment(
                    name = "${rules.id}_${identifier}",
                    configuration = configuration,
                    rules = rules,
                    // let's hope the data is sorted
                    start = identifier
                )
                publish {
                    +this@Stream.uri has TREE.relation being fragmentRelation(new)
                }
                fragments[rules]!!.add(new)
                listOf(new)
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
        parent: Shape
    ) {

        /**
         * The associated shape with this rule set.
         * Also contains the generated query, used to get all data satisfying the rules (adapted query) that were
         *  applied on the base shape (UNUSED! base query)
         */
        val shape = parent.narrow(constraints)

        val id = constraints.values.joinToString("") { value ->
            value.values.first().value.retainLetters().takeLast(7).lowercase()
        }

        companion object {

            internal fun Turtle.constraint(constraint: Map.Entry<NamedNodeTerm, Shape.ConstantProperty>) = blank {
                +SHACL.path being constraint.key
                +SHAPETS.constantValue being list(constraint.value.values)
            }

        }

    }

    class Fragment private constructor(
        stream: Stream,
        name: String,
        // TODO: generic for timestamp, geospatial, ... support?
        internal val start: Long,
        private val configuration: Configuration,
        internal val rules: Rules
    ): Publishable(
        path = "${stream.path}$name/",
        target = stream.target
    ) {

        init {
            log("Created a fragment with id `$name`")
        }

        val shape: Shape
            get() = rules.shape

        class Resource(
            fragment: Fragment,
            name: String
        ): Publishable(
            path = "${fragment.path}$name",
            target = fragment.target
        ) {

            var count: Int = 0
                private set

            var data: String = ""
                private set

            fun add(data: String) {
                ++count
                this.data += data
            }

            suspend fun publish() = publish {
                resource(this@Resource)
            }

        }

        // so only 1 writer/reader is working w/ this fragment's data
        private val lock = Mutex()

        // all fragment content that is COMPLETE!
        private var resourceCount = 0
        // FIXME improve ID gen
        private var buf = generateResource()

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

        /**
         * Flushes the existing buffer so it can be published, even when it is small
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
                name: String,
                start: Long,
                configuration: Configuration,
                rules: Rules
            ): Fragment {
                val fragment = Fragment(
                    stream = this,
                    name = name,
                    start = start,
                    configuration = configuration,
                    rules = rules
                )
                with (fragment) {
                    publish { fragment(this@with) }
                }
                return fragment
            }

        }

        private fun generateResource() = Resource(fragment = this, name = "ID_${resourceCount++}")

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
            val stream = Stream(
                name = name,
                configuration = configuration,
                shape = shape,
                rules = rules
            )
            // we have to publish the stream once, future fragments are added through LDP automatically
            with (stream) {
                publish {
                    stream(this@with)
                }
            }
            return stream
        }

    }

}
