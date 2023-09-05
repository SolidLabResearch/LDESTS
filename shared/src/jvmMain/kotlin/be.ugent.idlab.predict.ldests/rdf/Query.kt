package be.ugent.idlab.predict.ldests.rdf

actual class Binding(internal val values: Map<String, Term>) {

    actual fun toPrettyString(): String = values
        .asIterable()
        .joinToString(
            prefix = "[",
            postfix = "]",
            transform = { (name, value) -> "$name = `$value`" }
        )

}

actual operator fun Binding.get(variable: String) = values[variable]


actual suspend fun TripleProvider.query(query: Query, callback: (Binding) -> Unit) {
    TODO("Not yet implemented")
}
