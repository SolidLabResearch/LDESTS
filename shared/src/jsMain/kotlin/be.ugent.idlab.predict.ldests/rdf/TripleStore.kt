package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.rdf.ontology.RDF


actual class TripleStore(val store: N3Store) {

    actual val size
        get() = store.size

    actual val subjects
        get() = store.getSubjects()

    actual val predicates
        get() = store.getPredicates()

    actual val objects
        get() = store.getObjects()

    actual constructor(): this(store = N3Store())

    actual fun add(triple: Triple) = store.add(triple)

    actual fun has(triple: Triple) = store.has(triple)

    actual fun delete(triple: Triple) = store.delete(triple)

    actual fun insert(context: RDFBuilder.Context, block: RDFBuilder.() -> Unit) {
        RDFBuilder(context) { subject, predicate, `object`: Any ->
            // this should be correct with the right usage
            store.add(
                subject = subject,
                predicate = predicate,
                `object` = `object`.processed()
            )
        }.apply(block)
    }

    actual companion object;

    private fun Any.processed(): Term = when (this) {
        is RDFBuilder.Blank -> { processed() }
        is RDFBuilder.List -> { processed() }
        else /* Term hopefully */ -> {
            /* no processing needed */
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            this as Term
        }
    }

    private fun RDFBuilder.Blank.processed(): Term {
        // creating a blank node to represent this term
        val blank = store.createBlankNode()
        // pairing all data members to this term as a subject
        data.forEach { (predicate, `object`) ->
            store.add(
                subject = blank,
                predicate = predicate,
                `object` = `object`.processed()
            )
        }
        return blank
    }

    private fun RDFBuilder.List.processed(): Term {
        val subj = store.createBlankNode()
        store.add(
            subject = subj,
            predicate = RDF.first,
            `object` = data.first().processed()
        )
        store.add(
            subject = subj,
            predicate = RDF.rest,
            `object` = if (data.size > 1) {
                RDFBuilder.List(data.subList(1, data.size)).processed()
            } else {
                RDF.nil
            }
        )
        return subj
    }

}
