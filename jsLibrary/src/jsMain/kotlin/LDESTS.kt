
import be.ugent.idlab.predict.ldests.core.LDESTS

/**
 * A JS-exportable wrapper for the SolidConnection class from the shared codebase
 */

@JsExport
@JsName("LDESTS")
@ExternalUse
class LDESTSJS private constructor(
    private val parent: LDESTS
) {

    @ExternalUse
    fun append(filename: String) = parent.append(filename = filename)

    @ExternalUse
    fun flush() = promise { parent.flush() }

    @ExternalUse
    fun close() = promise { parent.close() }

    @JsName("Builder")
    @ExternalUse
    class BuilderJS(url: String) {

        private val builder = LDESTS.Builder(url)

        @ExternalUse
        fun shape(shape: ShapeJS): BuilderJS {
            builder.shape(shape.shape)
            return this
        }

        @ExternalUse
        fun file(filepath: String): BuilderJS {
            builder.file(filepath)
            return this
        }

        @ExternalUse
        fun stream(ldesUrl: String): BuilderJS {
            builder.stream(ldesUrl)
            TODO()
        }

        @ExternalUse
        fun remote(url: String): BuilderJS {
            builder.remote(url)
            return this
        }

        // TODO: provide proper shape builder, exposing properties for target class and
        //  either a set of constraints, or the ability to automatically infer the rest

        @ExternalUse
        fun create() = promise { LDESTSJS(builder.create()) }

    }

}
