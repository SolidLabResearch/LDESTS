package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Shape.IdentifierProperty.Companion.BINDING_IDENTIFIER
import be.ugent.idlab.predict.ldests.core.Shape.Property.Companion.query
import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.RDF
import be.ugent.idlab.predict.ldests.util.log
import be.ugent.idlab.predict.ldests.util.stringified
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class Shape private constructor(
    val typeIdentifier: ClassProperty,
    val sampleIdentifier: IdentifierProperty,
    val properties: Map<NamedNodeTerm, Property>
) {

    // LUTs for parsing the data: non-encoded and encoded values
    // the cast is correct here as only `ConstantProperty` can have `indices` initialised as `null`
    @Suppress("UNCHECKED_CAST")
    private val constants = properties.toList().filter { it.second.index == null } as List<Pair<NamedNodeTerm, ConstantProperty>>
    private val variables = properties.toList().filter { it.second.index != null }.sortedBy { it.second.index!! }
    // slots used by constants for lookup for this specific shape
    private val slots: Map<NamedNodeTerm, Int> = run {
        val map = mutableMapOf<NamedNodeTerm, Int>()
        // starting with an offset of 1, as this is where the identifier property is put
        var offset = 1
        // variables are already sorted
        variables.forEach { (predicate, property) ->
            val count = (property as? VariableProperty)?.count ?: 1
            if (property is ConstantProperty) {
                map[predicate] = offset
            }
            offset += count
        }
        map
    }

    override fun equals(other: Any?): Boolean {
        if (other !is Shape) { return false }
        if (typeIdentifier != other.typeIdentifier || sampleIdentifier != other.sampleIdentifier) {
            log("Base configuration mismatch: ${typeIdentifier.stringified()} != ${other.typeIdentifier.stringified()} | ${sampleIdentifier.stringified()} != ${other.sampleIdentifier.stringified()}")
            return false
        }
        if (properties.size != other.properties.size || properties.keys.any { it !in other.properties.keys }) {
            log("Available properties mismatch")
            return false
        }
        properties.forEach { (predicate, firstValue) ->
            val secondValue = other.properties[predicate]!!
            if (firstValue != secondValue) {
                log("Existing properties mismatch: `${predicate.value}`: ``${firstValue} - `${secondValue}`")
                return false
            }
        }
        return true
    }

    /** The type/class property, not part of the regular property hierarchy due to its unique nature **/
    data class ClassProperty(
        val value: NamedNodeTerm
    )

    /** The timestamp / tree property, not part of the regular property hierarchy due to its unique nature **/
    data class IdentifierProperty(
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
        val identifier: String?,
        val index: Int?
    ) {

        protected abstract val query: String

        /** Whether or not the property validly covers the provided one (as a validity test of the provided property) **/
        abstract fun covers(property: Property): Boolean

        companion object {

            fun Pair<NamedNodeTerm, Property>.query(subject: String = "s"): String {
                return "?$subject <${first.value}> ${second.query} ."
            }

        }

        abstract fun encode(data: Term): String

        abstract fun decode(data: List<String>): List<Term>

    }

    // TODO: split the two constant cases up
    // TODO: make the non-binding a different property type
    class ConstantProperty: Property {
        /**
         * TODO: migrate back to a query which looks like ([] already included)
         * ```
         * [?subject <predicate>] ?cv{id} .
         * BIND(
         *      IF(?cv{id} = value[0], 0, IF(?cv{id} = value[1], 1, ..., -1)) AS ?c{id}
         * )
         * ```
         */

        /* NamedNodeTerm, BlankNodeTerm and LiteralTerm possible, but Literal's shouldn't be used */
        internal val values: List<Term>
        private val id: Int?
        override val query: String

        constructor(
            values: List<Term>,
            id: Int
        ): super(
            identifier = "c${id}",
            index = id
        ) {
            require(values.size > 1)
            this.values = values
            this.id = id
            this.query = "?c${id}"
        }

        constructor(value: Term): super(
            identifier = /* Not bound externally */ null,
            index = null // takes up no space
        ) {
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

        override fun equals(other: Any?): Boolean {
            val o = other as? ConstantProperty ?: return false
            // order of values is relevant, so strict checking of lists
            return values == o.values
        }

        override fun hashCode(): Int {
            var result = values.hashCode()
            result = 31 * result + (id ?: 0)
            return result
        }

        override fun encode(data: Term): String {
            return values.indexOf(data).toString()
        }

        override fun decode(data: List<String>): List<Term> {
            require(values.size > 1 && data.size == 1)
            return listOf(values[data.first().toInt()])
        }

    }

    class VariableProperty(
        val type: NamedNodeTerm,
        val count: Int,
        id: Int
    ): Property(
        identifier = "v${id}",
        index = id
    ) {

        override val query = "?$identifier"

        override fun covers(property: Property): Boolean {
            return property is VariableProperty &&
                    property.type == type
        }

        override fun equals(other: Any?): Boolean {
            val o = other as? VariableProperty ?: return false
            return type == o.type && count == o.count
        }

        override fun hashCode(): Int {
            var result = type.hashCode()
            result = 31 * result + count
            return result
        }

        override fun encode(data: Term): String {
            // TODO: support for other datatypes, such as dates, and process the value as such
            return data.value
        }

        override fun decode(data: List<String>): List<Term> {
            // TODO: use the type to know what exact conversion needs to take place
            return data.map { it.toFloat().asLiteral() }
        }

    }

    companion object {

        operator fun invoke(
            type: NamedNodeTerm,
            build: BuildScope.() -> Shape
        ): Shape = BuildScope(typeIdentifier = ClassProperty(value = type)).build()

        fun Binding.id(): Long {
            val value = get(BINDING_IDENTIFIER)!!.value
            return try {
                // checking for either timezone aware strings...
                Instant.parse(value).toEpochMilliseconds()
            } catch (e: Throwable) {
                // or otherwise falling back to local date time strings
                LocalDateTime.parse(value).toEpochMilli()
            }
        }

        private fun LocalDateTime.toEpochMilli(): Long {
            return this.toInstant(TimeZone.of("UTC")).toEpochMilliseconds()
        }

        fun Map<NamedNodeTerm, ConstantProperty>.stringified() = asIterable().joinToString { (predicate, value) ->
            "${predicate.value} - ${value.values.joinToString(", ") { it.value }}"
        }

    }

    class BuildScope(private val typeIdentifier: ClassProperty) {

        private val properties = mutableMapOf<NamedNodeTerm, Property>()
        // current index used for creating unique identifiers and indices for the resulting format
        // not the same as `properties.size`, as this count does NOT increment from constant properties not
        //  taking up indices in the resulting format
        private var variableCount = 0

        fun apply(constraints: Map<NamedNodeTerm, Property>) {
            // TODO: change this method to not trake in direct properties, so they can be configured here w/ correct ID
            //  instead
            properties.putAll(constraints)
        }

        fun variable(path: String, type: String, count: Int = 1) {
            properties[path.asNamedNode()] = VariableProperty(
                type = type.asNamedNode(),
                count = count,
                id = variableCount
            )
            variableCount += count
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
                    id = variableCount++
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

    fun encode(result: Binding): String {
        return "${result.id()}:" + variables.joinToString(";") { it.second.encode(result[it.second.identifier!!]!!) } + ';'
    }

    fun decode(
        publisher: Publisher,
        range: LongRange,
        constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>,
        source: Iterable<Char>
    ) = flow {
        // extracting the values
        val iter = source.iterator()
        // reading in chunks of max 256, assuming no data is longer than 256 byte for now
        val buf = CharArray(256)
        var i = 0
        // used to form the subject with
        var j = 0
        // the various extracted fields, first one should be the text representation of the identifier
        val data = mutableListOf<String>()
        // callback used when data is found
        suspend fun process() {
            // checking the id to see if there's any publishing requested
            if (data.isEmpty() || data.first().toLong() !in range) return
            // checking all constraints first to see if they match up prior to generating any triples
            val relevant = constraints.all { (predicate, values) ->
                // finding the slot index; if `null`, the query constraint is not part of the rule set variables, and is
                //  thus already matched when querying over fragments
                val index = slots[predicate] ?: return@all true
                val decoded = (properties[predicate]!! as ConstantProperty).decode(data.subList(index, index + 1)).first()
                decoded in values
            }
            if (!relevant) return
            // processing the data & sending it through
            data.decode(publisher.context, j++).forEach { emit(it) }
        }
        while (iter.hasNext()) {
            when (val c = iter.next()) {
                ':' -> {
                    // current buffer content denotes the identifier property, so using the buffer as source for
                    //  new triples, and inserting the current buf for the new entry
                    process()
                    // resetting the data, and setting the identifier of the next entry
                    data.clear()
                    data.add(buf.concatToString(endIndex = i))
                    i = 0
                }
                ';' -> {
                    // current buffer denotes a data entry, inserting it as such
                    data.add(buf.concatToString(endIndex = i))
                    i = 0
                }
                else -> {
                    // simply appending the data
                    buf[i++] = c
                }
            }
        }
        // emitting the last set of triples
        process()
    }

    // helper for the method above
    /**
     * Explained by the example below, for a shape consisting out of Long time identifier, one constant value
     *  and one variable value. `this` should contain ["time", "index", "value"]. The returned triples is the type
     *  triple, the identifier triple, all constant properties (exact 1 constant value from the rule set) and lastly
     *  the variable triples from the resource itself
     */
    private fun List<String>.decode(
        context: RDFBuilder.Context,
        id: Int
    ) = buildTriples(context) {
        val subject = with (context) { "Sample_$id".absolutePath }
        +subject has RDF.type being typeIdentifier.value
        +subject has sampleIdentifier.predicate being Instant.fromEpochMilliseconds(first().toLong()).asLiteral()
        constants.forEach { (predicate, constant) ->
            // there can only be one as seen by the construction of this collection
            +subject has predicate being constant.values.first()
        }
        // starting from 1, as the identifier took the very first index
        var index = 1
        variables.forEach { (predicate, property) ->
            val count = (property as? VariableProperty)?.count ?: 1
            property.decode(subList(index, index + count))
                .forEach { value ->
                    +subject has predicate being value
                }
            index += count
        }
    }

    /**
     * Validates compatibility of this shape with the provided constraints
     */
    fun complies(constraints: Map<NamedNodeTerm, Property>): Boolean {
        if (constraints.keys.any { it !in properties }) {
            return false
        }
        return constraints.all { (property, value) -> properties[property]!!.covers(value) }
    }

    /**
     * Validates compatibility of this shape with the provided constants
     */
    fun complies(constraints: Map<NamedNodeTerm, Iterable<NamedNodeTerm>>): Boolean {
        if (constraints.keys.any { it !in properties }) {
            return false
        }
        return constraints.all { (property, value) ->
            (properties[property] as? ConstantProperty)
                ?.values
                ?.let { values ->
                    value.all { value -> value in values }
                } ?: false
        }
    }

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
