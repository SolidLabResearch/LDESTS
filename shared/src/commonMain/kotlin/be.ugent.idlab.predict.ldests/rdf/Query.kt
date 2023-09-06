package be.ugent.idlab.predict.ldests.rdf

// TODO: add optimisation strategies for querying a stream
//  possible approach: analyse all constants, and have a shape w/ all constraint sets result in a subset
//  of these constraint sets to query (and possibly time based constraints)
data class Query(
    val sparql: String
)

// not nested, as it is a typealias in the JS target
expect class Binding {
    fun toPrettyString(): String
}

// externally declared, same reason as the one above
expect operator fun Binding.get(variable: String): Term?

/**
 * Querying the provider (directly); most efficient way of obtaining a stream of bindings instantly. The callback is
 *  possibly never invoked if the provider is either invalid (e.g. existing but non-rdf file), or contains
 *  no (accessible) resources (remote)
 */
expect suspend fun TripleProvider.query(sparql: String, callback: (Binding) -> Unit)

