package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.lib.node.NodeStream
import be.ugent.idlab.predict.ldests.lib.node.WritableNodeStream
import be.ugent.idlab.predict.ldests.lib.node.finished
import kotlinx.coroutines.await

internal inline fun <reified T> createWritableNodeStream(
    objectMode: Boolean = true,
    crossinline block: suspend (data: T?) -> Unit
): WritableNodeStream<T?> {
    val opts: dynamic = Any()
    opts.objectMode = objectMode
    val stream = WritableNodeStream<T?>(opts)
    stream.onReceive = { data, _, callback ->
        block(data)
        callback()
    }
    return stream
}

suspend fun NodeStream.join() = finished(this).await()
