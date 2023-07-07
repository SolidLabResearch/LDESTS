@file:JsModule("@comunica/incremental-rdf-streaming-store")

package be.ugent.idlab.predict.ldests.lib.rdf

import be.ugent.idlab.predict.ldests.lib.node.ReadableNodeStream

@JsName("StreamingStore")
external class StreamingStore() {

    fun import(stream: ReadableNodeStream<N3Triple>)

}
