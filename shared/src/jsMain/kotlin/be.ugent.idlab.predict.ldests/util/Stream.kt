package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.lib.node.*
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual typealias Stream = NodeStream

actual typealias InputStream<T> = ReadableNodeStream<T>

actual suspend fun NodeStream.join() = suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
    cont.invokeOnCancellation {
        // destroying the stream if canceled
        error("NodeStream", "A node stream has been destroyed!")
        this@join.destroy()
    }
    // alternatively, on("finish") and on("error") could be used
    finished(this@join).then(
        onFulfilled = {
            log("NodeStream", "A node stream has finished!")
            cont.resume(it)
        },
        onRejected = {
            warn("NodeStream", "A node stream `join` has been rejected!")
            cont.resumeWithException(it)
        }
    )
}

actual fun N3Triple.streamify(): ReadableNodeStream<N3Triple> =
    object: ReadableNodeStream<N3Triple>(
        options = dyn("objectMode" to true)
    ) {
        override fun read() {
            push(this@streamify)
            destroy()
        }
    }

actual fun Iterable<N3Triple>.streamify(): ReadableNodeStream<N3Triple> =
    object: ReadableNodeStream<N3Triple>(
        options = dyn("objectMode" to true)
    ) {
        override fun read() {
            this@streamify.forEach { triple -> push(triple) }
            destroy()
        }
    }

actual fun fromFile(filepath: String): InputStream<String> {
    return createReadFileStream(filepath)
}

actual inline fun <T> InputStream<T>.consume(crossinline block: (T) -> Unit): Stream {
    val consumer = createWritableNodeStream<T> { if (it != null) { block(it) } }
    this.pipe(consumer)
    return consumer
}


inline fun <T> createWritableNodeStream(
    objectMode: Boolean = true,
    crossinline block: suspend (data: T) -> Unit
): WritableNodeStream<T> {
    val opts: dynamic = Any()
    opts.objectMode = objectMode
    val stream = WritableNodeStream<T>(opts)
    stream.onReceive = { data, _, callback ->
        block(data)
        callback()
    }
    return stream
}

inline fun <T> createReadableNodeStream(
    objectMode: Boolean = true
): ReadableNodeStream<T> {
    val opts: dynamic = Any()
    opts.objectMode = objectMode
    return ReadableNodeStream(opts)
}
