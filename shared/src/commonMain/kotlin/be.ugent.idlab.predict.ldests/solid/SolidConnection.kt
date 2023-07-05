package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.RemoteResource
import be.ugent.idlab.predict.ldests.rdf.TripleBuilder
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.util.SubmitRequestType
import be.ugent.idlab.predict.ldests.util.submit

class SolidConnection(
    url: String
) {

    val root = Folder(if (url.last() != '/') "$url/" else url)

    open class Resource(
        val url: String
    ) {

        fun read(): RemoteResource {
            return RemoteResource.from(url = url)
        }

        open suspend fun write(block: TripleBuilder.() -> Unit): Int {
            // `PUT`ting the resource directly
            return submit(
                type = SubmitRequestType.PUT,
                url = url,
                headers = listOf("Content-type" to "text/turtle"),
                body = Turtle(prefixes = Ontology.PREFIXES, block = block)
            )
        }

        open suspend fun write(turtle: String): Int {
            // `PUT`ting the resource directly
            return submit(
                type = SubmitRequestType.PUT,
                url = url,
                headers = listOf("Content-type" to "text/turtle"),
                body = turtle
            )
        }

    }

    class Folder(
        // always has a trailing '/'!
        url: String
    ): Resource(url) {

        override suspend fun write(block: TripleBuilder.() -> Unit): Int {
            // creating the folder
            submit(
                type = SubmitRequestType.PUT,
                url = url,
                headers = listOf(),
                body = ""
            )
            // setting the .meta file for the additional data
            return submit(
                type = SubmitRequestType.PATCH,
                url = "$url.meta",
                headers = listOf("Content-type" to "application/sparql-update"),
                // no prefixes used here, as the `INSERT DATA {}` construct doesn't like the `@prefix` syntax
                body = "INSERT DATA { ${Turtle(block = block)} }"
            )
        }

        override suspend fun write(turtle: String): Int {
            // creating the folder
            submit(
                type = SubmitRequestType.PUT,
                url = url,
                headers = listOf(),
                body = ""
            )
            // setting the .meta file for the additional data
            return submit(
                type = SubmitRequestType.PATCH,
                url = "$url.meta",
                headers = listOf("Content-type" to "application/sparql-update"),
                // no prefixes used here, as the `INSERT DATA {}` construct doesn't like the `@prefix` syntax
                body = "INSERT DATA { $turtle }"
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

    /**
     * Returns a usable resource (either regular or folder) depending on the passed URL
     */
    fun fromUrl(url: String): Resource {
        require(url.startsWith(this.root.url))
        // depending on the url (trailing / or not), either a folder or resource is constructed
        // TODO: check with remote on existence (throwing) & type (instead of simply looking at the trailing char)
        return if (url.endsWith('/')) {
            Folder(url = url)
        } else {
            Resource(url = url)
        }
    }

}