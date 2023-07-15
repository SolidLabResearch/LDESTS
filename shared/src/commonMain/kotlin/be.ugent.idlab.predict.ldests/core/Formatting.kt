package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.*
import be.ugent.idlab.predict.ldests.rdf.ontology.LDESTS
import be.ugent.idlab.predict.ldests.util.error
import be.ugent.idlab.predict.ldests.util.warn

internal fun RDFBuilder.stream(stream: Stream) {
    // creating the shape first, in the same document for now (and thus 'local' uri)
    val shape = "Shape".asNamedNode()
    shape(subject = shape, shape = stream.shape)
    // associating the shape from above with the stream here
    +stream.uri has RDF.type being TREE.Node
    +stream.uri has RDF.type being LDESTS.StreamType
    +stream.uri has LDESTS.shape being shape
    // associating the various rule groups to the different fragments by rule id
    val constraints = "ConstraintSet".asNamedNode()
    constraintSet(subject = constraints, rules = stream.rules.values)
    +stream.uri has LDESTS.constraintSet being constraints
}

internal fun RDFBuilder.constraintSet(subject: NamedNodeTerm, rules: Iterable<Stream.Rules>) {
    +subject has RDF.type being LDESTS.ConstraintSet
    rules.forEach { rule ->
        val uri = rule.name.asNamedNode()
        +subject has LDESTS.constraintValue being uri
        +uri has RDF.type being LDESTS.Constraint
        rule.constraints.forEach {
            +uri has LDESTS.constraints being constraint(it)
        }
    }
}

internal fun RDFBuilder.constraint(constraint: Map.Entry<NamedNodeTerm, Shape.ConstantProperty>) = blank {
    +SHACL.path being constraint.key
    if (constraint.value.values.size == 1) {
        +SHAPETS.constantValue being constraint.value.values.first()
    } else {
        +SHAPETS.constantValues being list(constraint.value.values)
    }
}

// the associated sparql query
internal fun CONSTRAINT_SPARQL_QUERY_FOR_STREAM(stream: NamedNodeTerm) = Query("""
SELECT ?rule ?ruleArg ?ruleVal WHERE {
    <${stream.value}> a <${LDESTS.StreamType.value}> .
    <${stream.value}> <${LDESTS.constraintSet.value}> ?set .

    ?set a <${LDESTS.ConstraintSet.value}> .
    ?set <${LDESTS.constraintValue.value}> ?rule .
    ?rule a <${LDESTS.Constraint.value}> .
    ?rule <${LDESTS.constraints.value}> ?constraints .
    ?constraints <${SHACL.path.value}> ?ruleArg .
    ?constraints <${SHAPETS.constantValue.value}> ?ruleVal .
}""")

internal suspend fun TripleProvider.retrieveRuleData(
    stream: NamedNodeTerm
): Map<String, Map<NamedNodeTerm, List<Term>>> {
    val result = mutableMapOf<String, MutableMap<NamedNodeTerm, MutableList<Term>>>()
    query(CONSTRAINT_SPARQL_QUERY_FOR_STREAM(stream)) { binding ->
        result
            .getOrPut(binding["rule"]!!.value) { mutableMapOf() }
            .getOrPut(binding["ruleArg"]!! as NamedNodeTerm) { mutableListOf() }
            .add(binding["ruleVal"]!!)
    }
    return result
}

internal fun RDFBuilder.fragmentRelation(fragment: Stream.Fragment) = blank {
    +RDF.type being TREE.GreaterThanOrEqualToRelation // FIXME other relations? custom relation?
    +TREE.value being fragment.properties.start
    +TREE.path being fragment.shape.sampleIdentifier.predicate
    +TREE.node being fragment.uri
    // should exist here as well due to the stream definition
    +LDESTS.constraints being fragment.properties.rules.uri
}

internal fun FRAGMENT_RELATION_SPARQL_QUERY(stream: NamedNodeTerm) = Query("""
SELECT ?constraints ?name ?start WHERE {
    <${stream.value}> a <${LDESTS.StreamType.value}> .
    <${stream.value}> <${TREE.relation.value}> ?fragment .
    ?fragment
        <${LDESTS.constraints.value}> ?constraints ;
        <${TREE.node.value}> ?name ;
        <${TREE.value.value}> ?start .
}
""")

internal fun RDFBuilder.fragment(fragment: Stream.Fragment) {
    +fragment.uri has RDF.type being LDESTS.FragmentType
    +fragment.uri has LDESTS.contents being fragment.data.asLiteral()
}

internal fun FRAGMENT_CONTENTS_SPARQL_QUERY(fragment: NamedNodeTerm) = Query("""
SELECT ?data WHERE {
    <${fragment.value}>
        a <${LDESTS.FragmentType.value}> ;
        <${LDESTS.contents.value}> ?data
}
""")

fun RDFBuilder.shape(subject: NamedNodeTerm, shape: Shape) {
    +subject has RDF.type being SHACL.Shape
    +subject has RDF.type being SHAPETS.Type
    +subject has SHACL.targetClass being shape.typeIdentifier.value
    +subject has SHACL.property being property(shape.sampleIdentifier)
    shape.properties.forEach {
        +subject has SHACL.property being property(it)
    }
}

internal fun RDFBuilder.property(property: Shape.IdentifierProperty) = blank {
    +RDF.type being SHACL.Property
    +RDF.type being SHAPETS.Identifier
    +SHACL.path being property.predicate
    +SHACL.nodeKind being SHACL.Literal
    +SHACL.dataType being property.type
    +SHACL.minCount being 1
    +SHACL.maxCount being 1
}

internal fun RDFBuilder.property(
    property: Map.Entry<NamedNodeTerm, Shape.Property>
) = when (val prop = property.value) {
    is Shape.ConstantProperty -> property(property.key to prop)
    is Shape.VariableProperty -> property(property.key to prop)
}

internal fun RDFBuilder.property(property: Pair<NamedNodeTerm, Shape.ConstantProperty>) = blank {
    +RDF.type being SHACL.Property
    +RDF.type being SHAPETS.Constant
    +SHACL.path being property.first
    +SHACL.minCount being 1
    +SHACL.maxCount being 1
    property.second.index?.let {
        +LDESTS.order being it
    }
    // FIXME: support for non-IRI constants?
    +SHACL.nodeKind being SHACL.IRI
    // FIXME: what to do with a single value? same or different path?
    +SHAPETS.constantValues being list(property.second.values)
}

internal fun RDFBuilder.property(property: Pair<NamedNodeTerm, Shape.VariableProperty>) = blank {
    +RDF.type being SHACL.Property
    +RDF.type being SHAPETS.Variable
    +SHACL.path being property.first
    // FIXME: enforced lower bound
    +SHACL.minCount being 1
    +SHACL.maxCount being property.second.count
    +SHACL.nodeKind being SHACL.Literal
    +SHACL.dataType being property.second.type
    +LDESTS.order being property.second.index!!
}

// the associated sparql query
internal fun SHAPE_SPARQL_QUERY_FOR_STREAM(stream: NamedNodeTerm) = Query("""
PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>

SELECT ?shapeTarget ?propType ?propPath ?propKind ?propDType ?propDefValue ?propValues ?propStart ?propEnd WHERE {
    <${stream.value}> a <https://predict.ugent.be/ldests#Node> .
    <${stream.value}> <https://predict.ugent.be/ldests#shape> ?shape .
    ?shape a <https://predict.ugent.be/shape#BaseShape> .
    ?shape <http://www.w3.org/ns/shacl#property> ?shapeProperty .
    ?shape <http://www.w3.org/ns/shacl#targetClass> ?shapeTarget .

    ?shapeProperty a ?propType .
    ?shapeProperty <http://www.w3.org/ns/shacl#path> ?propPath .
    OPTIONAL {
        ?shapeProperty <http://www.w3.org/ns/shacl#nodeKind> ?propKind .
    }
    OPTIONAL {
        ?shapeProperty <https://predict.ugent.be/shape#value> ?propDefValue .
    }
    OPTIONAL {
        ?shapeProperty <https://predict.ugent.be/shape#values>/rdf:rest*/rdf:first ?propValues .
    }
    OPTIONAL {
        ?shapeProperty <http://www.w3.org/ns/shacl#datatype> ?propDType .
    }
    OPTIONAL {
        ?shapeProperty <https://predict.ugent.be/shape#startIndex> ?propStart .
        ?shapeProperty <https://predict.ugent.be/shape#endIndex> ?propEnd .
    }
}
""")

internal suspend fun TripleProvider.retrieveShapeData(stream: NamedNodeTerm): Shape? {
    var type: NamedNodeTerm? = null
    var identifierPath: String? = null
    var identifierType: String? = null
    val vars = mutableMapOf<NamedNodeTerm, Shape.VariableProperty>()
    // as constant properties typically consists out of multiple binding (once every fixed `value`), they
    //  need to be represented in a intermediate way first
    val consts = mutableMapOf<NamedNodeTerm, MutableList<Term>>()
    try {
        query(SHAPE_SPARQL_QUERY_FOR_STREAM(stream)) { binding ->
            binding["shapeTarget"]?.let { type = it as NamedNodeTerm }
            when (binding["propType"]!!.value) {
                SHAPETS.Identifier.value -> {
                    identifierPath = binding["propPath"]!!.value
                    identifierType = binding["propDType"]!!.value
                }
                SHAPETS.Variable.value -> {
                    vars[binding["propPath"]!! as NamedNodeTerm] = Shape.VariableProperty(
                        type = binding["propDType"]!! as NamedNodeTerm,
                        count = 1 /* FIXME */,
                        id = vars.size + consts.size /* FIXME rework this ID stuff... */
                    )
                }
                SHAPETS.Constant.value -> {
                    consts
                        .getOrPut(binding["propPath"]!! as NamedNodeTerm) { mutableListOf() }
                        .add(binding["propValues"]!!)
                    // TODO: propKind?
                }
                else -> {
                    warn("Unrecognized shape property type found: `${binding["propType"]!!.value}`")
                }
            }
        }
    } catch (e: Throwable) {
        error("Something went wrong while parsing shape information: ${e.message}")
        return null
    }
    return if (type == null || identifierPath == null || identifierType == null) {
        null
    } else {
        Shape(type!!) {
            apply(vars)
            var temp = 0
            apply(
                consts.mapValues {
                    if (it.value.size == 1) Shape.ConstantProperty(value = it.value.first())
                    else Shape.ConstantProperty(values = it.value, id = ++temp) }
            )
            identifier(identifierPath!!, identifierType!!)
        }
    }
}
