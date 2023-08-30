package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.ComunicaBinding
import be.ugent.idlab.predict.ldests.lib.rdf.ComunicaQueryEngine
import be.ugent.idlab.predict.ldests.util.dyn
import be.ugent.idlab.predict.ldests.util.error
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.await
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual typealias Binding = ComunicaBinding

actual suspend fun TripleProvider.query(query: Query, callback: (ComunicaBinding) -> Unit) {
    val stream = try {
        when (this) {
            is LocalResource -> ComunicaQueryEngine()
                    .query(query.sparql, dyn("sources" to arrayOf(data.store)))
                    .await()

            is RemoteResource -> {
                // fallback for an issue with remote lookup in comunica
                val resource = this.toLocalResource() ?: run {
                    error("A query on `${this::class.simpleName}` failed during initialisation: `RemoteResource` could not be converted to a `LocalResource`!")
                    return
                }
                ComunicaQueryEngine()
                    .query(query.sparql, dyn("sources" to arrayOf(resource.data.store)))
                    .await()
            }

//            is StreamingResource -> IncremunicaQueryEngine()
//                .query(query.sparql, dyn("sources" to arrayOf(stream)))
//                .await()

            else -> throw RuntimeException("Unrecognized triple provider used in `query`!")
        }
    } catch (t: Throwable) {
        error("A query on `${this::class.simpleName}` failed during initialisation: ${t.message?.substringBefore('\n')}")
        // ensure the coroutine is still active, so rethrow if necessary
        coroutineContext.ensureActive()
        // the callback is never used, so returning early
        return
    }
    suspendCancellableCoroutine { continuation ->
        stream.on("data", callback)
        continuation.invokeOnCancellation {
            error("A query on `${this::class.simpleName}` has been canceled")
            stream.destroy(Error(it))
        }
        stream.on("end") {
            log("A query on `${this::class.simpleName}` finished successfully")
            continuation.resume(Unit)
        }
        stream.on("error") { t: Throwable ->
            error("A query on `${this::class.simpleName}` failed during query execution: ${t.message?.substringBefore('\n')}")
            continuation.resumeWithException(t)
        }
    }
}

actual operator fun ComunicaBinding.get(variable: String): Term? = get(variable)
