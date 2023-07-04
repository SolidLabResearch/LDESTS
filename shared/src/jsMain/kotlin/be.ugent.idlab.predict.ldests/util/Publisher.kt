package be.ugent.idlab.predict.ldests.util

import kotlinx.coroutines.await
import kotlin.js.Promise

@JsName("fetch")
external fun fetchJs(
    url: String,
    options: dynamic = definedExternally
): Promise<dynamic>

actual suspend fun submit(
    type: SubmitRequestType,
    url: String,
    headers: List<Pair<String, String>>,
    body: String
): Int {
    val response = fetchJs(
        url = url,
        options = dyn(
            "method" to type.name,
            "headers" to headers.toDynamic(),
            "body" to body
        )
    ).await()
    val status = response.status as Int
    if (status in 200..299) {
        log("Request", "${type.name}: $status ${response.statusText} - $url")
    } else {
        error("Request", "${type.name}: $status ${response.statusText} - $url")
    }
    return response.status as Int
}


actual suspend fun fetch(
    url: String,
    headers: List<Pair<String, String>>,
): String? {
    val response = fetchJs(
        url = url,
        options = dyn(
            "method" to "GET",
            "headers" to headers.toDynamic()
        )
    ).await()
    val status = response.status as Int
    if (status in 200..299) {
        log("Request", "GET: $status ${response.statusText} - $url")
    } else {
        error("Request", "GET: $status ${response.statusText} - $url")
    }
    return (response.text() as Promise<String>).await()
}