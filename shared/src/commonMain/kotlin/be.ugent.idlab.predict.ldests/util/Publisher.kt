package be.ugent.idlab.predict.ldests.util

enum class SubmitRequestType {
    POST, PATCH, PUT
}

expect suspend fun submit(
    type: SubmitRequestType,
    url: String,
    headers: List<Pair<String, String>>,
    body: String
): Int

expect suspend fun fetch(
    url: String,
    headers: List<Pair<String, String>>,
): String?
