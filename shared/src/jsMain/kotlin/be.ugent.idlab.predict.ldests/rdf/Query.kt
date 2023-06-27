package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.node.createReadFileStream
import be.ugent.idlab.predict.ldests.lib.node.finished
import be.ugent.idlab.predict.ldests.lib.rdf.ComunicaBinding
import be.ugent.idlab.predict.ldests.lib.rdf.ComunicaQueryEngine
import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple
import be.ugent.idlab.predict.ldests.util.*
import kotlinx.coroutines.await

actual typealias Binding = ComunicaBinding


actual suspend fun InputStream<N3Triple>.query(query: Query): InputStream<Binding> {
    // using an intermediate actual N3 store generated from the input stream, which is used as a source for
    //  the engine
    val store = N3Store()
    on("data") { store.add(it) }
    finished(this).await()
    // the resulting store can directly be used in the query engine
    val options: dynamic = Any()
    options.sources = arrayOf(store)
    return ComunicaQueryEngine()
        .query(query.sparql, options)
        .await()
        .toStream()
}

actual suspend fun TripleProvider.query(query: Query): InputStream<Binding> {
    val options: dynamic = Any()
    options.sources = when (this) {
        is LocalResource -> {
            // using an intermediate actual N3 store generated from the file, which is used as a source for
            //  the engine
            val store = data.buf
            // keeping the store
            arrayOf(store)
        }
        is RemoteResource -> arrayOf(url)
    }
    log("Applying querying (variables ${query.variables})")
    return ComunicaQueryEngine()
        .query(query.sparql, options)
        .await()
        .toStream()
}

actual operator fun ComunicaBinding.get(variable: String): Term? = get(variable)
