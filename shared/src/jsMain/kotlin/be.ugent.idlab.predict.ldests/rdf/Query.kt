package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.node.createReadFileStream
import be.ugent.idlab.predict.ldests.lib.rdf.*
import be.ugent.idlab.predict.ldests.util.createStreamParser
import be.ugent.idlab.predict.ldests.util.createWritableNodeStream
import kotlinx.coroutines.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    suspendCancellableCoroutine { continuation ->
        stream.on("data") { value: BindingStreamValue ->
            onValueReceived(
                // FIXME: other nodes or something
                Triple(
                    subject = N3NamedNode(value.get("s").value),
                    predicate = N3NamedNode(value.get("p").value),
                    `object` = N3NamedNode(value.get("o").value)
                )
            )
        }.on("error") {
            continuation.resumeWithException(it as Error)
        }.on("end") {
            continuation.resume(Unit)
        }
        continuation.invokeOnCancellation {
            // not propagating the cancel exception provided, as this is not an "error"
            stream.destroy()
        }
    }
}

internal actual suspend fun file(
    filename: String,
    onValueReceived: (Triple) -> Unit
) = suspendCancellableCoroutine { continuation ->
    val fileStream = createReadFileStream(filename)
    val resultStream = createWritableNodeStream<Triple> {
        if (it != null) {
            onValueReceived(it)
        }
    }
    val parseStream = createStreamParser(resultStream)
    fileStream.pipe(parseStream)
    continuation.invokeOnCancellation {
        // stopping the file stream, which should cancel the following parts as well
        // not propagating the cancel exception provided, as this is not an "error"
        fileStream.destroy()
    }
    resultStream.on("finish") {
        continuation.resume(Unit)
    }
    resultStream.on("error") { error: Error ->
        fileStream.destroy(error)
        continuation.resumeWithException(error)
    }
}
