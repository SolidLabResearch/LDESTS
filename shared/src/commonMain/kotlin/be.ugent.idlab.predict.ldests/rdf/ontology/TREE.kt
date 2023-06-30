package be.ugent.idlab.predict.ldests.rdf.ontology

import be.ugent.idlab.predict.ldests.rdf.asNamedNode

object TREE: Ontology {

    override val prefix = "tree"
    override val base_uri = "https://w3id.org/tree#"

    val Node = "${base_uri}Node".asNamedNode()
    val GreaterThanOrEqualToRelation = "${base_uri}GreaterThanOrEqualToRelation".asNamedNode()

    val relation = "${base_uri}relation".asNamedNode()
    val value = "${base_uri}value".asNamedNode()
    val path = "${base_uri}path".asNamedNode()
    val node = "${base_uri}node".asNamedNode()

}
