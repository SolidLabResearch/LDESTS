
import be.ugent.idlab.predict.ldests.core.Shape
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import be.ugent.idlab.predict.ldests.util.keys

@JsExport
@JsName("Shape")
@ExternalUse
class ShapeJS private constructor(
    internal val shape: Shape
) {

    val query: String
        get() = shape.query.sparql

    val sampleType: NamedNodeTerm
        get() = shape.typeIdentifier.value

    val sampleIdentifier: NamedNodeTerm
        get() = shape.sampleIdentifier.predicate

    @ExternalUse
    class Builder(
        type: String,
        private val identifier: String,
        private val identifierType: String = "http://www.w3.org/2001/XMLSchema#dateTime"
    ) {

        private val builder = Shape.BuildScope(
            typeIdentifier = Shape.ClassProperty(type.asNamedNode())
        )

        @ExternalUse
        fun constant(path: String, vararg values: String): Builder {
            builder.constant(path = path, value = values)
            return this
        }

        @ExternalUse
        fun variable(path: String, type: String, count: Int = 1): Builder {
            builder.variable(path = path, type = type, count = count)
            return this
        }

        @ExternalUse
        fun build(): ShapeJS {
            return ShapeJS(
                shape = builder.identifier(identifier, identifierType)
            )
        }

    }

    companion object {

        @ExternalUse
        fun parse(description: ShapeDescription): ShapeJS {
            val builder = Builder(
                type = description.type,
                identifier = description.identifier,
                identifierType = description.identifierType ?: "http://www.w3.org/2001/XMLSchema#dateTime"
            )
            keys(description.constants).forEach { path ->
                builder.constant(path = path, values = description.constants[path] as Array<String>)
            }
            keys(description.variables).forEach { path ->
                builder.variable(path = path, type = description.variables[path] as String)
            }
            return builder.build()
        }

    }

}

@JsExport
@ExternalUse
external interface ShapeDescription {
    val type: String
    val identifier: String
    val identifierType: String?
        get() = definedExternally

    // {"path1": ["value1_1", "value1_2", ... ], ... }
    val constants: dynamic
    // {"path1": "type1", ... }
    val variables: dynamic
}
