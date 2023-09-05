package be.ugent.idlab.predict.ldests.rdf

actual sealed interface TripleProvider

actual class LocalResource private constructor(
    actual val data: TripleStore
): TripleProvider {

    actual companion object {
        actual suspend fun from(filepath: String): LocalResource {
            TODO("Not yet implemented")
        }

        actual fun wrap(buffer: TripleStore): LocalResource {
            TODO("Not yet implemented")
        }

    }

}
