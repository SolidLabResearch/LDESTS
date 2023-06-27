package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Stream.Fragment.Rules.Companion.split
import be.ugent.idlab.predict.ldests.rdf.LocalResource
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.*

class LDESTS private constructor(
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

    // keeps track of all IO (local & remote) regarding this LDESTS stream, hosting the jobs of the publisher & reader
    //  as well
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    private val stream = Stream(shape = shape, rules = rules)

    fun append(filename: String) {
        scope.launch {
            with (stream) {
                log("Appending data from file `$filename`")
                LocalResource
                    .from(
                        filepath = filename,
                        scope = scope
                    ).insert()
            }
        }
    }

    suspend fun flush() {
        job.join()
    }

    suspend fun close() {
        job.cancelAndJoin()
    }

    class Builder(
        private val url: String /*TODO: Solid conn, url somehow or filepath/directory*/
    ) {

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

        // TODO: if shape is null, try reading from the url field above to see if a stream already exists
        //  in LDESTS itself
        // TODO: read URL and see if provided shape & present shape are compatible
        suspend fun create(): LDESTS = shape?.let {
            LDESTS(shape = it, rules = it.split(*queryUris.toTypedArray()))
        } ?: throw Error("Invalid Builder() usage!")

    }

}