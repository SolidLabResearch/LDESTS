@file:JsModule("stream")
package be.ugent.idlab.predict.ldests.lib.node

external interface NodeStream {

    fun on(event: String, callback: (data: dynamic) -> Unit): NodeStream

    fun destroy(error: Error? = definedExternally)

}
