
import be.ugent.idlab.predict.ldests.core.LDESTS
import be.ugent.idlab.predict.ldests.core.MemoryPublisher
import be.ugent.idlab.predict.ldests.core.Stream
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import kotlin.time.Duration.Companion.minutes

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

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun flush() = promise { parent.flush() }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun close() = promise { parent.close() }

    @ExternalUse
    fun toStore() = promise {
        // flushing first, so the store is usable
        parent.flush()
        // looking for a memory based publisher
        (parent.publishers.find { it is MemoryPublisher } as? MemoryPublisher)
            ?.buffer?.store
    }

    @JsName("Builder")
    @ExternalUse
    class BuilderJS(name: String) {

        private val builder = LDESTS.Builder(name = name)

        @ExternalUse
        fun config(config: dynamic): BuilderJS {
            builder.config(Stream.Configuration(
                window = (config.window as? Int)?.minutes ?: Stream.Configuration.DEFAULT_WINDOW_MIN.minutes,
                resourceSize = config.resourceSize as? Int ?: Stream.Configuration.DEFAULT_RESOURCE_SIZE,
                resourceCount = config.resourceCount as? Int ?: Stream.Configuration.DEFAULT_RESOURCE_COUNT,
            ))
            return this
        }

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

        @ExternalUse
        fun queryUri(uri: String): BuilderJS {
            builder.queryRule(uri.asNamedNode())
            return this
        }

        @ExternalUse
        fun attachDebugPublisher(): BuilderJS {
            builder.attachDebugPublisher()
            return this
        }

        @ExternalUse
        fun attachMemoryPublisher(): BuilderJS {
            builder.attachMemoryPublisher()
            return this
        }

        @ExternalUse
        fun attachSolidPublisher(url: String): BuilderJS {
            builder.attachSolidPublisher(url = url)
            return this
        }

        @ExternalUse
        fun create() = promise { LDESTSJS(builder.create()) }

    }

}
