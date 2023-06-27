
import be.ugent.idlab.predict.ldests.core.Shape
import be.ugent.idlab.predict.ldests.rdf.asNamedNode

@JsExport
@JsName("Shape")
@ExternalUse
class ShapeJS private constructor(
    internal val shape: Shape
) {

    val query: String
        get() = shape.query.sparql

    @ExternalUse
    class Builder(
        type: String,
        private val identifier: String,
        private val identifierType: String = "<http://www.w3.org/2001/XMLSchema#dateTime>"
    ) {

        internal val builder = Shape.BuildScope(
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

}