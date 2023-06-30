package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology

expect class Turtle {

    class Subject {

        infix fun has(predicate: String): SubjectPredicate

        infix fun has(predicate: NamedNodeTerm): SubjectPredicate

    }

    class SubjectPredicate {

        infix fun being(literal: String)

        infix fun being(literal: Int)

        infix fun being(literal: Long)

        infix fun being(literal: Float)

        infix fun being(literal: Double)

        infix fun being(value: Term)

        infix fun being(value: Blank)

        infix fun being(value: List)

    }

    inner class Blank {

        inner class Scope {

            operator fun String.unaryPlus(): SubjectPredicate

            operator fun NamedNodeTerm.unaryPlus(): SubjectPredicate

        }

    }

    class List

    operator fun String.unaryPlus(): Subject

    operator fun NamedNodeTerm.unaryPlus(): Subject

    fun blank(block: Blank.Scope.() -> Unit): Blank

    fun list(items: Iterable<Any /*Term or result from BlankScope only!*/>): List

    fun list(vararg data: Any /*Term or result from BlankScope only!*/): List

    internal suspend fun finish(): String

    companion object {

        suspend operator fun invoke(
            vararg prefixes: Pair<String, String> = Ontology.PREFIXES,
            block: Turtle.() -> Unit
        ): String

    }

}