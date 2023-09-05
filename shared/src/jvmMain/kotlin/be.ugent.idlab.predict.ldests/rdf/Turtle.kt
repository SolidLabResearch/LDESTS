package be.ugent.idlab.predict.ldests.rdf

actual class Turtle private constructor(prefixes: Map<String, String>) {

    internal actual fun finish(): String {
        TODO("Not yet implemented")
    }

    actual companion object {

        actual operator fun invoke(
            context: RDFBuilder.Context,
            prefixes: Map<String, String>,
            block: RDFBuilder.() -> Unit
        ): String {
            TODO("Not yet implemented")
        }

    }

}
