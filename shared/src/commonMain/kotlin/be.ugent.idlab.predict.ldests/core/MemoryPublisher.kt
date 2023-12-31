package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.TripleStore

class MemoryPublisher: Publisher() {

    override val context = RDFBuilder.EmptyContext

    val buffer = TripleStore()

    override suspend fun fetch(path: String): TripleProvider? {
        // FIXME: search for all subjects starting with path... somehow
        return null
    }

    override fun publish(path: String, data: RDFBuilder.() -> Unit) {
        buffer.insert(context = RDFBuilder.Context(path = path), block = data)
    }

}
