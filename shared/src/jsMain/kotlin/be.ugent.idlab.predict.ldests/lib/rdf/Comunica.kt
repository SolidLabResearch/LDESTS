@file:JsModule("@comunica/query-sparql")
package be.ugent.idlab.predict.ldests.lib.rdf

import be.ugent.idlab.predict.ldests.lib.node.ReadableNodeStream
import kotlin.js.Promise

@JsName("QueryEngine")
external class ComunicaQueryEngine {

    @JsName("queryBindings")
    fun query(query: String, options: dynamic) : Promise<ComunicaBindingStream>

}

external class ComunicaBindingStream: ReadableNodeStream

external interface BindingStreamValue {
    fun get(type: String) : BindingStreamProperty
}

external interface BindingStreamProperty {
    val value: String
    val termType: String
}
