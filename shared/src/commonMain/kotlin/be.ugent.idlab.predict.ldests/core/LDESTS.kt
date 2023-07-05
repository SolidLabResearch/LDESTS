package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.solid.SolidPublisher
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LDESTS private constructor(
    /**
     * The stream itself, constructed outside of the LDESTS as this is an async operation
     */
    private val stream: Stream,
    /**
     * (temporary) Publishers used to publish the stream to
     */
    private val publishers: List<Publisher>,
    /** Stream buffer, already attached **/
    private val buffer: PublishBuffer,
    /**
     * Initial data that can already be published to the stream, generating the first fragment(s) if necessary
     */
    data: List<TripleProvider> = listOf()
) {

    // keeps track of all IO (local & remote) regarding this LDESTS stream, hosting the jobs of the publisher & reader
    //  as well
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)
    // lock responsible for all resources that aren't used by the stream (yet)
    private val resourceLock = Mutex()

    suspend fun init() {
        publishers.forEach { it.subscribe(scope, buffer) }
    }

    fun append(filename: String) {
        scope.launch {
            with (stream) {
                log("Appending data from file `$filename`")
                resourceLock.withLock {
                    LocalResource.from(filepath = filename)
                    // TODO: other resource related work here, such as checks for compat, maybe shape extraction, ...
                }.insert()
            }
        }
    }

    suspend fun flush() {
        warn("Flush: releasing assigned resources.")
        resourceLock.lock()
        warn("Flush: waiting for the stream to finish.")
        stream.flush()
        // NOTE: it is not possible to call `job.join()` here when testing, it probably deadlocks the code due
        //  to the single threaded nature of JS (`flush` waiting on `join`, waiting in `globalscope` which waits on `flush` ?)
        warn("Flush: completed!")
        // unlocking again, so new data additions can be made
        resourceLock.unlock()
    }

    suspend fun close() {
        warn("Close called, stopping all ongoing work.")
        warn("Close: stopping all publishers.")
        publishers.forEach { it.close() }
        warn("Stopping remaining jobs.")
        job.cancelAndJoin()
        log("Stream has been closed.")
    }

    class Builder(
        private val name: String
    ) {

        private var configuration = Stream.Configuration()
        private var shape: Shape? = null
        // TODO: replace this with an instruction to initialise the stream's content based on either pre-existing
        //  data, or a configuration provided by this builder
        private var data = mutableListOf<TripleProvider>()
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

        // TODO: ability to add memory buffer

        fun attachDebugPublisher(): Builder {
            publishers.add(
                object: Publisher() {

                    override val root: String = "debug.local"

                    override suspend fun fetch(path: String): TripleProvider? {
                        // no compat checking relevant here
                        return null
                    }

                    override suspend fun publish(path: String, data: TripleBuilder.() -> Unit): Boolean {
                        val str = Turtle(prefixes = Ontology.PREFIXES, block = data)
                        log("In debugger for `$path`:\n$str")
                        return true
                    }

                }
            )
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
                // TODO: make this a build option
                publishers = publishers,
                buffer = buf
                // TODO: initial set of data providers
            ).apply { init() }
        } ?: throw Error("Invalid Builder() usage!")

    }

}