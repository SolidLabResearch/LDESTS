@file:JsModule("@comunica/query-sparql-incremental")

package be.ugent.idlab.predict.ldests.lib.rdf

import kotlin.js.Promise

@JsName("QueryEngine")
external class IncremunicaQueryEngine {

    @JsName("queryBindings")
    fun query(query: String, options: dynamic) : Promise<ComunicaBindingStream>

}
