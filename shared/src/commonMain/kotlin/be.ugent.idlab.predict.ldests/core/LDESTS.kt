package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.solid.SolidPublisher
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn
import kotlinx.coroutines.flow.Flow

class LDESTS private constructor(
    /** The stream itself, constructed outside of the LDESTS as this is an async operation **/
    private val stream: Stream,
    /** Active publishers, all listening to the stream **/
    val publishers: List<Publisher>,
    /** Stream buffer, already attached **/
    private val buffer: PublishBuffer
) {

    suspend fun init() {
        publishers.forEach { it.subscribe(buffer) }
        log("Flushing publishers for the first time")
        buffer.flush()
    }

    suspend fun append(filename: String) {
        with (stream) {
            log("Appending data from file `$filename`")
            LocalResource.from(filepath = filename).insert()
        }
    }

    /**
     * Inserts data as a streaming input source
     */
    fun insert(data: Triple) {
//        input.add(data.streamify())
    }

    /**
     * Inserts multiple data entries as a streaming input source
     */
    fun insert(data: Iterable<Triple>) {
//        input.add(data.streamify())
    }

    /**
     * Inserts an entire chunk of data in one single go
     */
    suspend fun add(data: Iterable<Triple>) = with (stream) {
        LocalResource.wrap(data.toStore()).insert()
    }

    suspend fun query(
        publisher: Publisher,
        constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>,
        range: LongRange = 0 until Long.MAX_VALUE
    ): Flow<Triple> {
        // TODO: require a compatibility check similar to onAttach
        log("Executing a query for `${publisher::class.simpleName}` with ${constraints.size} constraint(s) and time range ${range.first} - ${range.last}")
        return stream.query(publisher, constraints, range)
    }

    suspend fun flush() {
        log("Flushing stream data")
        stream.flush()
        log("Flushing all attached publishers")
        buffer.flush()
    }

    fun close() {
        warn("Close called, stopping all ongoing work.")
        warn("Close: stopping all publishers.")
        publishers.forEach { it.close() }
//        warn("Stopping remaining jobs.")
//        scope.cancel()
        log("Stream has been closed.")
    }

    class Builder(
        private val name: String
    ) {

        private var configuration = Stream.Configuration()
        private var shape: Shape? = null
        private val queryUris = mutableListOf<NamedNodeTerm>()
        private val publishers = mutableListOf<Publisher>()

        fun file(filepath: String): Builder {
            /* TODO: derive shape using the triples read from this filepath if necessary instead */
            // TODO offload ^ to LDESTS itself, not builder
            return this
        }

        fun stream(ldesUrl: String): Builder {
            /* TODO: derive shape using the triples read from this stream if necessary instead */
            // TODO offload ^ to LDESTS itself, not builder
            TODO()
        }

        fun remote(url: String): Builder {
            /* TODO: derive shape using the triples read from this remote resource if necessary instead */
            // TODO offload ^ to LDESTS itself, not builder
            return this
        }

        fun shape(shape: Shape): Builder {
            // setting it no matter what
            this.shape = shape
            return this
        }

        fun queryRule(uri: NamedNodeTerm): Builder {
            this.queryUris.add(uri)
            return this
        }

        fun config(configuration: Stream.Configuration): Builder {
            this.configuration = configuration
            return this
        }

        fun attachDebugPublisher(): Builder {
            publishers.add(
                object: Publisher() {

                    override val context = RDFBuilder.Context(
                        path = "debug.local"
                    )

                    override suspend fun fetch(path: String): TripleProvider? {
                        // no compat checking relevant here
                        return null
                    }

                    override fun publish(path: String, data: RDFBuilder.() -> Unit) {
                        val str = Turtle(
                            context = context,
                            prefixes = Ontology.PREFIXES,
                            block = data
                        )
                        log("In debugger for `$path`:\n$str")
                    }

                }
            )
            return this
        }

        fun attachMemoryPublisher(): Builder {
            publishers.add(MemoryPublisher())
            return this
        }

        fun attachSolidPublisher(url: String): Builder {
            publishers.add(SolidPublisher(url = url))
            return this
        }

        suspend fun create(): LDESTS = shape?.let {
            val stream = Stream.create(
                name = name,
                configuration = configuration,
                shape = it,
                rules = it.split(*queryUris.toTypedArray())
            )
            val buf = PublishBuffer()
            stream.attach(buf)
            LDESTS(
                stream = stream,
                publishers = publishers,
                buffer = buf
            ).apply { init() }
        } ?: throw Error("Invalid Builder() usage!")

    }

}