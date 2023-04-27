package be.ugent.idlab.predict.ldests.remote

import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// helper method to create triples from binding stream values
fun BindingStreamValue.toTriple(): Triple {
    return object: Triple {
        override val s = get("s").value
        override val p = get("p").value
        override val o = get("o").value
    }
}

actual suspend fun query(query: String, url: String, onValueReceived: (Triple) -> Unit) {
    // creating the stream with the url as option
    val options: dynamic = Any()
    options.sources = arrayOf(url)
    val stream = ComunicaQueryEngine()
        .query(
            query = query,
            options = options
        )
        .await()
    // blocking this coroutine until the end of the stream has been reached
    suspendCoroutine { continuation ->
        stream.on("data") { value: BindingStreamValue ->
            onValueReceived(value.toTriple())
        }.on("error") {
            continuation.resumeWithException(RuntimeException("Error occurred during SPARQL-query!"))
        }.on("end") {
            continuation.resume(Unit)
        }
    }
}
