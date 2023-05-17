package be.ugent.idlab.predict.ldests.api

@ExperimentalJsExport
@JsExport
@JsName("Triple")
data class Triple(
    val s: String,
    val p: String,
    val o: String
)