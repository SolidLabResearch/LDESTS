package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.remote.Triple


// connection helpers

fun <T> List<T>.string(cap: Int = 3, transform: (T) -> String): String {
    return if (isEmpty()) {
        "< empty >"
    } else if (size < cap) {
        "[ " + this.joinToString(", ") { transform(it).ifBlank { "<root>" } } + " ]"
    } else {
        "[ " + this.subList(0, cap).joinToString(", ") {
            transform(it).ifBlank { "<root>" }
        } + ", ... (${size - cap} more) ]"
    }
}

fun List<SolidConnection.Resource>.string(): String {
    return string { it.path }
}

fun List<Triple>.string(): String {
    return string(cap = 2) { "${it.s} - ${it.p} - ${it.o}" }
}
