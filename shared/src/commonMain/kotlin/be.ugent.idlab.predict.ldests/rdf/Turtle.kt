package be.ugent.idlab.predict.ldests.rdf

expect class Turtle {

    internal fun finish(): String

    companion object {

        operator fun invoke(
            context: RDFBuilder.Context,
            prefixes: Map<String, String> = mapOf(),
            block: RDFBuilder.() -> Unit
        ): String

    }

}