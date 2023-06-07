package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.warn

// collection of helper methods using the query method from above to collect the query result in various ways
object Query {

    object SPARQL {

        const val GET_ALL = "SELECT * WHERE { ?s ?p ?o }"

        fun GET_ALL(limit: Int) = "SELECT * WHERE { ?s ?p ?o } LIMIT $limit"

    }

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

internal expect suspend fun query(query: String, url: String, onValueReceived: (Triple) -> Unit)

internal expect suspend fun file(filename: String, onValueReceived: (Triple) -> Unit)
