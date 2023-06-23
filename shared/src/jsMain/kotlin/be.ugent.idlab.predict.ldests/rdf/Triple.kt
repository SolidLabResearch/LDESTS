package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.*

actual typealias Triple = N3Triple
actual typealias Term = N3Term
actual typealias NamedNodeTerm = N3NamedNode
actual typealias BlankNodeTerm = N3BlankNode
actual typealias LiteralTerm = N3Literal

actual fun createNamedNodeTerm(value: String): NamedNodeTerm = createN3NamedNode(value)

actual fun createLiteralNodeTerm(value: String): LiteralTerm = createN3LiteralNode(value)
