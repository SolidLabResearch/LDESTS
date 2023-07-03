package be.ugent.idlab.predict.ldests.util

enum class RequestType {
    GET, POST, PATCH, PUT
}

expect suspend fun request(
    type: RequestType,
    url: String,
    headers: List<Pair<String, String>>,
    body: String
): Int
