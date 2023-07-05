package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.core.Publisher
import be.ugent.idlab.predict.ldests.rdf.TripleBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider

class SolidPublisher(
    url: String
): Publisher() {

    override val root: String = url

    private val connection = SolidConnection(url = url)

    override suspend fun fetch(path: String): TripleProvider {
        return connection.fromUrl("$root/$path").read()
    }

    override suspend fun publish(path: String, data: TripleBuilder.() -> Unit): Boolean {
        return connection.fromUrl("$root/$path").write(block = data) in 200..299
    }

}
