package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Stream.Companion.split
import be.ugent.idlab.predict.ldests.rdf.LocalResource
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.solid.SolidPublisher
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LDESTS private constructor(
    /**
     * The publishing URL, also used to form the stream uri
     */
    url: String,
    /**
     * The name of the stream, combined with the URL to form the URI
     */
    name: String,
    /**
     * Stream-specific configuration
     */
    configuration: Stream.Configuration,
    /**
     * The generic "global" shape, gets converted to individual SPARQL-queries after applying fragment-based
     * shape properties
     */
    shape: Shape,
    /**
     * The rules for creating new fragments
     */
    rules: List<Stream.Rules>,
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

    private val stream = Stream(
        publisher = SolidPublisher(url),
        name = name,
        configuration = configuration,
        shape = shape,
        rules = rules.toMutableList()
    )

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
        job.cancelAndJoin()
        log("Stream has been closed.")
    }

    class Builder(
        private val name: String,
        private val url: String /* TODO: identification of publisher type */
    ) {

        private var configuration = Stream.Configuration()
        private var shape: Shape? = null
        // TODO: replace this with an instruction to initialise the stream's content based on either pre-existing
        //  data, or a configuration provided by this builder
        private var data = mutableListOf<TripleProvider>()
        private val queryUris = mutableListOf<NamedNodeTerm>()

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

        suspend fun create(): LDESTS = shape?.let {
            val ldests = LDESTS(
                name = name,
                url = url,
                configuration = configuration,
                shape = it,
                rules = it.split(*queryUris.toTypedArray())
            )
            // TODO: better init here
            ldests.stream.init()
            ldests
        } ?: throw Error("Invalid Builder() usage!")

    }

}