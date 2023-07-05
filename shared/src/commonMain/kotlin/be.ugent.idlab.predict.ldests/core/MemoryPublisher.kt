package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.LocalResource
import be.ugent.idlab.predict.ldests.rdf.TripleBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.TripleStore

class MemoryPublisher: Publisher() {

    override val root = ""

    private val bufs = mutableMapOf<String, TripleStore>()

    override suspend fun fetch(path: String): TripleProvider? {
        return bufs[path]?.let { LocalResource.wrap(it) }
    }

    override suspend fun publish(path: String, data: TripleBuilder.() -> Unit): Boolean {
        bufs.getOrPut(path) { TripleStore() }.insert(data)
        // always succeeds
        return true
    }

}
