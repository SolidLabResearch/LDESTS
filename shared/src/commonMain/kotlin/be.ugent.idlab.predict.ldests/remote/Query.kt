package be.ugent.idlab.predict.ldests.remote

import be.ugent.idlab.predict.ldests.log
import be.ugent.idlab.predict.ldests.warn

interface Triple {
    val s: String
    val p: String
    val o: String
}

object Queries {

    const val SPARQL_GET_ALL = "SELECT * WHERE { ?s ?p ?o }"

    fun SPARQL_GET_ALL(limit: Int) = "SELECT * WHERE { ?s ?p ?o } LIMIT $limit"

}

internal expect suspend fun query(query: String, url: String, onValueReceived: (Triple) -> Unit)

// collection of helper methods using the query method from above to collect the query result in various ways
object Query {

    suspend fun queryCatching(query: String, url: String): List<Triple> {
        log("Querying '$url'")
        val result = mutableListOf<Triple>()
        try {
            query(
                query = query,
                url = url,
                onValueReceived = { result.add(it) }
            )
        } catch (e: Exception) {
            warn("Caught an exception during this query!")
            warn(e)
            if (result.isNotEmpty()) {
                warn("Stream is incomplete, but not empty.")
            }
        }
        return result
    }

}

