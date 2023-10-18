@file:JsModule("@incremunica/incremental-rdf-streaming-store")

package be.ugent.idlab.predict.ldests.lib.rdf

@JsName("StreamingStore")
external class IncremunicaStreamingStore() {

    @JsName("addQuad")
    fun add(triple: N3Triple)

    @JsName("end")
    fun close()

}
