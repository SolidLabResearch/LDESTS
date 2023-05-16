package be.ugent.idlab.predict.ldests.remote

interface Triple {
    val s: String
    val p: String
    val o: String
}

object Queries {

    const val SPARQL_GET_ALL = "SELECT * WHERE { ?s ?p ?o }"

    fun SPARQL_GET_ALL(limit: Int) = "SELECT * WHERE { ?s ?p ?o } LIMIT $limit"

}


expect suspend fun query(query: String, url: String, onValueReceived: (Triple) -> Unit)