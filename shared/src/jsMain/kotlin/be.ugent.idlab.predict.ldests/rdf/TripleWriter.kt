package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Term
import be.ugent.idlab.predict.ldests.lib.rdf.N3Writer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual class TripleWriter private constructor(prefixes: Array<out Pair<String, String>>){

    private val writer = run {
        val options: dynamic = Any()
        val p: dynamic = Any()
        prefixes.forEach { (prefix, long) -> p[prefix] = long }
        options.prefixes = p
        N3Writer(prefixes)
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

            internal val data = mutableMapOf<Term, Any>()

            actual fun add(property: Term, `object`: Term) {
                data[property] = `object`
            }

            actual fun add(property: Term, `object`: Blank.Scope.() -> Unit) {
                data[property] = Blank(`object`).data
            }

            actual fun add(property: Term, `object`: List) {
                data[property] = `object`
            }

        }

    }

    actual class List internal constructor(internal val data: N3Writer.IntermediateResult)

    actual fun add(subject: Term, predicate: Term, `object`: Term) {
        writer.add(subject = subject, predicate = predicate, `object` = `object`)
    }

    actual fun add(subject: Term, predicate: Term, `object`: Blank) {
        writer.add(subject = subject, predicate = predicate, `object` = `object`.data)
    }

    actual fun add(
        subject: Term,
        predicate: Term,
        `object`: List
    ) {
        writer.add(subject = subject, predicate = predicate, `object` = `object`.data)
    }

    actual fun blank(block: TripleWriter.Blank.Scope.() -> Unit): Blank {
        return Blank(block)
    }

    actual fun kotlin.collections.List<Any>.toWritableList(): List {
        return List(
            writer.createList(
                map {
                    when (it) {
                        is Blank -> { it.data }
                        else -> { it /* N3Term hopefully */ }
                    }
                }.toTypedArray() as Array<N3Term>
            )
        )
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

        actual suspend operator fun invoke(vararg prefixes: Pair<String, String>, block: TripleWriter.() -> Unit): String {
            val scope = TripleWriter(prefixes = prefixes)
            scope.block()
            return scope.finish()
        }

    }

}
