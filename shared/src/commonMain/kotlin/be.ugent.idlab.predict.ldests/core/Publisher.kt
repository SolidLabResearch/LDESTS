package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleProvider
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import kotlinx.coroutines.*
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

abstract class Publisher {

    /**
     * The base URL of this publisher; e.g. `localhost:3000`
     */
    abstract val root: String

    /**
     * Fetches data related to the provided path (depending on the type of publisher)
     */
    abstract suspend fun fetch(path: String): TripleProvider?

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

    /**
     * The publishable items this publisher is subscribed to
     */
    private val jobs = mutableMapOf<PublishBuffer, Job>()

    /**
     * Starts publishing everything the buffer receives. Keeps listening until `unsubscribe` with
     *  the same buffer is called or the program terminates
     */
    suspend fun subscribe(scope: CoroutineScope, buffer: PublishBuffer) = with(buffer) {
        subscribe(scope) { path, block -> publish(path, Turtle(block = block)) }?.let {
            jobs[buffer] = it
        }
    }

    /**
     * Starts publishing everything created by the publishable item. Keeps listening until `unsubscribe` with
     *  the same publisher is called
     */
    suspend fun unsubscribe(item: PublishBuffer) {
        jobs[item]?.cancelAndJoin()
        jobs.remove(item)
    }

    suspend fun close() {
        coroutineScope {
            jobs.map { (item, job) ->
                async {
                    job.cancelAndJoin()
                    jobs.remove(item)
                }
            }.awaitAll()
        }
    }

    val Publishable.uri: NamedNodeTerm
        get() = "$root/$path".asNamedNode()

}
