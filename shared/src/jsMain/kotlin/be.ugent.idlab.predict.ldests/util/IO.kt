package be.ugent.idlab.predict.ldests.util

import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
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
): Int? {
    try {
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
    } catch (t: Throwable) {
        error("Request", "${type.name}: ${t.message}")
        return null
    }
}


actual suspend fun fetch(
    url: String,
    vararg headers: Pair<String, List<String>>
): String? {
    val response = fetchJs(
        url = url,
        options = dyn(
            "method" to "GET",
            "headers" to headers
                .map { (key, values) -> key to values.joinToString(separator = ",") }
                .toDynamic()
        )
    ).await()
    val status = response.status as Int
    return if (status in 200..299) {
        log("Request", "GET: $status ${response.statusText} - $url")
        (response.text() as Promise<String>).await()
    } else {
        error("Request", "GET: $status ${response.statusText} - $url")
        error("Request", (response.text() as Promise<String>).await())
        null
    }
}

// can throw
actual suspend fun readFile(filename: String): String = suspendCoroutine { continuation ->
    be.ugent.idlab.predict.ldests.lib.node.readFile(
        filename = filename
    ) { err, data ->
        if (data != null) {
            // as the regular `data` is suddenly an object (probably being boxed here?), the JS `toString()` is crucial
            continuation.resume(js("data.toString()"))
        } else {
            continuation.resumeWithException(err ?: RuntimeException())
        }
    }
}
