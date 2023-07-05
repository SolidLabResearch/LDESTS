package be.ugent.idlab.predict.ldests.rdf.ontology

import be.ugent.idlab.predict.ldests.rdf.asNamedNode

object LDESTS: Ontology {

    // FIXME: use an actual prefix once the ontology is more official
    override val prefix = "ldests"
    override val base_uri = "https://predict.ugent.be/ldests#"

    val StreamType = "${base_uri}Node".asNamedNode()
    val shape = "${base_uri}shape".asNamedNode()
    val rules = "${base_uri}rules".asNamedNode()
    val constraintSet = "${base_uri}constraintSet".asNamedNode()
    val ConstraintSet = "${base_uri}ConstraintSet".asNamedNode()
    val Constraint = "${base_uri}Constraint".asNamedNode()
    val constraintValue = "${base_uri}constraintValue".asNamedNode()
    val constraints = "${base_uri}constraints".asNamedNode()
    val constraintId = "${base_uri}constraintId".asNamedNode()

    val FragmentType = "${base_uri}Fragment".asNamedNode()
    val resources = "${base_uri}resources".asNamedNode()

    val ResourceType = "${base_uri}Resource".asNamedNode()
    val contents = "${base_uri}contains".asNamedNode()


}
