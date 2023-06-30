package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Writer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class Turtle private constructor(prefixes: Array<out Pair<String, String>>){

    private val writer = run {
        val options: dynamic = Any()
        val p: dynamic = Any()
        prefixes.forEach { (prefix, long) -> p[prefix] = long }
        options.prefixes = p
        N3Writer(options)
    }

    actual class Subject(private val callback: (predicate: NamedNodeTerm, `object`: dynamic) -> Unit) {

        actual infix fun has(predicate: String): SubjectPredicate {
            return SubjectPredicate { callback(predicate.asNamedNode(), it) }
        }

        actual infix fun has(predicate: NamedNodeTerm): SubjectPredicate {
            return SubjectPredicate { callback(predicate, it) }
        }

    }

    actual class SubjectPredicate(private val callback: (value: dynamic) -> Unit) {

        actual infix fun being(literal: String) {
            callback(literal.asLiteral())
        }

        actual infix fun being(literal: Int) {
            callback(literal.asLiteral())
        }

        actual infix fun being(literal: Long) {
            callback(literal.asLiteral())
        }

        actual infix fun being(literal: Float) {
            callback(literal.asLiteral())
        }

        actual infix fun being(literal: Double) {
            callback(literal.asLiteral())
        }

        actual infix fun being(value: Term) {
            callback(value)
        }

        actual infix fun being(value: Blank) {
            callback(value.data)
        }

        actual infix fun being(value: List) {
            callback(value.data)
        }

    }

    actual inner class Blank(block: Scope.() -> Unit) {

        internal val data = writer.createBlank(
            Scope().apply(block).data
                .map { (predicate, `object`) ->
                    val result: dynamic = Any()
                    result.predicate = predicate
                    result.`object` = `object`
                    result
                }
                .toTypedArray()
        )

        actual inner class Scope {

            internal val data = mutableMapOf<NamedNodeTerm, Any>()

            actual operator fun String.unaryPlus() = SubjectPredicate {
                data[this.asNamedNode()] = it
            }

            actual operator fun NamedNodeTerm.unaryPlus() = SubjectPredicate {
                data[this] = it
            }

        }

    }

    actual class List internal constructor(internal val data: dynamic)

    actual fun blank(block: Turtle.Blank.Scope.() -> Unit): Blank {
        return Blank(block)
    }

    internal actual suspend fun finish(): String = suspendCoroutine {
        writer.finish { error, result ->
            if (error != null) {
                it.resumeWithException(error)
            } else {
                it.resume(result)
            }
        }
    }

    actual companion object {

        actual suspend operator fun invoke(vararg prefixes: Pair<String, String>, block: Turtle.() -> Unit): String {
            val scope = Turtle(prefixes = prefixes)
            scope.block()
            return scope.finish()
        }

    }

    actual operator fun String.unaryPlus() = Subject { predicate: NamedNodeTerm, `object`: Any ->
        writer.add(
            subject = this.asNamedNode(),
            predicate = predicate,
            `object` = `object`
        )
    }

    actual operator fun NamedNodeTerm.unaryPlus() = Subject { predicate: NamedNodeTerm, `object`: Any ->
        writer.add(
            subject = this,
            predicate = predicate,
            `object` = `object`
        )
    }

    actual fun list(items: Iterable<Any>) = List(
        writer.createList(
            items.map {
                when (it) {
                    is Blank -> { it.data }
                    else -> { it /* N3Term hopefully */ }
                }
            }.toTypedArray()
        )
    )

    actual fun list(vararg data: Any) = List(
        writer.createList(
            data.map {
                when (it) {
                    is Blank -> { it.data }
                    else -> { it /* N3Term hopefully */ }
                }
            }.toTypedArray()
        )
    )

}
