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

expect fun createNamedNodeTerm(value: String): NamedNodeTerm

expect fun createLiteralNodeTerm(value: String): LiteralTerm

// helpers for the methods above
fun String.asNamedNode() = createNamedNodeTerm(this)

fun String.asLiteralNode() = createLiteralNodeTerm(this)
