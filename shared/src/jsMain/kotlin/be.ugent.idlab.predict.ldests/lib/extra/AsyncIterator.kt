@file:JsModule("asynciterator")
package be.ugent.idlab.predict.ldests.lib.extra

external interface AsyncIterator<T> {
    fun read(): T?

    fun once(event: String, callback: (data: dynamic) -> Unit)

}
