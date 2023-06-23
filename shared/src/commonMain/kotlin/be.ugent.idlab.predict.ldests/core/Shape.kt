package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class Shape private constructor(
    private val typeIdentifier: ClassProperty,
    private val sampleIdentifier: IdentifierProperty,
    private val properties: List<Property>
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
        protected val predicate: NamedNodeTerm,
        /* Identifier used in the sparql query, `null` if it is not bound outside of the query */
        val identifier: String?
    ) {

        /** Whether or not the property is compatible with the provided data **/
        abstract fun satisfies(data: Term): Boolean

        abstract fun asQuery(subject: String = "s"): String

    }

    class ConstantProperty(
        predicate: NamedNodeTerm,
        /* NamedNodeTerm, BlankNodeTerm and LiteralTerm possible, but Literal's shouldn't be used */
        val value: List<Term>,
        private val id: Int
    ): Property(
        predicate = predicate,
        identifier = if (value.size == 1) { /* Not bound externally */ null } else { "c${id}" }
    ) {

        // see `asQuery()` to see the use-case
        private val filter = if (value.size == 1) { "" } else {
            var result = "BIND("
            value.forEachIndexed { index, term ->
                val statement = "?cv${id} = ${term.value}, $index"
                result = "${result}IF($statement, "
            }
            result + "-1${")".repeat(value.size)} AS ?$identifier)"
        }

        override fun satisfies(data: Term): Boolean {
            return data in value
        }

        override fun asQuery(subject: String): String {
            return if (value.size == 1) {
                // simple "assertion" statement only
                "?$subject ${predicate.value} ${value.first().value} ."
            } else {
                /**
                 * The resulting query looks like
                 * ```
                 * ?subject <predicate> ?cv{id} .
                 * BIND(
                 *      IF(?cv{id} = value[0], 0, IF(?cv{id} = value[1], 1, ..., -1)) AS ?c{id}
                 * )
                 * ```
                 */
                return "?$subject ${predicate.value} ?cv${id} .\n$filter ."
            }
        }

    }

    class VariableProperty(
        predicate: NamedNodeTerm,
        val type: NamedNodeTerm,
        val count: Int,
        id: Int
    ): Property(
        predicate = predicate,
        identifier = "v${id}"
    ) {
        override fun satisfies(data: Term): Boolean {
            return data.type == "LiteralTerm" && (data as LiteralTerm).datatype == type
        }

        override fun asQuery(subject: String): String {
            return "?$subject ${predicate.value} ?$identifier ."
        }

    }

    companion object {

        fun create(
            type: NamedNodeTerm,
            build: BuildScope.() -> Shape
        ): Shape = BuildScope(typeIdentifier = ClassProperty(value = type)).build()

    }

    class BuildScope(private val typeIdentifier: ClassProperty) {

        private val properties = mutableListOf<Property>()
        private val prefixes = mutableMapOf<String, String>()

        fun apply(constraints: Iterable<Property>) {
            properties.addAll(constraints)
        }

        fun prefix(short: String, full: String) {
            prefixes[short] = full
        }

        fun variable(path: String, type: String, count: Int = 1) {
            properties.add(
                element = VariableProperty(
                    predicate = path.parsed(),
                    type = type.parsed(),
                    count = count,
                    id = properties.size
                )
            )
        }

        fun constant(path: String, vararg value: String) {
            properties.add(
                element = ConstantProperty(
                    predicate = path.parsed(),
                    value = value.map { it.parsed() },
                    id = properties.size
                )
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
        val select = "SELECT ?id " + properties.filter { it.identifier != null }.joinToString(" ") { "?${it.identifier}" } + " WHERE"
        val body = "?s a ${typeIdentifier.value.value} .\n?s ${sampleIdentifier.predicate.value} ?id .\n" + properties.joinToString("\n") { it.asQuery() }
        val query = "$select {\n$body\n}"
        Query(sparql = query)
//        return Query("""
//            PREFIX saref: <https://saref.etsi.org/core/>
//            PREFIX rdf: <http://rdfs.org/ns/void#>
//            PREFIX dahcc_acc: <https://dahcc.idlab.ugent.be/Ontology/SensorsAndWearables/SmartphoneAcceleration/>
//
//            SELECT ?id ?value ?index WHERE {
//                ?sample saref:hasTimestamp ?id ;
//                    saref:relatesToProperty ?prop ;
//                    saref:hasValue ?value .
//                BIND(
//                    IF(?prop = dahcc_acc:x, 0, IF(?prop = dahcc_acc:y, 1, IF(?prop = dahcc_acc:z, 2, -1))) AS ?index
//                )
//            }"""
//        )
    }

    fun format(result: Binding): String {
        return "${result.id()}:" + properties.filter { it.identifier != null }.joinToString(";") { result[it.identifier!!]!!.value }
    }

    private fun Binding.id() = LocalDateTime.parse(get("id")!!.value).toEpochMilli()

    private fun LocalDateTime.toEpochMilli(): Long {
        return this.toInstant(TimeZone.of("UTC")).toEpochMilliseconds()
    }

    // TODO: getters for constrained constants, unconstrained constants, ...


    /**
     * Narrows the broader shape (`this`) with the constraints found in `delta` (using own ontology!)
     */
    // TODO: constraintsbuilder instead ?
    internal fun applyConstraints(constraints: Iterable<Property>): Shape {
        // TODO: logic described above
        return this
    }
}
