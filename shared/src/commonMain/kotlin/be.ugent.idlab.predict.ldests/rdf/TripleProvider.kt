package be.ugent.idlab.predict.ldests.rdf

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred

// represents all supported possible ways of obtaining triples (data)
// includes local files and remote
expect sealed interface TripleProvider

// various possible triple providers
expect class LocalResource private constructor(
    filepath: String,
    scope: CoroutineScope
): TripleProvider {

    val data: Deferred<TripleStore>

    companion object {
        // TODO: validity checking
        fun from(filepath: String, scope: CoroutineScope): LocalResource

    }

}

class RemoteResource private constructor(
    val url: String
    /* No buf here, as there's no guarantee that the contents remain unchanged as-is */
): TripleProvider {

    companion object {

        fun from(url: String) = RemoteResource(url = url)

    }

}
