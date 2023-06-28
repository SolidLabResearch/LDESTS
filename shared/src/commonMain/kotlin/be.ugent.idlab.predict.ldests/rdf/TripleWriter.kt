package be.ugent.idlab.predict.ldests.rdf

expect class TripleWriter {
    inner class Blank {

        inner class Scope {

            fun add(property: Term, `object`: Term)

            fun add(property: Term, `object`: Blank.Scope.() -> Unit)

            fun add(property: Term, `object`: List)

        }

    }

    class List

    fun add(subject: Term, predicate: Term, `object`: Term)

    fun add(subject: Term, predicate: Term, `object`: Blank)

    fun add(subject: Term, predicate: Term, `object`: List)

    fun blank(block: Blank.Scope.() -> Unit): Blank

    fun kotlin.collections.List<Any /*Term or result from BlankScope only!*/>.toWritableList(): List

    internal suspend fun finish(): String

    companion object {

        suspend operator fun invoke(vararg prefixes: Pair<String, String>, block: TripleWriter.() -> Unit): String

    }

}