package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Companion.id
import be.ugent.idlab.predict.ldests.core.Shape.Companion.shape
import be.ugent.idlab.predict.ldests.core.Stream.Rules.Companion.constraint
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.*
import be.ugent.idlab.predict.ldests.rdf.ontology.LDESTS
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

class Stream internal constructor(
    private val publisher: Publisher,
    internal val name: String,
    private val configuration: Configuration,
    private val shape: Shape,
    // TODO: check the rules w/ remote stream, if present, and change them here prior to publishing if necessary
    // TODO: properly function when no rules exist, making a single entry w/ empty constraints
    rules: List<Rules>
) {

    val url = "${publisher.root}$name/"
    private val ruleSet = rules.toMutableList()
    // "global" lock, active while data is being added/flushed/...
    private val globalLock = Mutex()
    // "inner" lock, active while individual fragments are being updated
    private val innerLock = Mutex()
    // lock flow: data source added | data being read & processed | new fragment(s) created | done
    //  global         L            |             L               |            L            |  U
    //  inner          U            |           U & L             |          U & L          |  U
    private val fragments: MutableMap<Rules, MutableList<Fragment>> = mutableMapOf()

    internal constructor(
        publisher: Publisher,
        name: String,
        configuration: Configuration,
        shape: Shape,
        queryUris: List<NamedNodeTerm>
    ): this(
        publisher = publisher,
        name = name,
        configuration = configuration,
        shape = shape,
        // FIXME: use all! uris from the list
        rules = shape.split(queryUris)
    )

    // TODO: ctors through comp obj, suspend there, along with remote config sync/check
    suspend fun init() {
        // we have to publish the stream once, future fragments are added through LDP automatically
        with (publisher) {
            publish()
        }
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
                val new = Fragment(
                    name = "f_${identifier}_${rules.id}",
                    configuration = configuration,
                    rules = rules,
                    // let's hope the data is sorted
                    start = identifier
                )
                new.init()
                with (publisher) {
                    publish {
                        +this@Stream.url has TREE.relation being fragmentRelation(new)
                    }
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

    inner class Fragment internal constructor(
        internal val name: String,
        // TODO: generic for timestamp, geospatial, ... support?
        internal val start: Long,
        private val configuration: Configuration,
        internal val rules: Rules
    ) {

        val url = "${this@Stream.url}$name/"

        init {
            log("Created a fragment with id `$name`")
        }

        val shape: Shape
            get() = rules.shape

        // TODO: maybe make this a sealed type, so a SparseResource (or similar) denoting a remote-only resource
        //  can exist as well; this way, published data can be kept in memory through IDs/URIs only for updating the
        //  fragment metadata, w/o keeping the resource's data in memory/locally (sealed might even be overkill, publish
        //  can keep in memory too)
        inner class Resource(
            name: String
        ) {

            val url = "${this@Fragment.url}$name"

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
        private var resourceCount = 0
        // FIXME improve ID gen
        private var buf = generateResource()

        private val range = start .. start + configuration.window.inWholeMilliseconds

        // TODO: through comp obj init suspend func
        suspend fun init() {
            with (publisher) {
                publish()
            }
        }

        /**
         * Returns true if this fragment can store the given data
         */
        internal fun canStore(data: Binding): Boolean =
            data.id() in range && (
                  resourceCount < configuration.resourceCount ||
                  buf.count < configuration.resourceSize
            )

        /**
         * Adds the binding to the fragment
         */
        internal suspend fun append(data: Binding) = lock.withLock {
            val result = rules.shape.format(data)
            buf.add(result)
            if (buf.count == configuration.resourceSize) {
                // TODO: see note about resources above (make it sparse here, `publish` needs to keep enough in memory
                //  to succeed)
                with (publisher) {
                    buf.publish()
                }
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
            with (publisher) {
                buf.publish()
            }
            buf = generateResource()
        }

        private fun generateResource() = Resource("ID_${resourceCount++}")

    }

    companion object {

        /**
         * Divides the given shape into various rules that can be used to create new fragments with, so the
         *  provided `uri` can be used to query over (e.g. constant `ex:prop` with 3 possible values, results in
         *  3 rules dividing `ex:prop` over the three possible values, so up to 3 fragments can be made)
         */
        fun Shape.split(vararg uris: NamedNodeTerm) = split(uris.toList())

        /**
         * Divides the given shape into various rules that can be used to create new fragments with, so the
         *  provided `uri` can be used to query over (e.g. constant `ex:prop` with 3 possible values, results in
         *  3 rules dividing `ex:prop` over the three possible values, so up to 3 fragments can be made)
         */
        fun Shape.split(uris: Collection<NamedNodeTerm>): List<Rules> {
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

        fun Turtle.stream(stream: Stream) {
            // creating the shape first, in the same document for now
            val shape = "${stream.url}shape".asNamedNode()
            shape(subject = shape, shape = stream.shape)
            val subj = stream.url.asNamedNode()
            // associating the shape from above with the stream here
            +subj has RDF.type being TREE.Node
            +subj has RDF.type being LDESTS.StreamType
            +subj has LDESTS.shape being shape
        }

        internal fun Turtle.fragmentRelation(fragment: Fragment) = blank {
            +RDF.type being TREE.GreaterThanOrEqualToRelation // FIXME other relations? custom relation?
            +TREE.value being fragment.start
            +TREE.path being fragment.shape.sampleIdentifier.predicate
            +TREE.node being fragment.url.asNamedNode()
        }

        fun Turtle.fragment(fragment: Fragment) {
            val subj = fragment.url.asNamedNode()
            +subj has RDF.type being LDESTS.FragmentType
            fragment.rules.constraints.forEach {
                +subj has LDESTS.constraints being constraint(it)
            }
        }

        fun Turtle.resource(resource: Fragment.Resource) {
            val subj = resource.url.asNamedNode()
            +subj has RDF.type being LDESTS.ResourceType
            +subj has LDESTS.contents being resource.data.asLiteral()
        }

    }

}
