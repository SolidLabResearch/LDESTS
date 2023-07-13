package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.util.InputStream


// represents all supported possible ways of obtaining triples (data)
// includes local files and remote
expect sealed interface TripleProvider

// various possible triple providers
expect class LocalResource: TripleProvider {

    val data: TripleStore

    companion object {
        // TODO: validity checking
        suspend fun from(filepath: String): LocalResource

        fun wrap(buffer: TripleStore): LocalResource

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

expect class StreamingResource(): TripleProvider {

    fun add(stream: InputStream<Triple>)

    fun stop()

}
