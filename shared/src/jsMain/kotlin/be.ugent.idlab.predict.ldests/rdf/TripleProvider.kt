package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.node.createReadFileStream
import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.util.join
import be.ugent.idlab.predict.ldests.util.lazy
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.mapToTriples
import kotlinx.coroutines.CoroutineScope

actual sealed interface TripleProvider

actual class LocalResource private actual constructor(
    filepath: String,
    scope: CoroutineScope
): TripleProvider {

    actual val data = scope.lazy {
        log("Reading from `$filepath` to get local triples!")
        val store = N3Store()
        createReadFileStream(filepath)
            .mapToTriples()
            .on("data") { store.add(it) }
            .join()
        log("Read ${store.size} triples!")
        TripleStore(store)
    }

    actual companion object {

        actual fun from(filepath: String, scope: CoroutineScope) = LocalResource(
            filepath = filepath,
            scope = scope
        )

    }

}