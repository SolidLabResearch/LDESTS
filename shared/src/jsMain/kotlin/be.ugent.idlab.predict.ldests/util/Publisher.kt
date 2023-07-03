package be.ugent.idlab.predict.ldests.util

import kotlinx.coroutines.await
import kotlin.js.Promise

external fun fetch(
    url: String,
    options: dynamic = definedExternally
): Promise<dynamic>

actual suspend fun request(type: RequestType, url: String, headers: List<Pair<String, String>>, body: String): Int {
    val response = fetch(
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
