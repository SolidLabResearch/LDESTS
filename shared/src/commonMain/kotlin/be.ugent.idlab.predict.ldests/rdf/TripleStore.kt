package be.ugent.idlab.predict.ldests.rdf

expect class TripleStore() {

    val size: Int

    val subjects:   Array<Term>
    val predicates: Array<Term>
    val objects:    Array<Term>

    fun add(triple: Triple)
    fun add(subject: Term, predicate: NamedNodeTerm, `object`: Term)
    fun has(triple: Triple)
    fun delete(triple: Triple)

    fun insert(context: RDFBuilder.Context, block: RDFBuilder.() -> Unit)

    companion object

}

operator fun TripleStore.Companion.invoke(path: String = "", block: RDFBuilder.() -> Unit) =
    TripleStore().apply { insert(context = RDFBuilder.Context(path = path), block) }

