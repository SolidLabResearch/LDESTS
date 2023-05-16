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

internal actual suspend fun query(query: String, url: String, onValueReceived: (Triple) -> Unit) {
    // creating the stream with the url as option
    val options: dynamic = Any()
    options.sources = arrayOf(url)
    val stream = try {
        ComunicaQueryEngine()
            .query(
                query = query,
                options = options
            )
            .await()
    } catch (t: Throwable) {
        // have to catch all throwables, as it typically throws `Error`, which is not a subtype of
        //  `Exception` apparently
        throw RuntimeException(t.message)
    }
    // blocking this coroutine until the end of the stream has been reached
    suspendCoroutine { continuation ->
        stream.on("data") { value: BindingStreamValue ->
            onValueReceived(value.toTriple())
        }.on("error") {
            continuation.resumeWithException(RuntimeException("Error occurred during query execution!"))
        }.on("end") {
            continuation.resume(Unit)
        }
    }
}
