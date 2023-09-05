package be.ugent.idlab.predict.ldests.rdf.ontology

import be.ugent.idlab.predict.ldests.rdf.asNamedNode

data object XSD: Ontology {

    override val prefix = "xsd"
    override val base_uri = "http://www.w3.org/2001/XMLSchema#"
    val string = "${base_uri}string".asNamedNode()
    val float = "${base_uri}float".asNamedNode()
    val double = "${base_uri}double".asNamedNode()
    val integer = "${base_uri}integer".asNamedNode()

}
