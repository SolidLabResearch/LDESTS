package be.ugent.idlab.predict.ldests.rdf

expect class Turtle {

    internal suspend fun finish(): String

    companion object {

        suspend operator fun invoke(
            vararg prefixes: Pair<String, String>,
            block: TripleBuilder.() -> Unit
        ): String

    }

}