package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.core.Publisher
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.Turtle

class SolidPublisher(
    url: String
): Publisher() {

    override val root: String = url

    private val connection = SolidConnection(url = url)

    override suspend fun fetch(path: String): TripleProvider {
        return connection.fromUrl("$root/$path").read()
    }

    override suspend fun publish(path: String, data: Turtle.() -> Unit): Boolean {
        return connection.fromUrl("$root/$path").write(block = data) in 200..299
    }

    override suspend fun publish(path: String, data: String): Boolean {
        return connection.fromUrl("$root/$path").write(data) in 200..299
    }

}
