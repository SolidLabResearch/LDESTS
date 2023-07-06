package be.ugent.idlab.predict.ldests.rdf

class RDFBuilder(
    val context: Context,
    private val onTripleAdded: (subject: Term, predicate: Term, `object`: Any /* either Blank, Term or List */) -> Unit
) {

    data class Context(
        val path: String
    ) {
        val Element.uri: NamedNodeTerm
            get() = "${this@Context.path}/${name}".asNamedNode()
    }

    interface Element {
        val name: String
    }

    val Element.uri
        get() = with (context) { uri }

    operator fun NamedNodeTerm.unaryPlus() = Subject { predicate: NamedNodeTerm, `object`: Any ->
        onTripleAdded(this, predicate, `object`)
    }

    class Subject(private val callback: (predicate: NamedNodeTerm, `object`: Any) -> Unit) {

        infix fun has(predicate: NamedNodeTerm) = SubjectPredicate { callback(predicate, it) }

    }

    class SubjectPredicate(private val callback: (`object`: Any) -> Unit) {

        infix fun being(literal: Int) = callback(literal.asLiteral())

        infix fun being(literal: Long) = callback(literal.asLiteral())

        infix fun being(literal: Float) = callback(literal.asLiteral())

        infix fun being(literal: Double) = callback(literal.asLiteral())

        infix fun being(value: Term) = callback(value)

        infix fun being(blank: Blank) = callback(blank)

        infix fun being(list: List) = callback(list)

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

    class List internal constructor(internal val data: kotlin.collections.List<Any>)

    fun blank(block: Blank.Scope.() -> Unit): Blank {
        return Blank.from(block)
    }

    // does the necessary type checking
    fun list(items: Iterable<Any>) = List(data = items.toList())

    fun list(vararg data: Any) = List(data.toList())

}