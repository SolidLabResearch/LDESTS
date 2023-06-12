package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.LocalResource
import be.ugent.idlab.predict.ldests.rdf.Query
import be.ugent.idlab.predict.ldests.rdf.Query.Companion.query
import be.ugent.idlab.predict.ldests.rdf.TripleStore
import be.ugent.idlab.predict.ldests.rdf.get
import be.ugent.idlab.predict.ldests.util.consume
import be.ugent.idlab.predict.ldests.util.join
import kotlinx.coroutines.*

class LDESTS {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    private val buffer = TripleStore()

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

}