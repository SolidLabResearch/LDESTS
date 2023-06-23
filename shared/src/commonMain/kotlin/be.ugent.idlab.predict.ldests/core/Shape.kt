package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.Property.Companion.query
import be.ugent.idlab.predict.ldests.rdf.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class Shape private constructor(
    private val typeIdentifier: ClassProperty,
    private val sampleIdentifier: IdentifierProperty,
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
    )

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
                return "?$subject ${first.value} ${second.query} ."
            }

        }

    }

    class ConstantProperty(
        /* NamedNodeTerm, BlankNodeTerm and LiteralTerm possible, but Literal's shouldn't be used */
        private val value: List<Term>,
        private val id: Int
    ): Property(
        identifier = if (value.size == 1) { /* Not bound externally */ null } else { "c${id}" }
    ) {

        override val query: String = run {
            if (value.size == 1) {
                // simple "assertion" statement suffices
                value.first().value
            } else {
                var filter = "BIND("
                value.forEachIndexed { index, term ->
                    val statement = "?cv${id} = ${term.value}, $index"
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
                "?cv${id} .\n$filter -1${")".repeat(value.size)} AS ?$identifier)"
            }
        }

        override fun covers(property: Property): Boolean {
            return property is ConstantProperty &&
                    property.value.all { prop -> value.any { it.value == prop.value } }
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

    }

    class BuildScope(private val typeIdentifier: ClassProperty) {

        private val properties = mutableMapOf<NamedNodeTerm, Property>()
        private val prefixes = mutableMapOf<String, String>()

        fun apply(constraints: Map<NamedNodeTerm, Property>) {
            properties.putAll(constraints)
        }

        fun prefix(short: String, full: String) {
            prefixes[short] = full
        }

        fun variable(path: String, type: String, count: Int = 1) {
            properties[path.parsed()] = VariableProperty(
                type = type.parsed(),
                count = count,
                id = properties.size
            )
        }

        fun constant(path: String, vararg value: String) {
            properties[path.parsed()] = ConstantProperty(
                value = value.map { it.parsed() },
                id = properties.size
            )
        }

        fun identifier(path: String, type: String): Shape {
            return Shape(
                typeIdentifier = typeIdentifier,
                sampleIdentifier = IdentifierProperty(
                    predicate = path.parsed(),
                    type = type.parsed()
                ),
                properties = properties
            )
        }

        private fun String.parsed(): NamedNodeTerm {
            return if (startsWith('<') && endsWith('>')) {
                this.asNamedNode()
            } else {
                val prefix = this.substring(0, indexOfAny(charArrayOf(':', '#')))
                ('<' + prefixes[prefix]!! + substring(prefix.length + 1) + '>').asNamedNode()
            }
        }

    }

    internal object Ontology {

        // TODO: various members from the ontology (constants, tree path, variables)

    }


    /**
     * A generated query that can be used to extract all relevant fields from a collection of triples
     */

    val query = run {
        // TODO: the resulting query can be improved upon by setting the subject `?s` up front and using `;`
        val select = "SELECT ?id " + properties.values.mapNotNull { it.identifier }.joinToString(" ") { "?$it" } + " WHERE"
        val body = "?s a ${typeIdentifier.value.value} .\n?s ${sampleIdentifier.predicate.value} ?id .\n" + properties.asIterable().joinToString("\n") { (it.key to it.value).query() }
        val query = "$select {\n$body\n}"
        Query(
            sparql = query,
            variables = setOf("id") + properties.values.mapNotNull { it.identifier }
        )
    }

    fun format(result: Binding): String {
        return "${result.id()}:" + properties.values.filter { it.identifier != null }.joinToString(";") { result[it.identifier!!]!!.value }
    }

    private fun Binding.id() = LocalDateTime.parse(get("id")!!.value).toEpochMilli()

    private fun LocalDateTime.toEpochMilli(): Long {
        return this.toInstant(TimeZone.of("UTC")).toEpochMilliseconds()
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
