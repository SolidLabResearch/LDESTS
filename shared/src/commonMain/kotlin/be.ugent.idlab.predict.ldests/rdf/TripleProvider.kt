package be.ugent.idlab.predict.ldests.rdf

// represents all supported possible ways of obtaining triples (data)
// includes local files and remote
sealed interface TripleProvider

// various possible triple providers
// TODO: add local caches to improve IO
class LocalResource private constructor(
    val filepath: String
): TripleProvider {

    companion object {
        // TODO: validity checking
        fun from(path: String) = LocalResource(filepath = path)

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
