package be.ugent.idlab.predict.ldests.rdf.ontology

import be.ugent.idlab.predict.ldests.rdf.asNamedNode

object SHAPETS: Ontology {

    // FIXME: use an actual prefix once the ontology is more official
    override val prefix = "shts"
    override val base_uri = "https://predict.ugent.be/shape#"

    val Type = "${base_uri}BaseShape".asNamedNode()
    val Identifier = "${base_uri}SampleIdentifier".asNamedNode()
    val Constant = "${base_uri}SampleConstant".asNamedNode()
    val Variable = "${base_uri}SampleVariable".asNamedNode()

    val startIndex = "${base_uri}startIndex".asNamedNode()
    val endIndex = "${base_uri}endIndex".asNamedNode()
    val constantValues = "${base_uri}values".asNamedNode()
    val constantValue = "${base_uri}value".asNamedNode()

}
