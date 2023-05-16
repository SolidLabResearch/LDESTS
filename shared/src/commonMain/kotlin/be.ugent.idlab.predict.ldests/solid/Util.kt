package be.ugent.idlab.predict.ldests.solid


// connection helpers

fun List<SolidConnection.Data>.string(): String {
    return if (isEmpty()) {
        "< empty >"
    } else if (size < 3) {
        "[ " + this.joinToString(", ") { it.path.ifBlank { "<root>" } } + " ]"
    } else {
        "[ " + this.subList(0, 3).joinToString(", ") { it.path.ifBlank { "<root>" } } + ", ... (${size - 3} more) ]"
    }
}
