@file:JsModule("@comunica/query-sparql")
package be.ugent.idlab.predict.ldests.lib.rdf

import be.ugent.idlab.predict.ldests.lib.extra.AsyncIterator
import kotlin.js.Promise

@JsName("QueryEngine")
external class ComunicaQueryEngine {

    @JsName("queryBindings")
    fun query(query: String, options: dynamic) : Promise<ComunicaBindingStream>

}

external class ComunicaBindingStream: AsyncIterator<ComunicaBinding> {

    fun destroy(error: Error?)

    fun on(event: String, callback: (value: dynamic) -> Unit): ComunicaBindingStream

    override fun read(): ComunicaBinding?

    override fun once(event: String, callback: (data: dynamic) -> Unit)

}

external class ComunicaBinding {

    fun get(name: String): N3Term?

    @JsName("toString")
    fun toPrettyString(): String

}
