package be.ugent.idlab.predict.ldests.rdf

expect class TripleStore() {

    val size: Int

    val subjects:   Array<Term>
    val predicates: Array<Term>
    val objects:    Array<Term>

    fun add(triple: Triple)
    fun has(triple: Triple)
    fun delete(triple: Triple)

}
