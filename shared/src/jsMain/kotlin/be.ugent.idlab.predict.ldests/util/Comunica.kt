package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.lib.extra.AsyncIterator
import be.ugent.idlab.predict.ldests.lib.extra.asyncIteratorToGenerator
import be.ugent.idlab.predict.ldests.lib.node.ReadableNodeStream

fun <T> AsyncIterator<T>.toStream(objectMode: Boolean = true): ReadableNodeStream<T> {
    val options: dynamic = Any()
    options.objectMode = objectMode
    return ReadableNodeStream.from(
        generator = asyncIteratorToGenerator(this),
        options = options
    )
}
