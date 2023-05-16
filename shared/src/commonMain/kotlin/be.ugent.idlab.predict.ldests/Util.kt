package be.ugent.idlab.predict.ldests

import be.ugent.idlab.predict.ldests.remote.Queries
import be.ugent.idlab.predict.ldests.remote.Triple
import be.ugent.idlab.predict.ldests.remote.query
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class LDESTS private constructor(private val triples: List<Triple>) {

    companion object {

        suspend fun fromUrl(url: String): LDESTS {
            val triples = mutableListOf<Triple>()
            query(
                query = Queries.SPARQL_GET_ALL(5),
                url = url
            ) { triple ->
                triples.add(triple)
            }
            return LDESTS(triples = triples)
        }

    }

    override fun toString() =
        "LDESTS [${triples.map { it.s }.distinct().take(3).joinToString(", ")}]"

}

fun <T> CoroutineScope.lazy(
    context: CoroutineContext = Dispatchers.Unconfined,
    initializer: suspend CoroutineScope.() -> T
): Deferred<T> = async(context = context, start = CoroutineStart.LAZY, block = { this.initializer() })

expect inline fun <reified T> T.log(text: String)

expect inline fun <reified T> T.warn(text: String)

expect inline fun <reified T> T.error(text: String)
