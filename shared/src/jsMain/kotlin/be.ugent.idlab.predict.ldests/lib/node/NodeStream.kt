@file:JsModule("stream")
package be.ugent.idlab.predict.ldests.lib.node

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

}
