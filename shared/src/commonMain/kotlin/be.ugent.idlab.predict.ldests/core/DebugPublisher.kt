package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.util.log

class DebugPublisher: Publisher() {

    override val context = RDFBuilder.Context(
        path = "debug.local"
    )

    override suspend fun fetch(path: String): TripleProvider? {
        // no compat checking relevant here
        return null
    }

    override fun publish(path: String, data: RDFBuilder.() -> Unit) {
        val str = Turtle(
            context = context,
            prefixes = Ontology.PREFIXES,
            block = data
        )
        log("In debugger for `$path`:\n$str")
    }

}
