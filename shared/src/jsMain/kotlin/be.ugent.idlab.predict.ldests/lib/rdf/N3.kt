@file:JsModule("n3")
package be.ugent.idlab.predict.ldests.lib.rdf

import be.ugent.idlab.predict.ldests.lib.node.ReadableNodeStream

@JsName("Triple")
external class N3Triple(
    subject: N3Term,
    predicate: N3Term,
    `object`: N3Term,
    graph: N3Term? = definedExternally
) {

    val subject: N3Term
    val predicate: N3Term
    val `object`: N3Term

}

@JsName("Term")
external interface N3Term {
    val value: String
}

@JsName("NamedNode")
external class N3NamedNode(
    iri: String
): N3Term {
    override val value: String
}

@JsName("BlankNode")
external class N3BlankNode(
    name: String
): N3Term {
    override val value: String
}

@JsName("Literal")
external class N3Literal(
    id: String
): N3Term {
    override val value: String
}

@JsName("Variable")
external class N3Variable(
    name: String
): N3Term {
    override val value: String
}

@JsName("StreamParser")
external class N3StreamParser: ReadableNodeStream

@JsName("Store")
external class N3Store {

    val size: Int

    fun add(triple: N3Triple)
    fun has(triple: N3Triple)
    fun delete(triple: N3Triple)
    fun forEach(
        callback: (N3Triple) -> Unit,
        subject: N3Term? = definedExternally,
        predicate: N3Term? = definedExternally,
        `object`: N3Term? = definedExternally
    )
    fun getSubjects(): Array<N3Term>
    fun getPredicates(): Array<N3Term>
    fun getObjects(): Array<N3Term>

}
