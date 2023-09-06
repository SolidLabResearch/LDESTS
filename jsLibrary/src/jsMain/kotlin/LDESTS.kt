
import be.ugent.idlab.predict.ldests.core.*
import be.ugent.idlab.predict.ldests.lib.rdf.ComunicaBinding
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.Query
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import be.ugent.idlab.predict.ldests.solid.SolidPublisher
import be.ugent.idlab.predict.ldests.util.keys
import be.ugent.idlab.predict.ldests.util.warn
import kotlin.collections.set
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

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun insertFile(filename: String) = promise { parent.append(filename = filename) }

    @ExternalUse
    fun insertTriple(data: N3Triple) {
        parent.insert(data)
    }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun flush() = promise { parent.flush() }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun close() = promise { parent.close() }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun query(
        publisher: PublisherConfig,
        query: String,
        callback: (ComunicaBinding) -> Unit
    ) = promise {
        val pub = publisher.toPublisher() ?: return@promise
        parent.query(pub, Query(query), callback)
    }

    /** Helper methods **/

    // dynamic constraints: `predicate`: ["value1", "value2", ...]
    private fun parseConstraints(constraints: dynamic): Map<NamedNodeTerm, Iterable<NamedNodeTerm>> {
        val c = mutableMapOf<NamedNodeTerm, Iterable<NamedNodeTerm>>()
        keys(constraints).forEach { predicate ->
            c[predicate.asNamedNode()] = (constraints[predicate]!! as Any).let { constraint ->
                if (constraint is String) {
                    listOf(constraint.asNamedNode())
                } else if (constraint is Array<*>) {
                    // hopefully only containing strings
                    @Suppress("UNCHECKED_CAST")
                    (constraint as Array<String>).map { it.asNamedNode() }
                } else {
                    // named node term hopefully
                    listOf(constraint as NamedNodeTerm)
                }
            }
        }
        return c
    }

    @JsName("Builder")
    @ExternalUse
    class BuilderJS(name: String) {

        private val builder = LDESTS.Builder(name = name)

        @ExternalUse
        fun config(config: StreamConfig): BuilderJS {
            builder.config(
                Stream.Configuration(
                    window = (config.window as? Int)?.minutes ?: Stream.Configuration.DEFAULT_WINDOW_MIN.minutes,
                    resourceSize = config.resourceSize as? Int ?: Stream.Configuration.DEFAULT_RESOURCE_SIZE
                )
            )
            return this
        }

        @ExternalUse
        fun shape(shape: ShapeJS): BuilderJS {
            builder.shape(shape.shape)
            return this
        }

        @ExternalUse
        fun split(uri: String): BuilderJS {
            builder.queryRule(uri.asNamedNode())
            return this
        }

        @ExternalUse
        fun attach(config: PublisherConfig): BuilderJS {
            val publisher = config.toPublisher() ?: return this
            builder.attach(publisher)
            return this
        }

        @ExternalUse
        fun build() = promise { LDESTSJS(builder.build()) }

    }

}

@JsExport
@ExternalUse
external interface StreamConfig {
    /* The window for a single fragment, in minutes */
    val window: Int
    /* The max amount of samples in a single fragment */
    val resourceSize: Int
}

@JsExport
@ExternalUse
external interface PublisherConfig {
    val type: PublisherType
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
fun PublisherConfig.toPublisher(): Publisher? {
    return when (type) {
        PublisherType.Solid -> (this as? SolidPublisherConfig)?.url?.let { SolidPublisher(url = it) }
        PublisherType.Memory -> MemoryPublisher()
        PublisherType.Debug -> DebugPublisher()
    } ?: run {
        warn("PublisherConfig", "Wrong arguments supplied for publisher creation (type: ${type.name})")
        null
    }
}

@JsExport
@ExternalUse
enum class PublisherType {
    Solid, Memory, Debug
}

@JsExport
@ExternalUse
external interface SolidPublisherConfig: PublisherConfig {
    val url: String
}
