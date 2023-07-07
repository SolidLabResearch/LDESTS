package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.node.finished
import be.ugent.idlab.predict.ldests.lib.rdf.*
import be.ugent.idlab.predict.ldests.util.InputStream
import be.ugent.idlab.predict.ldests.util.dyn
import be.ugent.idlab.predict.ldests.util.error
import be.ugent.idlab.predict.ldests.util.toStream
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

actual suspend fun TripleProvider.query(query: Query): InputStream<Binding>? = when (this) {
    is LocalResource -> {
        // using an intermediate actual N3 store generated from the file, which is used as a source for
        //  the engine
        val store = data.store
        // keeping the store
        val options = dyn("sources" to arrayOf(store))
        try {
            ComunicaQueryEngine()
                .query(query.sparql, options)
                .await()
                .toStream()
        } catch (t: Throwable) {
            error("Query failed: ${t.message}")
            null
        }
    }
    is RemoteResource -> {
        val options = dyn("sources" to arrayOf(url))
        try {
            ComunicaQueryEngine()
                .query(query.sparql, options)
                .await()
                .toStream()
        } catch (t: Throwable) {
            error("Query failed: ${t.message}")
            null
        }
    }
    is StreamingResource -> {
        val options = dyn("sources" to arrayOf(stream))
        try {
            IncremunicaQueryEngine()
                .query(query.sparql, options)
                .await()
                .toStream()
        } catch (t: Throwable) {
            error("Query failed: ${t.message}")
            null
        }
    }
    else -> { throw RuntimeException("Unrecognized triple provider used in `query`!") }
}

actual operator fun ComunicaBinding.get(variable: String): Term? = get(variable)
