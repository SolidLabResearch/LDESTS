
import be.ugent.idlab.predict.ldests.core.LDESTS
import be.ugent.idlab.predict.ldests.core.Stream
import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import be.ugent.idlab.predict.ldests.solid.SolidPublisher
import be.ugent.idlab.predict.ldests.util.keys
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
    fun append(filename: String) = promise { parent.append(filename = filename) }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun flush() = promise { parent.flush() }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun close() = promise { parent.close() }

    @ExternalUse
    fun queryAsStore(
        url: String,
        constraints: dynamic,
        start: Double = .0,
        end: Double = Double.MAX_VALUE,
    ) = promise {
        val store = N3Store()
        parent.query(
            publisher = SolidPublisher(url),
            constraints = parseConstraints(constraints),
            range = start.toLong() until end.toLong()
        ).collect { store.add(it) }
        store
    }

    @Suppress("NON_EXPORTABLE_TYPE") // wrong in this case
    @ExternalUse
    fun query(
        url: String,
        callback: (N3Triple) -> Unit,
        constraints: dynamic,
        start: Double = 0.0,
        end: Double = Double.MAX_VALUE,
    ) = promise {
        parent.query(
            publisher = SolidPublisher(url),
            constraints = parseConstraints(constraints),
            range = start.toLong() until end.toLong()
        ).collect(callback)
    }

    @ExternalUse
    fun insert(data: N3Triple) {
        parent.insert(data)
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
                    @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
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
        fun config(config: dynamic): BuilderJS {
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
