package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.TripleStore
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.util.RequestType
import be.ugent.idlab.predict.ldests.util.request

class SolidConnection(
    url: String
) {

    val root = Folder(if (url.last() != '/') "$url/" else url)

    open class Resource(
        val url: String
    ) {

        suspend fun read(): TripleStore {
            TODO("Not yet implemented")
        }

        open suspend fun write(block: Turtle.() -> Unit) {
            // `PUT`ting the resource directly
            request(
                type = RequestType.PUT,
                url = url,
                headers = listOf("Content-type" to "text/turtle"),
                body = Turtle(prefixes = Ontology.PREFIXES, block = block)
            )
        }

    }

    class Folder(
        // always has a trailing '/'!
        url: String
    ): Resource(url) {

        override suspend fun write(block: Turtle.() -> Unit) {
            // creating the folder
            request(
                type = RequestType.PUT,
                url = url,
                headers = listOf(),
                body = ""
            )
            // setting the .meta file for the additional data
            request(
                type = RequestType.PATCH,
                url = "$url.meta",
                headers = listOf("Content-type" to "application/sparql-update"),
                // no prefixes used here, as the `INSERT DATA {}` construct doesn't like the `@prefix` syntax
                body = "INSERT DATA { ${Turtle(block = block)} }"
            )
        }

        fun folder(uri: NamedNodeTerm): Folder {
            require(uri.value.startsWith(url) && uri.value.last() == '/')
            return Folder(url = uri.value)
        }

        fun folder(name: String) = Folder(url = "$url$name/")

        fun resource(uri: NamedNodeTerm): Resource {
            require(uri.value.startsWith(url) && uri.value.last() != '/')
            return Resource(url = uri.value)
        }

        fun resource(name: String) = Resource(url = "$url$name")

    }

}