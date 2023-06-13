package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Query
import be.ugent.idlab.predict.ldests.rdf.TripleStore

class Shape {

    // TODO: maybe sealed type, covering the cases for IRI, collection of IRI and regular constant literal
    class Constraint {

    }

    internal object Ontology {

        // TODO: various members from the ontology (constants, tree path, variables)

    }

    private val data = TripleStore()

    // TODO: getters for constrained constants, unconstrained constants, ...


    /**
     * Narrows the broader shape (`this`) with the constraints found in `delta` (using own ontology!)
     */
    internal fun applyConstraints(constraints: Iterable<Constraint>): Shape {
        // TODO: logic described above
        return this
    }

    /**
     * Generates a query that can be used to extract all relevant fields from a collection of triples
     */
    internal fun asQuery(): Query {
        // TODO: temp hardcoded sparql query based on hardcoded (not included) shape, should be derived from data analyser instead
        //  the compacter query (top) is ~ 3 times faster, 10s <-> 28s to execute over the accel dataset
        return Query("""
                PREFIX saref: <https://saref.etsi.org/core/>
                PREFIX rdf: <http://rdfs.org/ns/void#>
            
                SELECT ?prop ?id ?value WHERE {
                    ?sample saref:relatesToProperty ?prop ;
                        saref:hasTimestamp ?id ;
                        saref:hasValue ?value .
                }""")
//                """
//                PREFIX saref: <https://saref.etsi.org/core/>
//                PREFIX rdf: <http://rdfs.org/ns/void#>
//
//                SELECT ?prop ?time ?value WHERE {
//                    ?sample a saref:Measurement ;
//                        rdf:inDataset ?dataset ;
//                        saref:measurementMadeBy ?source ;
//                        saref:relatesToProperty ?prop ;
//                        saref:hasTimestamp ?time ;
//                        saref:hasValue ?value .
//                }
//                """
    }

}
