package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.*
import be.ugent.idlab.predict.ldests.rdf.ontology.*
import be.ugent.idlab.predict.ldests.rdf.ontology.LDESTS
import be.ugent.idlab.predict.ldests.util.*

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
    constraintSet(subject = constraints, rules = stream.rules)
    +stream.uri has LDESTS.constraintSet being constraints
}

internal fun RDFBuilder.constraintSet(subject: NamedNodeTerm, rules: List<Stream.Rules>) {
    +subject has RDF.type being LDESTS.ConstraintSet
    rules.forEach { rule ->
        val uri = rule.id.asNamedNode()
        +subject has LDESTS.constraintValue being uri
        +uri has RDF.type being LDESTS.Constraint
        +uri has LDESTS.constraintId being rule.id.asNamedNode()
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
SELECT ?ruleId ?ruleArg ?ruleVal WHERE {
    <${stream.value}> a <https://predict.ugent.be/ldests#Node> .
    <${stream.value}> <https://predict.ugent.be/ldests#constraintSet> ?set .

    ?set a <https://predict.ugent.be/ldests#ConstraintSet> .
    ?set <https://predict.ugent.be/ldests#constraintValue> ?rule .
    ?rule a <https://predict.ugent.be/ldests#Constraint> .
    ?rule <https://predict.ugent.be/ldests#constraintId> ?ruleId .
    ?rule <https://predict.ugent.be/ldests#constraints> ?constraints .
    ?constraints <http://www.w3.org/ns/shacl#path> ?ruleArg .
    ?constraints <https://predict.ugent.be/shape#value> ?ruleVal .
}""")

internal suspend fun InputStream<Binding>.consumeAsRuleData(): Map<String, Map<NamedNodeTerm, List<Term>>> {
    // asserting non-null everywhere, callers job to use it on the correct binding stream
    val result = mutableMapOf<String, MutableMap<NamedNodeTerm, MutableList<Term>>>()
    consume { binding ->
        result
            .getOrPut(binding["ruleId"]!!.value) { mutableMapOf() }
            .getOrPut(binding["ruleArg"]!! as NamedNodeTerm) { mutableListOf() }
            .add(binding["ruleVal"]!!)
    }.join()
    return result
}

internal fun RDFBuilder.fragmentRelation(fragment: Stream.Fragment) = blank {
    +RDF.type being TREE.GreaterThanOrEqualToRelation // FIXME other relations? custom relation?
    +TREE.value being fragment.start
    +TREE.path being fragment.shape.sampleIdentifier.predicate
    +TREE.node being fragment.uri
    // should exist here as well due to the stream definition
    +LDESTS.constraints being fragment.rules.id.asNamedNode()
}

internal fun RDFBuilder.fragment(fragment: Stream.Fragment) {
    +fragment.uri has RDF.type being LDESTS.FragmentType
}

internal fun RDFBuilder.resource(resource: Stream.Fragment.Resource) {
    +resource.uri has RDF.type being LDESTS.ResourceType
    +resource.uri has LDESTS.contents being resource.data.asLiteral()
}

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
    +SHAPETS.startIndex being 0 // FIXME configure this in the property, set when adding the property to the shape
    +SHAPETS.endIndex being 0 + property.second.count // FIXME: see note above
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

internal suspend fun InputStream<Binding>.consumeAsShapeInformation(): Shape? {
    var type: NamedNodeTerm? = null
    var identifierPath: String? = null
    var identifierType: String? = null
    val vars = mutableMapOf<NamedNodeTerm, Shape.VariableProperty>()
    // as constant properties typically consists out of multiple binding (once every fixed `value`), they
    //  need to be represented in a intermediate way first
    val consts = mutableMapOf<NamedNodeTerm, MutableList<Term>>()
    try {
        consume { binding ->
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
        }.join()
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
