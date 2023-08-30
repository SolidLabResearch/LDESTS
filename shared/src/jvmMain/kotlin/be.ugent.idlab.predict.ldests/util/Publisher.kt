package be.ugent.idlab.predict.ldests.util

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*

private val client = HttpClient()

actual suspend fun fetch(
    url: String,
    vararg headers: Pair<String, List<String>>,
): String? =
    try {
        val response = client.get(url) {
            headersOf(*headers)
        }
        val (code, message) = response.status
        if (code in 200..299) {
            log("Request", "GET: $code $message - $url")
            response.body()
        } else {
            error("Request", "GET: $code $message - $url")
            error("Request", response.body())
            null
        }
    } catch (e: Exception) {
        error("Fetch", "Caught exception `${e::class.simpleName}` while fetching: ${e.message}")
        null
    }
