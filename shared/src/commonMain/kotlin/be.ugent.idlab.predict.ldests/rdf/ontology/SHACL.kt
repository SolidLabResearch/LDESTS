package be.ugent.idlab.predict.ldests.rdf.ontology

import be.ugent.idlab.predict.ldests.rdf.asNamedNode

object SHACL: Ontology {

    override val prefix = "sh"
    override val base_uri = "http://www.w3.org/ns/shacl#"

    val Shape = "${base_uri}NodeShape".asNamedNode()
    val Property = "${base_uri}PropertyShape".asNamedNode()
    val Literal = "${base_uri}Literal".asNamedNode()
    val IRI = "${base_uri}IRI".asNamedNode()

    val property = "${base_uri}property".asNamedNode()
    val path = "${base_uri}path".asNamedNode()
    val targetClass = "${base_uri}targetClass".asNamedNode()
    val nodeKind = "${base_uri}nodeKind".asNamedNode()
    val dataType = "${base_uri}datatype".asNamedNode()
    val minCount = "${base_uri}minCount".asNamedNode()
    val maxCount = "${base_uri}maxCount".asNamedNode()

}
