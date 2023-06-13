package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.util.InputStream

// collection of helper methods using the query method from above to collect the query result in various ways
expect class Query(
    sparql: String
) {

    val variables: Set<String>

    companion object {

        /**
         * Generic way of querying a stream (not very efficient)
         */
        suspend fun InputStream<Triple>.query(query: Query): InputStream<Binding>

        /**
         * Querying the provider (directly); most efficient way of obtaining a stream of bindings instantly
         */
        suspend fun TripleProvider.query(query: Query): InputStream<Binding>

    }

}

// not nested, as it is a typealias in the JS target
expect class Binding

// externally declared, same reason as the one above
expect operator fun Binding.get(variable: String): Term?

