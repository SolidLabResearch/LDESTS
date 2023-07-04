package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Stream.Rules.Companion.constraint
import be.ugent.idlab.predict.ldests.rdf.NamedNodeTerm
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.rdf.asLiteral
import be.ugent.idlab.predict.ldests.rdf.asNamedNode
import be.ugent.idlab.predict.ldests.rdf.ontology.*
import be.ugent.idlab.predict.ldests.rdf.ontology.LDESTS

// TODO: change the publisher argument to publisher context receiver once K2 releases
internal fun Turtle.stream(publisher: Publisher, stream: Stream) = with(publisher) {
    // creating the shape first, in the same document for now (and thus 'local' uri)
    val shape = "shape".asNamedNode()
    shape(subject = shape, shape = stream.shape)
    // associating the shape from above with the stream here
    +stream.uri has RDF.type being TREE.Node
    +stream.uri has RDF.type being LDESTS.StreamType
    +stream.uri has LDESTS.shape being shape
}

internal fun Turtle.fragmentRelation(publisher: Publisher, fragment: Stream.Fragment) = with(publisher) {
    blank {
        +RDF.type being TREE.GreaterThanOrEqualToRelation // FIXME other relations? custom relation?
        +TREE.value being fragment.start
        +TREE.path being fragment.shape.sampleIdentifier.predicate
        +TREE.node being fragment.uri
    }
}

internal fun Turtle.fragment(publisher: Publisher, fragment: Stream.Fragment) = with (publisher) {
    +fragment.uri has RDF.type being LDESTS.FragmentType
    fragment.rules.constraints.forEach {
        +fragment.uri has LDESTS.constraints being constraint(it)
    }
}

internal fun Turtle.resource(publisher: Publisher, resource: Stream.Fragment.Resource) = with(publisher) {
    +resource.uri has RDF.type being LDESTS.ResourceType
    +resource.uri has LDESTS.contents being resource.data.asLiteral()
}

fun Turtle.shape(subject: NamedNodeTerm, shape: Shape) {
    +subject has RDF.type being SHACL.Shape
    +subject has RDF.type being SHAPETS.Type
    +subject has SHACL.targetClass being shape.typeIdentifier.value
    +subject has SHACL.property being property(shape.sampleIdentifier)
    shape.properties.forEach {
        +subject has SHACL.property being property(it)
    }
}

internal fun Turtle.property(property: Shape.IdentifierProperty) = blank {
    +RDF.type being SHACL.Property
    +RDF.type being SHAPETS.Identifier
    +SHACL.path being property.predicate
    +SHACL.dataType being SHACL.Literal
    +SHACL.minCount being 1
    +SHACL.maxCount being 1
}

internal fun Turtle.property(
    property: Map.Entry<NamedNodeTerm, Shape.Property>
) = when (val prop = property.value) {
    is Shape.ConstantProperty -> property(property.key to prop)
    is Shape.VariableProperty -> property(property.key to prop)
}

internal fun Turtle.property(property: Pair<NamedNodeTerm, Shape.ConstantProperty>) = blank {
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

internal fun Turtle.property(property: Pair<NamedNodeTerm, Shape.VariableProperty>) = blank {
    +RDF.type being SHACL.Property
    +RDF.type being SHAPETS.Variable
    +SHACL.path being property.first
    // FIXME: enforced lower bound
    +SHACL.minCount being 1
    +SHACL.maxCount being property.second.count
    +SHACL.nodeKind being SHACL.Literal
    +SHAPETS.startIndex being 0 // FIXME configure this in the property, set when adding the property to the shape
    +SHAPETS.endIndex being 0 + property.second.count // FIXME: see note above
}
