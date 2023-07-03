package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

open class Publishable(
    // complete path after host, like `stream/fragment/` or `stream/fragment/resource`
    internal val path: String,
    protected var target: Publisher? = null
) {

    private val lock = Mutex()
    private val buffer = mutableListOf<Turtle.() -> Unit>()

    protected val url: String?
        get() = target?.let { "${it.root}/$path" }

    val uri: NamedNodeTerm
        get() = url!!.asNamedNode()

    protected suspend fun publish(block: Turtle.() -> Unit) = lock.withLock {
        target?.publish(
            path = path,
            data = block
        ) ?: run {
            // as we don't have a target yet, the block's data is undefined, so storing it as such instead
            buffer.add(block)
        }
    }

    // TODO: open version, allowing stream to recursively make the fragments publishing to the same publisher
    //  or rework this so the publishing is exclusively done by the publisher, and is responsible for
    //  recursive ops
    suspend fun publishTo(publisher: Publisher) = lock.withLock {
        target = publisher
        log("Added a publisher to a publishable, inserting ${buffer.size} buffered elements.")
        val it = buffer.iterator()
        while (it.hasNext() && target!!.publish(path = path, data = it.next())) {
            it.remove()
        }
    }

    suspend fun clear() = lock.withLock {
        target = null
    }

}