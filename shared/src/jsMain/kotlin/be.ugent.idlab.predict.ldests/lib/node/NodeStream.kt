@file:JsModule("stream")
package be.ugent.idlab.predict.ldests.lib.node

import be.ugent.idlab.predict.ldests.lib.extra.AsyncGenerator

external interface NodeStream {

    fun on(event: String, callback: (data: dynamic) -> Unit): NodeStream

    fun destroy(error: Error? = definedExternally)

}

@JsName("Readable")
open external class ReadableNodeStream<T>(
    options: dynamic
): NodeStream {

    open fun pipe(to: NodeStream)

    open fun push(data: T?)

    @JsName("_read")
    open fun read()

    override fun on(event: String, callback: (data: dynamic) -> Unit): ReadableNodeStream<T>

    override fun destroy(error: Error?)

    companion object {

        fun <T> from(generator: AsyncGenerator<T, *, *>, options: dynamic): ReadableNodeStream<T>

    }


}

@JsName("Writable")
open external class WritableNodeStream<T>(
    options: dynamic
): NodeStream {

    @JsName("_write")
    var onReceive: suspend (data: T, encoding: String, callback: () -> Unit) -> Unit

    override fun on(event: String, callback: (data: dynamic) -> Unit): WritableNodeStream<T>

    override fun destroy(error: Error?)

}
