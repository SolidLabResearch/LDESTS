package be.ugent.idlab.predict.ldests

import io.ktor.util.date.*

private val _start = getTimeMillis()

fun time(): String = (getTimeMillis() - _start).toString().padStart(6, '0')

fun fmt(level: String, loc: String?, text: String): String {
    return "${time()} | $level - ${(loc ?: "Anonymous").padEnd(16).substring(0, 16)} | $text"
}

actual inline fun <reified T> T.log(text: String) {
    console.log(fmt(
        level = "LOG",
        loc = T::class.simpleName,
        text = text
    ))
}

actual inline fun <reified T> T.warn(text: String) {
    console.warn(fmt(
        level = "WRN",
        loc = T::class.simpleName,
        text = text
    ))
}

actual inline fun <reified T> T.error(text: String) {
    console.error(fmt(
        level = "ERR",
        loc = T::class.simpleName,
        text = text
    ))
}
