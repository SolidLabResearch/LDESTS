package be.ugent.idlab.predict.ldests.solid

import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.rdf.RemoteResource
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.ontology.Ontology
import be.ugent.idlab.predict.ldests.util.SubmitRequestType
import be.ugent.idlab.predict.ldests.util.submit

class SolidConnection(
    url: String
) {

    val context = RDFBuilder.Context(
        path = url
    )

    val root = Folder(if (url.last() != '/') "$url/" else url)

    // all to-be-published requests
    private val requests = mutableListOf<suspend () -> Unit>()

    open inner class Resource(
        val url: String
    ) {

        fun read(): RemoteResource {
            return RemoteResource.from(url = url)
        }

        open fun write(block: RDFBuilder.() -> Unit) {
            requests.add {
                // TODO: check for success
                submit(
                    // `PUT`ting the resource directly
                    type = SubmitRequestType.PUT,
                    url = url,
                    headers = listOf("Content-type" to "text/turtle"),
                    body = Turtle(
                        context = context,
                        prefixes = Ontology.PREFIXES,
                        block = block
                    )
                )
            }
        }

    }

    inner class Folder(
        // always has a trailing '/'!
        url: String
    ): Resource(url) {

        override fun write(block: RDFBuilder.() -> Unit) {
            requests.add {
                // TODO: check for success
                // creating the folder
                submit(
                    type = SubmitRequestType.PUT,
                    url = url,
                    headers = listOf(),
                    body = ""
                )
                // setting the .meta file for the additional data
                submit(
                    type = SubmitRequestType.PATCH,
                    url = "$url.meta",
                    headers = listOf("Content-type" to "application/sparql-update"),
                    // no prefixes used here, as the `INSERT DATA {}` construct doesn't like the `@prefix` syntax
                    body = "INSERT DATA { ${Turtle(context = context, block = block)} }"
                )
            }
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

    suspend fun flush() {
        val it = requests.iterator()
        while (it.hasNext()) {
            it.next().invoke()
            it.remove()
        }
    }

}