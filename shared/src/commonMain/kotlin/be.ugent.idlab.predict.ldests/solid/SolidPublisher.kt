package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.core.Stream
import be.ugent.idlab.predict.ldests.core.Stream.Companion.fragment
import be.ugent.idlab.predict.ldests.core.Stream.Companion.resource
import be.ugent.idlab.predict.ldests.core.Stream.Companion.stream
import be.ugent.idlab.predict.ldests.rdf.Publisher
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.asNamedNode

class SolidPublisher(
    url: String
): Publisher() {

    private val connection = SolidConnection(url = url)

    override val root = connection.root.url

    override suspend fun Stream.publish() {
        // TODO: later support for custom location
        // creating a folder for the stream in the root of the connection
        connection.root.folder(name).write { stream(this@publish) }
    }

    override suspend fun Stream.publish(delta: Turtle.() -> Unit) {
        connection.root.folder(name).write(delta)
    }

    override suspend fun Stream.Fragment.publish() {
        connection.root.folder(url.asNamedNode()).write { fragment(this@publish) }
    }

    override suspend fun Stream.Fragment.Resource.publish() {
        connection.root.resource(url.asNamedNode()).write { resource(this@publish) }
    }

}
