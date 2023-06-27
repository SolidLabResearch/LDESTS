@file:Suppress("NOTHING_TO_INLINE")

package be.ugent.idlab.predict.ldests.util

import kotlin.js.Date

private val _start = Date.now()

fun time(): String = (Date.now() - _start).toString().padStart(6, '0')

fun fmt(level: String, loc: String?, text: String): String {
    return "${time()} | $level - ${(loc ?: "Unknown").padEnd(16).substring(0, 16)} | $text"
}

// have to use `console.log` everywhere as IntelliJ otherwise doesn't respect the order of logging

actual inline fun <reified T> T.log(text: String) {
    log(T::class.simpleName, text)
}

actual inline fun log(location: String?, text: String) {
    if (logLevel.ordinal > LogLevel.LOG.ordinal) {
        return
    }
    console.log(
        fmt(
            level = "LOG",
            loc = location,
            text = text
        )
    )
}

actual inline fun <reified T> T.warn(text: String) {
    warn(T::class.simpleName, text)
}

actual inline fun warn(location: String?, text: String) {
    if (logLevel.ordinal > LogLevel.WARN.ordinal) {
        return
    }
    console.log("\u001b[33m" + fmt(
        level = "WRN",
        loc = location,
        text = text
    ) + "\u001b[0m")
}

actual inline fun <reified T> T.error(text: String) {
    error(T::class.simpleName, text)
}

actual inline fun error(location: String?, text: String) {
    if (logLevel.ordinal > LogLevel.ERROR.ordinal) {
        return
    }
    console.log("\u001b[31m" + fmt(
        level = "ERR",
        loc = location,
        text = text
    ) + "\u001b[0m")
}
