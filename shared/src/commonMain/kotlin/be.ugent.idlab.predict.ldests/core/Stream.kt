package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Shape.Ontology.shape
import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Rules.Companion.split
import be.ugent.idlab.predict.ldests.rdf.*
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

class Stream {

    private val configuration: Configuration
    private val shape: Shape
    // "global" lock, active while data is being added/flushed/...
    private val globalLock = Mutex()
    // "inner" lock, active while individual fragments are being updated
    private val innerLock = Mutex()
    // lock flow: data source added | data being read & processed | new fragment(s) created | done
    //  global         L            |             L               |            L            |  U
    //  inner          U            |           U & L             |          U & L          |  U
    private val fragments: MutableMap<Fragment.Rules, MutableList<Fragment>> = mutableMapOf()

    // TODO: check the rules w/ remote stream, if present, and change them here prior to publishing if necessary
    // TODO: properly function when no rules exist, making a single entry w/ empty constraints
    private val ruleSet: MutableList<Fragment.Rules>

    internal constructor(
        configuration: Configuration,
        shape: Shape,
        rules: List<Fragment.Rules>
    ) {
        this.configuration = configuration
        this.shape = shape
        this.ruleSet = rules.toMutableList()
    }

    internal constructor(
        configuration: Configuration,
        shape: Shape,
        queryUris: List<NamedNodeTerm>
    ) {
        this.configuration = configuration
        this.shape = shape
        // FIXME: use all! uris from the list
        this.ruleSet = this.shape.split(queryUris.first()).toMutableList()
    }

    suspend fun flush() = globalLock.withLock {
        log("Lock in `flush` acquired.")
        fragments.values.forEach { fragments ->
            fragments.forEach { fragment ->
                fragment.flush() /* TODO & publish */
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
    internal suspend fun findOrCreate(rules: Fragment.Rules, data: Binding) = innerLock.withLock {
        fragments
            .getOrPut(rules) { mutableListOf() }
            .filter { it.canStore(data) }
            .ifEmpty {
                // using the first sample to create a fragment id with
                val identifier = data.id()
                val new = Fragment(
                    id = "f_${identifier}_${rules.id}",
                    configuration = configuration,
                    rules = rules,
                    // let's hope the data is sorted
                    start = identifier
                )
                // FIXME: temp
                val ttl = TripleWriter {
                    shape(rules.shape, "http://example.com/my_shape".asNamedNode())
                }
                log("Created a resulting shape text for this rule set:")
                log("\n${ttl.trim()}")
                // -- //
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

    class Fragment internal constructor(
        private val id: String,
        // TODO: generic for timestamp, geospatial, ... support?
        start: Long,
        private val configuration: Configuration,
        rules: Rules,
        private val transform: (data: Binding) -> String = { rules.shape.format(it) }
    ) {

        init {
            log("Created a fragment with id `$id`")
        }

        class Rules(
            constraints: Map<NamedNodeTerm, Shape.ConstantProperty>,
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

                /**
                 * Divides the given shape into various rules that can be used to create new fragments with, so the
                 *  provided `uri` can be used to query over (e.g. constant `ex:prop` with 3 possible values, results in
                 *  3 rules dividing `ex:prop` over the three possible values, so up to 3 fragments can be made)
                 */
                fun Shape.split(vararg uris: NamedNodeTerm): List<Rules> {
                    if (uris.isEmpty()) {
                        return listOf(
                            Rules(
                                constraints = mapOf() /* no constraints */,
                                parent = this
                            )
                        )
                    }
                    // creating the initial set of rules
                    val iter = uris.iterator()
                    var uri = iter.next()
                    var constraints = (properties[uri] as Shape.ConstantProperty).values.map {
                        mapOf(uri to Shape.ConstantProperty(it))
                    }
                    // recursively split all existing fragments from the previous iteration according
                    //  to the current iteration's uri
                    while (iter.hasNext()) {
                        uri = iter.next()
                        constraints = constraints.flatMap { set ->
                            (properties[uri] as Shape.ConstantProperty).values.map { prop ->
                                set + mapOf(uri to Shape.ConstantProperty(prop))
                            }
                        }
                    }
                    return constraints.map { Rules(constraints = it, parent = this) }
                }
            }

        }

        class Resource(
            val id: String
        ) {

            var count: Int = 0
                private set

            var data: String = ""
                private set

            fun add(data: String) {
                ++count
                this.data += data
            }

        }

        // so only 1 writer/reader is working w/ this fragment's data
        private val lock = Mutex()

        // all fragment content that is COMPLETE!
        // TODO not yet published
        private val resources = mutableListOf<Resource>()
        // FIXME improve ID gen
        private var buf = generateResource()

        private val range = start .. start + configuration.window.inWholeMilliseconds

        internal val url: String = "<TODO>/$id" // combination of parent stream & id something

        /**
         * Returns true if this fragment can store the given data
         */
        internal fun canStore(data: Binding): Boolean =
            data.id() in range && (
                resources.size < configuration.resourceCount ||
                resources.any { it.count < configuration.resourceSize }
            )

        /**
         * Adds the binding to the fragment
         */
        internal suspend fun append(data: Binding) = lock.withLock {
            val result = transform(data)
            buf.add(result)
            if (buf.count == configuration.resourceSize) {
                resources.add(buf)
                buf = generateResource()
                log("Completed a buffer for fragment `${id}`. Currently ${resources.size} resources can be published")
            }
        }

        /**
         * Flushes the existing buffer so it can be published, even when it is small
         */
        internal suspend fun flush() = lock.withLock {
            if (buf.count == 0) {
                log("Nothing to flush for fragment `${id}`. Currently ${resources.size} resources can be published")
                return@withLock
            }
            resources.add(buf)
            buf = generateResource()
            log("Flushed the internal buffer for fragment `${id}`. Currently ${resources.size} resources can be published")
        }

        private fun generateResource() = Resource("ID_${resources.size}")

    }

}
