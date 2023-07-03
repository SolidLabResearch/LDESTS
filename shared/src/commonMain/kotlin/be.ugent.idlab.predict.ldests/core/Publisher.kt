package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Turtle

abstract class Publisher {

    /**
     * The base URL of this publisher; e.g. `localhost:3000`
     */
    abstract val root: String

    /**
     * Publishes the data retrieved by executing the provided lambda. Returns `true` upon success (so memory can be
     *  freed again)
     * Expected path is the complete path after host, like `stream/fragment/` or `stream/fragment/resource`
     */
    abstract suspend fun publish(path: String, data: Turtle.() -> Unit): Boolean

    /**
     * Publishes the data passed to the function (turtle format typically expected). Returns `true` upon success (so
     *  memory can be freed again)
     * Expected path is the complete path after host, like `stream/fragment/` or `stream/fragment/resource`
     */
    abstract suspend fun publish(path: String, data: String): Boolean

}