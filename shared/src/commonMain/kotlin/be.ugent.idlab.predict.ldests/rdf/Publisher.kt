package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.core.Stream

abstract class Publisher {

    abstract val root: String

    abstract suspend fun Stream.publish()

    abstract suspend fun Stream.publish(delta: Turtle.() -> Unit)

    abstract suspend fun Stream.Fragment.publish()

    abstract suspend fun Stream.Fragment.Resource.publish()

}