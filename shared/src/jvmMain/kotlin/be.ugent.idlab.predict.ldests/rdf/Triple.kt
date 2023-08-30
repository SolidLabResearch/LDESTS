package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.rdf.ontology.XSD

actual data class Triple actual constructor(
    actual val subject: Term,
    actual val predicate: Term,
    actual val `object`: Term
)

actual interface Term {
    actual val value: String
}

// cannot make this a value class due to `expect` not being a value class :C
actual class NamedNodeTerm(override val value: String): Term

actual class BlankNodeTerm(override val value: String) : Term

actual class LiteralTerm(override val value: String, actual val dataType: NamedNodeTerm): Term

actual fun String.asNamedNode(): NamedNodeTerm = NamedNodeTerm(toString())

actual fun String.asLiteral(): LiteralTerm = literal(toString(), XSD.string)

actual fun Int.asLiteral(): LiteralTerm = literal(toString(), XSD.integer)

actual fun Long.asLiteral(): LiteralTerm = literal(toString(), XSD.integer)

actual fun Float.asLiteral(): LiteralTerm = literal(toString(), XSD.float)

actual fun Double.asLiteral(): LiteralTerm = literal(toString(), XSD.double)

internal actual fun literal(content: String, type: NamedNodeTerm): LiteralTerm = LiteralTerm(value = content, dataType = type)
