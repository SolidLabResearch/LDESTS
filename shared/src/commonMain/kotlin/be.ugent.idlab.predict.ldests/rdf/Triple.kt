package be.ugent.idlab.predict.ldests.rdf

expect class Triple {
    val subject: Term
    val predicate: Term
    val `object`: Term
}

expect interface Term {
    val value: String
}

expect class TripleStore() {

    val size: Int

    val subjects:   Array<Term>
    val predicates: Array<Term>
    val objects:    Array<Term>

    fun add(triple: Triple)
    fun has(triple: Triple)
    fun delete(triple: Triple)
    fun forEach(subject: Term? = null, predicate: Term? = null, `object`: Term? = null, action: (Triple) -> Unit)

}
