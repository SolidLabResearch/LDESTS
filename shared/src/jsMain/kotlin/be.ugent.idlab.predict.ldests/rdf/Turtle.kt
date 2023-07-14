package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Writer
import be.ugent.idlab.predict.ldests.util.dyn

actual class Turtle private constructor(prefixes: Map<String, String>){

    private val writer = run {
        val p: dynamic = Any()
        prefixes.forEach { (prefix, long) -> p[prefix] = long }
        N3Writer(dyn("prefixes" to p))
    }

    internal actual fun finish(): String {
        var turtle: String = ""
        writer.finish { error, result ->
            if (error != null) {
                throw error
            } else {
                turtle = result
            }
        }
        return turtle
    }

    actual companion object {

        actual operator fun invoke(
            context: RDFBuilder.Context,
            prefixes: Map<String, String>,
            block: RDFBuilder.() -> Unit
        ): String {
            val writer = Turtle(prefixes = prefixes)
            with (writer) {
                RDFBuilder(context) { subject, predicate, `object` ->
                    writer.writer.add(
                        subject = subject,
                        predicate = predicate,
                        `object` = `object`.processed()
                    )
                }.apply(block)
                return writer.finish()
            }
        }

    }

    private fun Any.processed(): dynamic = when (this) {
        is RDFBuilder.Blank -> { processed() }
        is RDFBuilder.List -> { processed() }
        else /* Term hopefully */ -> { /* no processing needed */ this }
    }

    private fun RDFBuilder.Blank.processed(): dynamic = writer.createBlank(
        data.map { (predicate, `object`) ->
            dyn(
                "predicate" to predicate,
                "object" to `object`.processed()
            )
        }.toTypedArray()
    )

    private fun RDFBuilder.List.processed(): dynamic = writer.createList(
        data.map { it.processed() }.toTypedArray()
    )

}
