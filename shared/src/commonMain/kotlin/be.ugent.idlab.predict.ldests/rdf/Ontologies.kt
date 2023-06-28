package be.ugent.idlab.predict.ldests.rdf

object Ontologies {

    object RDF {
        const val PREFIX = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        val P_TYPE = "${PREFIX}type".asNamedNode()
    }

    object LDES {
        // TODO: types
    }

    object TREE {
        const val PREFIX = "https://w3id.org/tree#"

        val O_NODE = "${PREFIX}Node".asNamedNode()
        val P_RELATION = "${PREFIX}relation".asNamedNode()
        val O_GTOE_RELATION = "${PREFIX}GreaterThanOrEqualToRelation".asNamedNode()
        val P_VALUE = "${PREFIX}value".asNamedNode()
        val P_PATH = "${PREFIX}path".asNamedNode()
        val P_NODE = "${PREFIX}node".asNamedNode()
    }

    object SHACL {
        const val PREFIX = "http://www.w3.org/ns/shacl#"

        val O_SHAPE = "${PREFIX}NodeShape".asNamedNode()
        val O_PROPERTY = "${PREFIX}PropertyShape".asNamedNode()
        val O_LITERAL = "${PREFIX}Literal".asNamedNode()
        val O_IRI = "${PREFIX}IRI".asNamedNode()
        val P_PROPERTY = "${PREFIX}property".asNamedNode()
        val P_PATH = "${PREFIX}path".asNamedNode()
        val P_TARGET_CLASS = "${PREFIX}targetClass".asNamedNode()
        val P_PROPERTY_TYPE = "${PREFIX}nodeKind".asNamedNode()
        val P_DATA_TYPE = "${PREFIX}datatype".asNamedNode()
        val P_MIN_COUNT = "${PREFIX}minCount".asNamedNode()
        val P_MAX_COUNT = "${PREFIX}maxCount".asNamedNode()
    }

}
