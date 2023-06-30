package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.IdentifierProperty.Companion.BINDING_IDENTIFIER
import be.ugent.idlab.predict.ldests.core.Shape.Property.Companion.query
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.RDF
import be.ugent.idlab.predict.ldests.rdf.ontology.SHACL
import be.ugent.idlab.predict.ldests.rdf.ontology.SHAPETS
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class Shape private constructor(
    internal val typeIdentifier: ClassProperty,
    internal val sampleIdentifier: IdentifierProperty,
    val properties: Map<NamedNodeTerm, Property>
) {

    /** The type/class property, not part of the regular property hierarchy due to its unique nature **/
    class ClassProperty(
        val value: NamedNodeTerm
    )

    /** The timestamp / tree property, not part of the regular property hierarchy due to its unique nature **/
    class IdentifierProperty(
        val predicate: NamedNodeTerm,
        val type: NamedNodeTerm /* Matches the Literal's `datatype` attr */
    ) {

        companion object {

            internal const val BINDING_IDENTIFIER = "id"

        }

    }

    /** Other properties, can be defined >= 0 times **/
    sealed class Property(
        /* Identifier used in the sparql query, `null` if it is not bound outside of the query */
        val identifier: String?
    ) {

        protected abstract val query: String

        /** Whether or not the property validly covers the provided one (as a validity test of the provided property) **/
        abstract fun covers(property: Property): Boolean

        companion object {

            fun Pair<NamedNodeTerm, Property>.query(subject: String = "s"): String {
                return "?$subject <${first.value}> ${second.query} ."
            }

        }

    }

    class ConstantProperty: Property {

        /* NamedNodeTerm, BlankNodeTerm and LiteralTerm possible, but Literal's shouldn't be used */
        internal val values: List<Term>
        private val id: Int?
        override val query: String

        constructor(values: List<Term>, id: Int): super(identifier = "c${id}") {
            require(values.size > 1)
            this.values = values
            this.id = id
            // creating the query
            var filter = "BIND("
            this.values.forEachIndexed { index, term ->
                val statement = "?cv${id} = <${term.value}>, $index"
                filter = "${filter}IF($statement, "
            }
            /**
             * The resulting query looks like ([] already included)
             * ```
             * [?subject <predicate>] ?cv{id} .
             * BIND(
             *      IF(?cv{id} = value[0], 0, IF(?cv{id} = value[1], 1, ..., -1)) AS ?c{id}
             * )
             * ```
             */
            this.query = "?cv${id} .\n$filter -1${")".repeat(this.values.size)} AS ?$identifier)"
        }

        constructor(value: Term): super(identifier = /* Not bound externally */ null) {
            this.values = listOf(value)
            this.id = null
            // creating the query
            // simple "assertion" statement suffices, so keeping only the actual term value
            this.query = "<${value.value}>"
        }

        override fun covers(property: Property): Boolean {
            return property is ConstantProperty &&
                    property.values.all { prop -> values.any { it.value == prop.value } }
        }

    }

    class VariableProperty(
        val type: NamedNodeTerm,
        val count: Int,
        id: Int
    ): Property(
        identifier = "v${id}"
    ) {

        override val query = "?$identifier"

        override fun covers(property: Property): Boolean {
            return property is VariableProperty &&
                    property.type.type == "LiteralTerm" &&
                    (property.type as LiteralTerm).datatype == type
        }

    }

    companion object {

        fun create(
            type: NamedNodeTerm,
            build: BuildScope.() -> Shape
        ): Shape = BuildScope(typeIdentifier = ClassProperty(value = type)).build()

        fun Turtle.shape(subject: NamedNodeTerm, shape: Shape) {
            +subject has RDF.type being list(SHACL.Shape, SHAPETS.Type)
            +subject has SHACL.targetClass being shape.typeIdentifier.value
            +subject has SHACL.property being list(
                items = shape.properties.map { prop -> property(prop) } + property(shape.sampleIdentifier)
            )
        }

        private fun Turtle.property(property: IdentifierProperty) = blank {
            +RDF.type being list(SHACL.Property, SHAPETS.Identifier)
            +SHACL.path being property.predicate
            +SHACL.dataType being SHACL.Literal
            +SHACL.minCount being 1
            +SHACL.maxCount being 1
        }

        internal fun Turtle.property(
            property: Map.Entry<NamedNodeTerm, Property>
        ) = when (val prop = property.value) {
            is ConstantProperty -> property(property.key to prop)
            is VariableProperty -> property(property.key to prop)
        }

        private fun Turtle.property(property: Pair<NamedNodeTerm, ConstantProperty>) = blank {
            +RDF.type being list(SHACL.Property, SHAPETS.Constant)
            +SHACL.path being property.first
            +SHACL.minCount being 1
            +SHACL.maxCount being 1
            // FIXME: support for non-IRI constants?
            +SHACL.nodeKind being SHACL.IRI
            // FIXME: what to do with a single value? same or different path?
            +SHAPETS.constantValues being list(property.second.values)
        }

        private fun Turtle.property(property: Pair<NamedNodeTerm, VariableProperty>) = blank {
            +RDF.type being list(SHACL.Property, SHAPETS.Variable)
            +SHACL.path being property.first
            // FIXME: enforced lower bound
            +SHACL.minCount being 1
            +SHACL.maxCount being property.second.count
            +SHACL.nodeKind being SHACL.Literal
            +SHAPETS.startIndex being 0 // FIXME configure this in the property, set when adding the property to the shape
            +SHAPETS.endIndex being 0 + property.second.count // FIXME: see note above
        }

        fun Binding.id() = LocalDateTime.parse(get(BINDING_IDENTIFIER)!!.value).toEpochMilli()

        private fun LocalDateTime.toEpochMilli(): Long {
            return this.toInstant(TimeZone.of("UTC")).toEpochMilliseconds()
        }

    }

    class BuildScope(private val typeIdentifier: ClassProperty) {

        private val properties = mutableMapOf<NamedNodeTerm, Property>()

        fun apply(constraints: Map<NamedNodeTerm, Property>) {
            properties.putAll(constraints)
        }

        fun variable(path: String, type: String, count: Int = 1) {
            properties[path.asNamedNode()] = VariableProperty(
                type = type.asNamedNode(),
                count = count,
                id = properties.size
            )
        }

        fun constant(path: String, vararg value: String) {
            require(value.isNotEmpty())
            if (value.size == 1) {
                properties[path.asNamedNode()] = ConstantProperty(
                    value = value.first().asNamedNode()
                )
            } else {
                properties[path.asNamedNode()] = ConstantProperty(
                    values = value.map { it.asNamedNode() },
                    id = properties.size
                )
            }
        }

        fun identifier(path: String, type: String): Shape {
            return Shape(
                typeIdentifier = typeIdentifier,
                sampleIdentifier = IdentifierProperty(
                    predicate = path.asNamedNode(),
                    type = type.asNamedNode()
                ),
                properties = properties
            )
        }

    }

    /**
     * A generated query that can be used to extract all relevant fields from a collection of triples
     */

    val query = run {
        // TODO: the resulting query can be improved upon by setting the subject `?s` up front and using `;`
        val select = "SELECT ?$BINDING_IDENTIFIER " + properties.values.mapNotNull { it.identifier }.joinToString(" ") { "?$it" } + " WHERE"
        val body = "?s a <${typeIdentifier.value.value}> .\n?s <${sampleIdentifier.predicate.value}> ?id .\n" + properties.asIterable().joinToString("\n") { (it.key to it.value).query() }
        val query = "$select {\n$body\n}"
        Query(
            sparql = query,
            variables = setOf("id") + properties.values.mapNotNull { it.identifier }
        )
    }

    fun format(result: Binding): String {
        return "${result.id()}:" + properties.values.filter { it.identifier != null }.joinToString(";") { result[it.identifier!!]!!.value }
    }

    // TODO: getters for constrained constants, unconstrained constants, ...


    /**
     * Narrows the broader shape (`this`) with the provided constraints
     */
    fun narrow(constraints: Map<NamedNodeTerm, Property>): Shape {
        check(constraints.all { properties[it.key]?.covers(it.value) == true })
        return Shape(
            typeIdentifier = typeIdentifier,
            sampleIdentifier = sampleIdentifier,
            properties = properties + constraints
        )
    }

}
