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

enum class LogLevel {
    LOG, WARN, ERROR
}

var logLevel: LogLevel = LogLevel.LOG

expect inline fun <reified T> T.log(text: String)

expect inline fun <reified T> T.warn(text: String)

expect inline fun <reified T> T.error(text: String)

expect inline fun log(location: String?, text: String)

expect inline fun warn(location: String?, text: String)

expect inline fun error(location: String?, text: String)

fun Throwable.formatted(): List<String> {
    val msg = message ?: return listOf()
    fun String.shorten(length: Int): String {
        return if (this.length > length) {
            substring(0, length - 3) + "..."
        } else {
            this
        }
    }
    return msg.split("\n")
        .map { it.trim().shorten(128) }
        .filter { it.isNotBlank() }
}

inline fun <reified T> T.warn(throwable: Throwable) {
    val f = throwable.formatted()
    when (f.size) {
        0 -> warn("No message provided")
        1 -> warn(f.first())
        else -> {
            warn("┏ ${f.first()}")
            for (i in 1 until f.size - 1) {
                warn("┃ ${f[i]}")
            }
            warn("┗ ${f.last()}")
        }
    }
}


inline fun <reified T> T.error(throwable: Throwable) {
    val f = throwable.formatted()
    when (f.size) {
        0 -> error("No message provided")
        1 -> error(f.first())
        else -> {
            error("┏ ${f.first()}")
            for (i in 1 until f.size - 1) {
                error("┃ ${f[i]}")
            }
            error("┗ ${f.last()}")
        }
    }
}
