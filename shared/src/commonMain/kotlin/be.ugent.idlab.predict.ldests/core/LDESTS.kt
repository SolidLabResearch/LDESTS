package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Rules.Companion.split
import be.ugent.idlab.predict.ldests.rdf.LocalResource
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LDESTS private constructor(
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
    rules: List<Stream.Fragment.Rules>,
    /**
     * Initial data that can already be published to the stream, generating the first fragment(s) if necessary
     */
    data: List<TripleProvider> = listOf()
) {

    internal object Ontology {

        // FIXME: use an actual prefix once the ontology is more official
        const val PREFIX = "https://predict.ugent.be/ldests#"

        object Stream {
            const val O_TYPE = "${PREFIX}Node"
            const val P_SHAPE = "${PREFIX}shape"
        }

        object Fragment {
            const val O_TYPE = "${PREFIX}Fragment"
            const val P_CONSTRAINTS = "${PREFIX}overrides" // TODO: get a better name here
            const val P_LUT = "${PREFIX}iris" // TODO: get a better name here
            const val P_HAS = "${PREFIX}contains"
        }

        // TODO: functions similar to `Shape.Ontology` for writing

    }

    // keeps track of all IO (local & remote) regarding this LDESTS stream, hosting the jobs of the publisher & reader
    //  as well
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)
    // lock responsible for all resources that aren't used by the stream (yet)
    private val resourceLock = Mutex()

    private val stream = Stream(
        configuration = configuration,
        shape = shape,
        rules = rules
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
        // TODO: launch jobs to submit data
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
        private val url: String /*TODO: Solid conn, url somehow or filepath/directory*/
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

        fun create(): LDESTS = shape?.let {
            LDESTS(
                configuration = configuration,
                shape = it,
                rules = it.split(*queryUris.toTypedArray())
            )
        } ?: throw Error("Invalid Builder() usage!")

    }

}