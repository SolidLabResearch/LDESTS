package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.node.createReadFileStream
import be.ugent.idlab.predict.ldests.lib.rdf.IncremunicaStreamingStore
import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple
import be.ugent.idlab.predict.ldests.util.InputStream
import be.ugent.idlab.predict.ldests.util.join
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.mapToTriples

actual sealed interface TripleProvider

actual class LocalResource private constructor(
    actual val data: TripleStore
): TripleProvider {

    actual companion object {

        actual suspend fun from(filepath: String) = LocalResource(
            data = run {
                log("Reading from `$filepath` to get local triples!")
                TODO()
                log("LocalResource", "Read ${store.size} triples!")
            }
        )

        actual fun wrap(buffer: TripleStore) = LocalResource(data = buffer)

    }

}

actual class StreamingResource actual constructor(): TripleProvider {

    init {
        TODO("Unfinished API")
    }

}
