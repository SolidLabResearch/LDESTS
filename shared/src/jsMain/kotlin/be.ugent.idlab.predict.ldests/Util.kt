package be.ugent.idlab.predict.ldests

import io.ktor.util.date.*

private val _start = getTimeMillis()

fun time(): String = (getTimeMillis() - _start).toString().padStart(6, '0')

fun fmt(level: String, loc: String?, text: String): String {
    return "${time()} | $level - ${(loc ?: "Anonymous").padEnd(16).substring(0, 16)} | $text"
}

// have to use `console.log` everywhere as IntelliJ otherwise doesn't respect the order of logging

actual inline fun <reified T> T.log(text: String) {
    console.log(fmt(
        level = "LOG",
        loc = T::class.simpleName,
        text = text
    ))
}

actual inline fun <reified T> T.warn(text: String) {
    console.log("\u001b[33m" + fmt(
        level = "WRN",
        loc = T::class.simpleName,
        text = text
    ) + "\u001b[0m")
}

actual inline fun <reified T> T.error(text: String) {
    console.log("\u001b[31m" + fmt(
        level = "ERR",
        loc = T::class.simpleName,
        text = text
    ) + "\u001b[0m")
}
