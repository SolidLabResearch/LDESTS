package be.ugent.idlab.predict.ldests.rdf

class TripleBuilder(
    private val onTripleAdded: (subject: Term, predicate: Term, `object`: Any /* either Blank, Term or List */) -> Unit
) {

    class Subject(private val callback: (predicate: NamedNodeTerm, `object`: Any) -> Unit) {

        infix fun has(predicate: String): SubjectPredicate {
            return SubjectPredicate { callback(predicate.asNamedNode(), it) }
        }

        infix fun has(predicate: NamedNodeTerm): SubjectPredicate {
            return SubjectPredicate { callback(predicate, it) }
        }

    }

    class SubjectPredicate(private val callback: (value: Any) -> Unit) {

        infix fun being(literal: String) {
            callback(literal.asLiteral())
        }

        infix fun being(literal: Int) {
            callback(literal.asLiteral())
        }

        infix fun being(literal: Long) {
            callback(literal.asLiteral())
        }

        infix fun being(literal: Float) {
            callback(literal.asLiteral())
        }

        infix fun being(literal: Double) {
            callback(literal.asLiteral())
        }

        infix fun being(value: Term) {
            callback(value)
        }

        infix fun being(value: Blank) {
            callback(value)
        }

        infix fun being(value: List) {
            callback(value)
        }

    }

    class Blank private constructor(internal val data: Map<NamedNodeTerm, Any /* Term, List or Blank */>) {

        companion object {

            fun from(block: Scope.() -> Unit): Blank {
                return Blank(data = Scope().apply(block).data)
            }

        }

        class Scope {

            internal val data = mutableMapOf<NamedNodeTerm, Any>()

            operator fun String.unaryPlus() = SubjectPredicate {
                data[this.asNamedNode()] = it
            }

            operator fun NamedNodeTerm.unaryPlus() = SubjectPredicate {
                data[this] = it
            }

        }

    }

    class List internal constructor(internal val data: kotlin.collections.List<Any /*type checked elsewhere*/>)

    fun blank(block: Blank.Scope.() -> Unit): Blank {
        return Blank.from(block)
    }

    operator fun String.unaryPlus() = Subject { predicate: NamedNodeTerm, `object`: Any ->
        onTripleAdded(this.asNamedNode(), predicate, `object`)
    }

    operator fun NamedNodeTerm.unaryPlus() = Subject { predicate: NamedNodeTerm, `object`: Any ->
        onTripleAdded(this, predicate, `object`)
    }

    fun list(items: Iterable<Any>) = List(data = items.toList())

    fun list(vararg data: Any) = List(data = data.toList())

}
