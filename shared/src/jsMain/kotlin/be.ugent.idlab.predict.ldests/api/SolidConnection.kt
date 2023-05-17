package be.ugent.idlab.predict.ldests.api

import be.ugent.idlab.predict.ldests.solid.SolidConnection

/**
 * A JS-exportable wrapper for the SolidConnection class from the shared codebase
 */

@ExperimentalJsExport
@JsExport
@JsName("SolidConnection")
class SolidConnectionJS internal constructor(
    private val con: SolidConnection
) {

    @JsName("root")
    val root = Directory(con.root)

    @JsName("close")
    fun close() = promise {
        con.close()
    }

    @JsName("Directory")
    class Directory internal constructor(
        private val parent: SolidConnection.Directory
    ) {
        fun files() = promise { parent.files().map { File(it) } }

        fun directories() = promise { parent.directories().map { Directory(it) } }

        /** Invalidates the contents, causing a new fetch for the next query **/
        fun invalidate() = parent.invalidate()

    }

    @JsName("File")
    class File internal constructor(
        private val parent: SolidConnection.File
    ) {
        fun content() = promise { parent.content().map { Triple(s = it.s, p = it.p, o = it.o) } }

        /** Invalidates the contents, causing a new fetch for the next query **/
        fun invalidate() = parent.invalidate()

    }

    class Builder {

        private lateinit var url: String
        private val builder = SolidConnection.Builder()

        fun url(url: String): Builder {
            this.url = url
            return this
        }

        fun root(directory: String): Builder {
            builder.rootDir = directory
            return this
        }

        fun authorisation(credentials: String): Builder {
            builder.authorisation = credentials
            return this
        }

        fun create(): SolidConnectionJS {
            return SolidConnectionJS(con = SolidConnection(url, builder))
        }

//        fun create(
//            url: String,
//            options: dynamic = undefined
//        ): SolidConnectionJS {
//            val builder = SolidConnection.Builder()
//            // checks to see if options is defined and non-null
//            if (options) {
//                (options.rootDir as? String)?.let { builder.rootDir = it }
//                (options.authorisation as? String)?.let { builder.authorisation = it }
//            }
//            return SolidConnectionJS(con = SolidConnection(url, builder))
//        }

    }

}

//@OptIn(ExperimentalJsExport::class)
//@JsExport
//@JsName("create")
//fun create(
//    url: String,
//    options: dynamic = undefined
//): SolidConnectionJS {
//    val builder = SolidConnection.Builder()
//    // checks to see if options is defined and non-null
//    if (options) {
//        (options.rootDir as? String)?.let { builder.rootDir = it }
//        (options.authorisation as? String)?.let { builder.authorisation = it }
//    }
//    return SolidConnectionJS(con = SolidConnection(url, builder))
//}
