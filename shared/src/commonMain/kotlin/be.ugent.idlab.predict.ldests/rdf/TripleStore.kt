package be.ugent.idlab.predict.ldests.rdf

expect class TripleStore() {

    val size: Int

    val subjects:   Array<Term>
    val predicates: Array<Term>
    val objects:    Array<Term>

    fun add(triple: Triple)
    fun add(triples: Collection<Triple>)
    fun add(triples: Array<Triple>)
    fun add(subject: Term, predicate: NamedNodeTerm, `object`: Term)
    fun has(triple: Triple)
    fun delete(triple: Triple)

    fun insert(context: RDFBuilder.Context, block: RDFBuilder.() -> Unit)

    fun insert(context: RDFBuilder.Context, turtle: String)

    fun asIterable(): Iterable<Triple>

    companion object

}

operator fun TripleStore.Companion.invoke(path: String = "", block: RDFBuilder.() -> Unit) =
    TripleStore().apply { insert(context = RDFBuilder.Context(path = path), block) }

fun Array<Triple>.toStore() = TripleStore().apply { add(this@toStore) }

fun Collection<Triple>.toStore() = TripleStore().apply { add(this@toStore) }

fun Iterable<Triple>.toStore() = this@toStore.toList().toStore()
