package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.remote.Queries
import be.ugent.idlab.predict.ldests.remote.Triple
import be.ugent.idlab.predict.ldests.remote.query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SolidConnection private constructor(
    private val url: String
) {

    class Builder internal constructor() {
        var authorisation: String? = null
        var rootDir: String = ""
    }

    inner class Directory internal constructor() {

        // TODO: replace with `lazy {}`-alike use
        suspend fun getFiles(): List<String> {
            // it is ensured through the API that these suspend funcs are running on the parent's scope,
            //  so as long as it isn't canceled, the connection should be valid
            TODO("Not yet implemented")
        }

        // TODO: replace with `lazy {}`-alike use
        suspend fun getDirectories(): List<Directory> {
            // it is ensured through the API that these suspend funcs are running on the parent's scope,
            //  so as long as it isn't canceled, the connection should be valid
            TODO("Not yet implemented")
        }

    }

    inner class File internal constructor() {

        suspend fun read(): List<Triple> {
            // TODO: actual implementation using the file's URL
            val result = mutableListOf<Triple>()
            query(
                url = url,
                query = Queries.SPARQL_GET_ALL(limit = 5),
                onValueReceived = { result.add(it) }
            )
            return result
        }

        fun write(content: List<Triple>) {
            scope.launch {
                TODO("Not yet implemented")
            }
        }

    }

    // the scope used by the various async "commit" calls to execute their jobs with
    // this scope can be canceled by the connection, killing all active navigators
    // TODO: lambda interface that allows for file manip (r/w) through this scope (instead of suspend functions on the API!)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    // TODO: use lambda here on scope instead
    fun open(): Directory? {
        TODO("Not yet implemented")
    }

    companion object {

        operator fun invoke(
            url: String,
            options: Builder.() -> Unit
        ): SolidConnection {
            val builder = Builder()
            builder.options()
            return SolidConnection(
                url = url
                /* TODO: apply the options received from the builder where relevant */
            )
        }

    }

}