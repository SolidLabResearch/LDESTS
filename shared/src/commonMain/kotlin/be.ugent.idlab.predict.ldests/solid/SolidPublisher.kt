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

    override fun publish(path: String, data: RDFBuilder.() -> Unit) {
        // TODO: keep the data callback somewhere "safe" so it can be reused if this call fails due to lacking
        //  authentication or other (temporary) failures, while dropping the data upon success or irrecoverable failure
        connection.fromUrl("${context.path}/$path").write(block = data)
    }

    override suspend fun flush() {
        connection.flush()
    }

}
