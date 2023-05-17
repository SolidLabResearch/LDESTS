@file:JsModule("n3")
package be.ugent.idlab.predict.ldests.rdf.lib

external class Triple(
    subject: Term,
    predicate: Term,
    `object`: Term,
    graph: Term? = definedExternally
) {

    val subject: Term
    val predicate: Term
    val `object`: Term

}

external interface Term {
    val value: String
}

external class NamedNode(
    iri: String
): Term {
    override val value: String
}

external class BlankNode(
    name: String
): Term {
    override val value: String
}

external class Literal(
    id: String
): Term {
    override val value: String
}

external class Variable(
    name: String
): Term {
    override val value: String
}
