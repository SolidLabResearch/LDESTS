@file:JsModule("@comunica/query-sparql")
package be.ugent.idlab.predict.ldests.rdf.lib

import kotlin.js.Promise

@JsName("QueryEngine")
external class ComunicaQueryEngine {

    @JsName("queryBindings")
    fun query(query: String, options: dynamic) : Promise<BindingStream>

}

external interface BindingStream {
    fun on(name: String, action: (BindingStreamValue) -> Unit): BindingStream
}

external interface BindingStreamValue {
    fun get(type: String) : BindingStreamProperty
}

external interface BindingStreamProperty {
    val value: String
    val termType: String
}
