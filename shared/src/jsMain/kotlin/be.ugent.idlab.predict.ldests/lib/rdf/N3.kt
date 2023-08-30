@file:JsModule("n3")
package be.ugent.idlab.predict.ldests.lib.rdf

@JsName("Triple")
external class N3Triple(
    subject: N3Term,
    predicate: N3Term,
    `object`: N3Term,
    graph: N3Term? = definedExternally
) {

    constructor(
        subject: N3Term,
        predicate: N3Term,
        `object`: N3Term,
    )

    val subject: N3Term
    val predicate: N3Term
    val `object`: N3Term

}

@JsName("Term")
external interface N3Term {
    val value: String
    @JsName("termType")
    val type: String
}

@JsName("NamedNode")
external class N3NamedNode: N3Term {
    override val value: String
    override val type: String
}

@JsName("BlankNode")
external class N3BlankNode: N3Term {
    override val value: String
    override val type: String
}

@JsName("Literal")
external class N3Literal: N3Term {
    override val value: String
    override val type: String

    /**
     * The language as lowercase BCP47 string (examples: en, en-gb)
     * or an empty string if the literal has no language.
     * @link http://tools.ietf.org/html/bcp47
     */
    val language: String
    /**
     * A NamedNode whose IRI represents the datatype of the literal.
     */
    @JsName("datatype")
    val dataType: N3NamedNode
}

@JsName("Parser")
external class N3Parser(
    /* Optional { format: 'application/<>' | 'N-Triples' | ..., baseIRI: 'http://example.com/' } */
    options: dynamic = definedExternally
) {

    /* Only using the synchronous version here */
    fun parse(turtle: String): Array<N3Triple>

}

@JsName("Store")
external class N3Store {

    val size: Int

    fun add(triple: N3Triple)
    @JsName("addQuad")
    fun add(subject: N3Term, predicate: N3Term, `object`: N3Term, graph: N3Term? = definedExternally)
    @JsName("addQuads")
    fun add(triples: Array<N3Triple>)
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
    fun getQuads(
        subject: N3Term = definedExternally,
        predicate: N3Term = definedExternally,
        `object`: N3Term = definedExternally,
        graph: N3Term = definedExternally
    ): Array<N3Triple>
    fun createBlankNode(suggestedName: String = definedExternally): N3BlankNode
}

@JsName("Writer")
external class N3Writer(options: dynamic) {


    // `object` accepts `N3Term` (typed) and the results from `blank` and `list` (dynamic)
    @JsName("addQuad")
    fun add(subject: N3Term, predicate: N3Term, `object`: dynamic)

    @JsName("blank")
    fun createBlank(properties: Array<dynamic /* predicate: N3Term, `object`: N3Term | IntermediateResult */>): dynamic

    // property types accept `N3Term` (typed) and the results from `blank` (dynamic)
    @JsName("list")
    fun createList(properties: Array<dynamic>): dynamic

    @JsName("end")
    fun finish(callback: (error: Error?, result: String) -> Unit)

}
