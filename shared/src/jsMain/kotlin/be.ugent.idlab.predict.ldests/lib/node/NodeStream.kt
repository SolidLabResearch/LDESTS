@file:JsModule("stream")
package be.ugent.idlab.predict.ldests.lib.node

external interface NodeStream {

    fun on(event: String, callback: (data: dynamic) -> Unit): NodeStream

    fun destroy(error: Error? = definedExternally)

}

@JsName("Readable")
open external class ReadableNodeStream: NodeStream {

    fun pipe(to: NodeStream)

    override fun destroy(error: Error?)

    override fun on(event: String, callback: (data: dynamic) -> Unit): ReadableNodeStream

}

@JsName("Writable")
external class WritableNodeStream<T> internal constructor(
    options: dynamic
): NodeStream {

    @JsName("_write")
    internal var onReceive: suspend (data: T?, encoding: String, callback: () -> Unit) -> Unit

    override fun on(event: String, callback: (data: dynamic) -> Unit): WritableNodeStream<T>

    override fun destroy(error: Error?)

}
