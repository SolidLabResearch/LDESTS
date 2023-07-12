@file:JsModule("n3")
@file:JsQualifier("DataFactory")
package be.ugent.idlab.predict.ldests.lib.rdf

@JsName("namedNode")
external fun createN3NamedNode(value: String): N3NamedNode

@JsName("literal")
external fun createN3Literal(value: String, type: N3NamedNode = definedExternally): N3Literal

@JsName("literal")
external fun createN3Literal(value: Int): N3Literal

@JsName("literal")
external fun createN3Literal(value: Long): N3Literal

@JsName("literal")
external fun createN3Literal(value: Float): N3Literal

@JsName("literal")
external fun createN3Literal(value: Double): N3Literal
