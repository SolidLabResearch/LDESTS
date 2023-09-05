package be.ugent.idlab.predict.ldests.util

// TODO: expect object Network, FS, others?
//  and then expand them here for overloads (like with fetch)
enum class SubmitRequestType {
    POST, PATCH, PUT
}

/**
 * Submits the data and returns its status code. If the status code is `null`, the request did not succeed
 */
expect suspend fun submit(
    type: SubmitRequestType,
    url: String,
    headers: List<Pair<String, String>>,
    body: String
): Int?

expect suspend fun fetch(
    url: String,
    vararg headers: Pair<String, List<String>>,
): String?

suspend fun fetch(
    url: String,
    vararg headers: Pair<String, String>
): String? = fetch(
    url = url,
    headers = headers.map { (key, value) -> key to listOf(value) }.toTypedArray()
)

// can throw
expect suspend fun readFile(filename: String): String
