@file:JsModule("n3")
@file:JsQualifier("DataFactory")
package be.ugent.idlab.predict.ldests.lib.rdf

@JsName("namedNode")
external fun createN3NamedNode(value: String): N3NamedNode

@JsName("literalNode")
external fun createN3LiteralNode(value: String): N3Literal
