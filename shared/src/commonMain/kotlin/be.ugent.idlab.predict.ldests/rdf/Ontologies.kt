package be.ugent.idlab.predict.ldests.rdf

object Ontologies {

    /**
     * Groups all provided triples by their subject
     */
    // TODO: remove this in favour of store
    fun List<Triple>.group(): Collection<List<Triple>> {
        return groupBy { it.subject.value }.values
    }

    object RDF {

        private const val BASE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

        object Predicate {

            const val RDF_IS = "${BASE}type"

        }

        /**
         * Extracts all type information from the collection of triples. The collection of triples supplied should all
         *  have the same subject!
         */
        fun List<Triple>.types(): List<String> {
            return filter { it.predicate.value == Predicate.RDF_IS }.map { it.`object`.value }
        }

    }

    object LDP {

        private const val BASE = "http://www.w3.org/ns/ldp#"

        object Object {
            const val LDP_RESOURCE = "${BASE}Resource"
            const val LDP_CONTAINER = "${BASE}Container"
            const val LDP_BASIC_CONTAINER = "${BASE}BasicContainer"
        }

        /**
         * Checks if the combination of types matches with that of a file (and NOT a folder!)
         */
        fun List<String>.isFile(): Boolean {
            return any { it == Object.LDP_RESOURCE } &&
                    none { it == Object.LDP_CONTAINER || it == Object.LDP_BASIC_CONTAINER }
        }

        /**
         * Checks if the combination of types matches with that of a folder
         */
        fun List<String>.isDirectory(): Boolean {
            return any { it == Object.LDP_CONTAINER || it == Object.LDP_BASIC_CONTAINER }
        }

    }


}
