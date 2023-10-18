package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.IncremunicaStreamingStore
import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.readFile

actual sealed interface TripleProvider

actual class LocalResource private constructor(
    actual val data: TripleStore
): TripleProvider {

    actual companion object {

        actual suspend fun from(filepath: String) = LocalResource(
            data = run {
                log("Reading from `$filepath` to get local triples!")
                val turtle = readFile(filepath)
                TripleStore(N3Store(/* Empty initial store */)).apply {
                    // do we need a path for local files?
                    insert(context = RDFBuilder.Context(path = ""), turtle = turtle)
                    log("LocalResource", "Read ${store.size} triples!")
                }
            }
        )

        actual fun wrap(buffer: TripleStore) = LocalResource(data = buffer)

    }

}

actual class StreamingResource(
    internal val stream: IncremunicaStreamingStore
): TripleProvider {

    actual constructor(): this(stream = IncremunicaStreamingStore())

    actual fun add(triple: Triple) {
        stream.add(triple)
    }

    actual fun close() {
        stream.close()
    }

}
