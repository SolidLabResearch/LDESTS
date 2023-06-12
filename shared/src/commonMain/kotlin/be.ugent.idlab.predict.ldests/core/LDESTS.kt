package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.Query.Companion.query
import be.ugent.idlab.predict.ldests.util.consume
import be.ugent.idlab.predict.ldests.util.join
import kotlinx.coroutines.*

class LDESTS private constructor(
    /* The generic "global" shape, gets converted to individual SPARQL-queries after applying fragment-based shape properties */
    private val shape: TripleStore,
    data: List<TripleProvider> = listOf()
) {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    private val buffer = TripleStore()

    init {
        // TODO: go through data, query these sources based on fragment queries
    }

    fun append(filename: String) {
        scope.launch {
            LocalResource.from(filename)
                .query(Query("SELECT * WHERE { ?s a ?o }"))
                .consume { println("Found obj ${it["s"]?.value} with type ${it["o"]?.value}") }
                .join()
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

        private var shape: TripleStore? = null
        private var data = mutableListOf<TripleProvider>()

        fun file(filepath: String): Builder {
            /* TODO: derive shape using the triples read from this filepath if necessary instead */
            shape = TripleStore()
            data.add(LocalResource.from(filepath))
            return this
        }

        fun stream(ldesUrl: String): Builder {
            /* TODO: derive shape using the triples read from this stream if necessary instead */
            shape = TripleStore()
            TODO()
        }

        fun remote(url: String): Builder {
            /* TODO: derive shape using the triples read from this remote resource if necessary instead */
            shape = TripleStore()
            data.add(RemoteResource.from(url))
            return this
        }

        fun shape(shape: TripleStore): Builder {
            // setting it no matter what
            this.shape = shape
            return this
        }

        // TODO: if shape is null, try reading from the url field above to see if a stream already exists
        // TODO: read URL and see if provided shape & present shape are compatible
        suspend fun create(): LDESTS = shape?.let { LDESTS(shape = it) } ?: throw Error("Invalid Builder() usage!")

    }

}