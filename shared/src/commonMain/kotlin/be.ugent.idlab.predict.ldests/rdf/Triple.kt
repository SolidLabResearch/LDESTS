package be.ugent.idlab.predict.ldests.rdf

expect class Triple {
    val subject: Term
    val predicate: Term
    val `object`: Term
}

expect interface Term {
    val value: String
    val type: String
}

expect interface NamedNodeTerm: Term

expect interface BlankNodeTerm: Term

expect interface LiteralTerm: Term {
    val datatype: NamedNodeTerm
}

// helpers for the methods above
expect fun String.asNamedNode(): NamedNodeTerm

expect fun String.asLiteral(): LiteralTerm

expect fun Int.asLiteral(): LiteralTerm

expect fun Long.asLiteral(): LiteralTerm

expect fun Float.asLiteral(): LiteralTerm

expect fun Double.asLiteral(): LiteralTerm
