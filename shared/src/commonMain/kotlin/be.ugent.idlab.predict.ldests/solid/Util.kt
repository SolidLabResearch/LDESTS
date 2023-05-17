package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.rdf.Triple


// connection helpers

fun <T> Collection<T>.string(cap: Int = 3, transform: (T) -> String): String {
    return if (isEmpty()) {
        "< empty >"
    } else if (size < cap) {
        "[ " + this.joinToString(", ") { transform(it).ifBlank { "< blank >" } } + " ]"
    } else {
        var result = "[ "
        val it = iterator()
        for (i in 0 until cap) {
            result += transform(it.next()).ifBlank { "< blank >, " }
        }
        result + "... (${size - cap} more) ]"
    }
}

fun Collection<SolidConnection.Resource>.string(cap: Int = 3): String {
    return string(cap = cap) { it.path }
}

fun Collection<Triple>.string(cap: Int = 2): String {
    return string(cap = cap) { "${it.subject.value} - ${it.predicate.value} - ${it.`object`.value}" }
}
