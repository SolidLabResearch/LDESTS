@file:JsModule("base")

package be.ugent.idlab.predict.ldests.lib.rdf

import be.ugent.idlab.predict.ldests.lib.node.ReadableNodeStream

@JsName("StreamingStore")
external class IncremunicaStreamingStore() {

    fun import(stream: ReadableNodeStream<N3Triple>)

    fun end()

}
