package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.lazy
import be.ugent.idlab.predict.ldests.log
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.LDP.isDirectory
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.LDP.isFile
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.RDF.types
import be.ugent.idlab.predict.ldests.rdf.TripleUtil.group
import be.ugent.idlab.predict.ldests.remote.Queries
import be.ugent.idlab.predict.ldests.remote.Triple
import be.ugent.idlab.predict.ldests.remote.query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class SolidConnection private constructor(
    private val url: String
) {

    // the scope used by the various async "fetch/commit" calls to execute their jobs with
    // this scope can be canceled by the connection, killing all active components
    // TODO: lambda interface that allows for file manip (r/w) through this scope (instead of suspend functions on the API!)
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    class Builder internal constructor() {
        /** Configurable options in the builder scope **/
        var authorisation: String? = null
        var rootDir: String = ""

        internal fun build(base: String): SolidConnection {
            // cleaning of the variables can be done here
            val url = base + rootDir
            log("Built a Solid connection ($url)")
            return SolidConnection(
                url = url
                /* TODO: apply other options */
            )
        }

    }

    /** Represents anything that could be inside a pod (mainly files & directories) **/
    sealed interface Data {
        /** The entire path of the object (relative to the root of the pod!) **/
        val path: String

    }

    inner class Directory internal constructor(
        override val path: String
    ): Data {

        // maybe a flow like approach with lazy init would be better, so refreshing & new attempts are possible?
        private val data = scope.lazy {
            // getting all the triples, so files/directories can be extracted from it
            log("Fetching data for directory '${path.ifBlank { "< root >" }}'")
            // TODO: make this a convenience method (arr + query results instead of query with lambda)
            val result = mutableListOf<Triple>()
            query(Queries.SPARQL_GET_ALL, this@SolidConnection.url + path) {
                // storing all the triples
                // TODO: maybe change this api to only keep relevant triples, using
                //  the LDP(/ LDES,TREE,...) ontology predicates
                result.add(it)
            }
            // grouping once
            result.group()
        }

        val files: Deferred<List<File>> = scope.lazy {
            // getting the data, and parsing its predicates
            val result = mutableListOf<File>()
            data.await().forEach {
                val types = it.types()
                if (types.isFile()) {
                    // the subject represent the location, and should be the same for every triple through the grouping
                    //  action from data
                    result.add(File(it.first().s.removePrefix(url)))
                }
            }
            result
        }

        val directories: Deferred<List<Directory>> = scope.lazy {
            // getting the data, and parsing its predicates
            val result = mutableListOf<Directory>()
            data.await().forEach {
                // skip this entry if it is this path
                if (it.first().s.removePrefix(url) == path) {
                    return@forEach
                }
                val types = it.types()
                if (types.isDirectory()) {
                    // the subject represent the location, and should be the same for every triple through the grouping
                    //  action from data
                    result.add(Directory(it.first().s.removePrefix(url)))
                }
            }
            result
        }

    }

    inner class File internal constructor(
        override val path: String
    ): Data {

//        suspend fun read(): List<Triple> {
//            // TODO: actual implementation using the file's URL
//            val result = mutableListOf<Triple>()
//            query(
//                url = url,
//                query = Queries.SPARQL_GET_ALL(limit = 5),
//                onValueReceived = { result.add(it) }
//            )
//            return result
//        }

//        fun write(content: List<Triple>) {
//            scope.launch {
//                TODO("Not yet implemented")
//            }
//        }

    }

    val root = Directory("")

    companion object {

        operator fun invoke(
            url: String,
            options: Builder.() -> Unit = {}
        ): SolidConnection {
            val builder = Builder()
            // applying the options
            builder.options()
            // creating the resulting connection
            return builder.build(url)
        }

    }

}
