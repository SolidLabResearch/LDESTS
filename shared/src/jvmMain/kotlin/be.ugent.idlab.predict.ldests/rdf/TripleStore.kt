package be.ugent.idlab.predict.ldests.rdf


actual class TripleStore actual constructor() {

    actual val size: Int
        get() = TODO("Not yet implemented")
    actual val subjects: Array<Term>
        get() = TODO("Not yet implemented")
    actual val predicates: Array<Term>
        get() = TODO("Not yet implemented")
    actual val objects: Array<Term>
        get() = TODO("Not yet implemented")

    actual fun add(triple: Triple) {
        TODO()
    }

    actual fun add(triples: Collection<Triple>) {
        TODO()
    }

    actual fun add(triples: Array<Triple>) {
        TODO()
    }

    actual fun add(
        subject: Term,
        predicate: NamedNodeTerm,
        `object`: Term
    ) {
        TODO()
    }

    actual fun has(triple: Triple) {
        TODO()
    }

    actual fun delete(triple: Triple) {
        TODO()
    }

    actual fun insert(
        context: RDFBuilder.Context,
        block: RDFBuilder.() -> Unit
    ) {
        TODO()
    }

    actual fun insert(context: RDFBuilder.Context, turtle: String) {
        TODO()
    }

    actual fun asIterable(): Iterable<Triple> {
        TODO("Not yet implemented")
    }

    actual companion object

}
