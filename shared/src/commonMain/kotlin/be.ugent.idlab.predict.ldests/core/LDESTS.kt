package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LDESTS private constructor(
    /** The stream itself, constructed outside of the LDESTS as this is an async operation **/
    private val stream: Stream,
    /** Active publishers, all listening to the stream **/
    val publishers: List<Publisher>,
    /** Stream buffer, already attached **/
    private val buffer: PublishBuffer
) {

    // lock used to guarantee that `flush`ing only occurs when there are no additions being made,
    //  and additions are never overlapping
    private val lock = Mutex()
    // internal stream instance used to gather all "loose" input triples and insert them directly accordingly
    private val input = StreamingResource()
    private lateinit var inputJob: Job

    suspend fun init() {
        publishers.forEach { it.subscribe(buffer) }
        log("Flushing publishers for the first time")
        buffer.flush()
        log("Attaching the input stream instance to the stream")
        // starting this job in another separate scope, as the `init()` block is called within the builder's context and
        //  not the actual stream's lifetime
        inputJob = CoroutineScope(Dispatchers.Unconfined).launch {
            with (stream) {
                input.insert()
            }
        }
    }

    suspend fun append(filename: String) {
        log("Acquiring lock to insert data from file into the stream")
        lock.withLock {
            with (stream) {
                log("Appending data from file `$filename`")
                LocalResource.from(filepath = filename).insert()
            }
        }
    }

    /**
     * Inserts data as a streaming input source
     */
    fun insert(data: Triple) {
        input.add(data)
    }

    /**
     * Inserts multiple data entries as a streaming input source
     */
    fun insert(data: Iterable<Triple>) {
        data.forEach { input.add(it) }
    }

    /**
     * Inserts an entire chunk of data as a standalone "file" to the stream
     */
    suspend fun add(data: TripleStore) = with (stream) {
        log("Acquiring lock to insert data as a store into the stream")
        lock.withLock {
            LocalResource.wrap(data).insert()
        }
    }

    suspend fun query(publisher: Publisher, query: Query, callback: (Binding) -> Unit) {
        // TODO: require a compatibility check similar to onAttach
        return stream.query(publisher, query, callback)
    }

    suspend fun flush() {
        log("Acquiring the lock prior to flushing the data")
        lock.withLock {
            log("Flushing stream data")
            stream.flush()
            log("Flushing all attached publishers")
            buffer.flush()
        }
    }

    suspend fun close() {
        log("Close called, waiting until the last job has finished")
        lock.withLock {
            log("Ending manual triple insertion streams")
            input.close()
            inputJob.join()
            log("Close: stopping all publishers.")
            publishers.forEach { it.close() }
            log("Stream has been closed.")
        }
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

        fun attach(publisher: Publisher): Builder {
            publishers.add(publisher)
            return this
        }

        suspend fun build(): LDESTS = shape?.let {
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