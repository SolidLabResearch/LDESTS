package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.rdf.StreamingResource
import be.ugent.idlab.predict.ldests.rdf.Triple
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

/**
 * Imports triples in a separate job (same scope) and finishes this job when the triples have been exhausted.
 * IMPORTANT: this is ongoing in the background! It will only get canceled if the parent scope/job gets canceled and
 *  will otherwise only end (including calling `close()`) normally. The behaviour of calling `close()` on the returned
 *  resource is platform dependant is generally not recommended.
 */
suspend fun Flow<Triple>.toAsyncResource(): StreamingResource {
    val resource = StreamingResource()
    coroutineScope {
        launch { insert(resource) }
    }
    return resource
}

/**
 * Appends the data from `this` flow into the provided resource. Blocks until completed (or canceled) and closes the
 *  stream (regardless of whether it was canceled
 */
private suspend fun Flow<Triple>.insert(into: StreamingResource) {
    try {
        collect { triple -> into.add(triple) }
    } finally {
        into.close()
    }
    coroutineContext.ensureActive()
}
