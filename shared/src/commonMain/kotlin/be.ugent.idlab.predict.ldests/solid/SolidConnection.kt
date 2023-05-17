package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.lazy
import be.ugent.idlab.predict.ldests.log
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.LDP.isDirectory
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.LDP.isFile
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.RDF.types
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.group
import be.ugent.idlab.predict.ldests.remote.Queries
import be.ugent.idlab.predict.ldests.remote.Query
import be.ugent.idlab.predict.ldests.remote.Triple
import kotlinx.coroutines.*

class SolidConnection private constructor(
    private val url: String
) {

    // the scope used by the various async "fetch/commit" calls to execute their jobs with
    // this scope can be canceled by the connection, killing all active components
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    /** Represents anything that could be inside a pod (either files or directories) **/
    sealed class Resource(
        /** The connection this data is part of **/
        val connection: SolidConnection,
        /** The entire path of the object (relative to the root of the pod!) **/
        val path: String
    ) {

        protected var data = connection.scope.lazy {
            // getting all the triples, so files/directories can be extracted from it
            Query.queryCatching(Queries.SPARQL_GET_ALL, connection.url + path)
        }
        private set

        open fun invalidate() {
            // resetting the deferred call
            data = connection.scope.lazy {
                // getting all the triples, so files/directories can be extracted from it
                Query.queryCatching(Queries.SPARQL_GET_ALL, connection.url + path)
            }
        }

    }

    inner class Directory internal constructor(
        path: String
    ): Resource(connection = this@SolidConnection, path = path) {

        private fun createDeferredFileCall() = scope.lazy {
            // getting the data, and parsing its predicates
            val result = mutableListOf<File>()
            data.await().group().forEach {
                if (it.types().isFile()) {
                    // the subject represent the location, and should be the same for every triple through the grouping
                    //  action from data
                    result.add(File(it.first().s.removePrefix(url)))
                }
            }
            result
        }

        private fun createDeferredDirCall() = scope.lazy {
            // getting the data, and parsing its predicates
            val result = mutableListOf<Directory>()
            data.await().group().forEach {
                // skip this entry if it is this path
                if (it.first().s.removePrefix(url) == path) {
                    return@forEach
                }
                if (it.types().isDirectory()) {
                    // the subject represent the location, and should be the same for every triple through the grouping
                    //  action from data
                    result.add(Directory(it.first().s.removePrefix(url)))
                }
            }
            result
        }

        private var files: Deferred<List<File>> = createDeferredFileCall()

        private var directories: Deferred<List<Directory>> = createDeferredDirCall()

        override fun invalidate() {
            super.invalidate()
            // resetting the deferred calls
            files = createDeferredFileCall()
        }

        suspend fun files() = files.await()

        suspend fun directories() = directories.await()

    }

    inner class File internal constructor(
        path: String
    ): Resource(connection = this@SolidConnection, path = path) {

        suspend fun content(): List<Triple> {
            return data.await()
        }

//        fun write(content: List<Triple>) {
//            scope.launch {
//                // The inner data param would also have to be refreshed, so refresh has to be called here
//                // somewhat wasteful to obtain the data again after just submitting it (instead of updating data directly),
//                // but it gives more guarantees
//                TODO("Not yet implemented")
//            }
//        }

    }

    class Builder internal constructor() {
        /** Configurable options in the builder scope **/
        var authorisation: String? = null
        var rootDir: String = ""

        internal fun build(base: String): SolidConnection {
            // cleaning of the variables can be done here
            val url = base + rootDir
            return SolidConnection(
                url = url
                /* TODO: apply other options, e.g. preparing auth, maybe using suspension func */
            )
        }

    }

    val root = Directory("")

    /**
     * Cancels all ongoing IO
     */
    suspend fun close() {
        log("Closing the connection to '$url'")
        job.cancelAndJoin()
        log("Shut down successfully")
    }

    companion object {

        operator fun invoke(
            url: String,
            options: Builder.() -> Unit = {}
        ): SolidConnection {
            val builder = Builder()
            // applying the options
            builder.options()
            // creating the resulting connection
            val con = builder.build(url)
            log("Built a Solid connection (${con.url})")
            return con
        }

        internal operator fun invoke(
            url: String,
            config: Builder
        ): SolidConnection {
            // using the given config directly to build the connection
            val con = config.build(url)
            log("Built a Solid connection (${con.url})")
            return con
        }

    }

}
