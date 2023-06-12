/**
 * A JS-exportable wrapper for the SolidConnection class from the shared codebase
 */

//@JsExport
//@JsName("SolidConnection")
//@ExternalUse
//class SolidConnectionJS private constructor(
//    private val con: SolidConnection
//) {
//
//    @ExternalUse
//    val root = Directory(con.root)
//
//    @ExternalUse
//    fun close() = promise {
//        con.close()
//    }
//
//    @ExternalUse
//    class Directory internal constructor(
//        private val parent: SolidConnection.Directory
//    ) {
//
//        @ExternalUse
//        fun data() = promise { parent.data.await().arr() }
//
//        @ExternalUse
//        fun files() = promise { parent.files().map { File(it) } }
//
//        @ExternalUse
//        fun directories() = promise { parent.directories().map { Directory(it) } }
//
//        /** Invalidates the contents, causing a new fetch for the next query **/
//        @ExternalUse
//        fun invalidate() = parent.invalidate()
//
//    }
//
//    @ExternalUse
//    class File internal constructor(
//        private val parent: SolidConnection.File
//    ) {
//
//        @ExternalUse
//        fun data() = promise { parent.data.await().arr() }
//
//        @ExternalUse
//        fun content() = promise { parent.content().arr() }
//
//        /** Invalidates the contents, causing a new fetch for the next query **/
//        @ExternalUse
//        fun invalidate() = parent.invalidate()
//
//    }
//
//    @ExternalUse
//    class Builder {
//
//        private lateinit var url: String
//        private val builder = SolidConnection.Builder()
//
//        @ExternalUse
//        fun url(url: String): Builder {
//            this.url = url
//            return this
//        }
//
//        @ExternalUse
//        fun root(directory: String): Builder {
//            builder.rootDir = directory
//            return this
//        }
//
//        @ExternalUse
//        fun authorisation(credentials: String): Builder {
//            builder.authorisation = credentials
//            return this
//        }
//
//        @ExternalUse
//        fun create(): SolidConnectionJS {
//            return SolidConnectionJS(con = SolidConnection(url, builder))
//        }
//
//    }
//
//}
