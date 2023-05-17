package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.rdf.lib.BindingStreamValue
import be.ugent.idlab.predict.ldests.rdf.lib.ComunicaQueryEngine
import be.ugent.idlab.predict.ldests.rdf.lib.NamedNode
import kotlinx.coroutines.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

actual typealias Triple = be.ugent.idlab.predict.ldests.rdf.lib.Triple
actual typealias Term = be.ugent.idlab.predict.ldests.rdf.lib.Term

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
            onValueReceived(
                // FIXME: other nodes or something
                Triple(
                    subject = NamedNode(value.get("s").value),
                    predicate = NamedNode(value.get("p").value),
                    `object` = NamedNode(value.get("o").value)
                )
            )
        }.on("error") {
            continuation.resumeWithException(RuntimeException("Error occurred during query execution!"))
        }.on("end") {
            continuation.resume(Unit)
        }
    }
}
