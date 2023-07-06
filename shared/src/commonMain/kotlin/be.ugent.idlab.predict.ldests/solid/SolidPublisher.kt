package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.core.Publisher
import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider

class SolidPublisher(
    url: String
): Publisher() {

    override val context = RDFBuilder.Context(
        path = url
    )

    private val connection = SolidConnection(url = url)

    override suspend fun fetch(path: String): TripleProvider {
        return connection.fromUrl("${context.path}/$path").read()
    }

    override suspend fun publish(path: String, data: RDFBuilder.() -> Unit): Boolean {
        return connection.fromUrl("${context.path}/$path").write(block = data) in 200..299
    }

}
