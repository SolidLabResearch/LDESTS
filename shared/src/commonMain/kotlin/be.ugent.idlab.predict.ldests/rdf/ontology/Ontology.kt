package be.ugent.idlab.predict.ldests.rdf.ontology

sealed interface Ontology {

    val prefix: String
    val base_uri: String

    companion object {

        val ONTOLOGIES = setOf(RDF, SHACL, TREE, SHAPETS, LDESTS)
        val PREFIXES = ONTOLOGIES.associate { it.prefix to it.base_uri }

    }

}