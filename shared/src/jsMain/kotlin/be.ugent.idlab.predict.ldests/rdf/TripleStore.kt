package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Store


actual class TripleStore(val buf: N3Store) {

    actual val size
        get() = buf.size

    actual val subjects
        get() = buf.getSubjects()

    actual val predicates
        get() = buf.getPredicates()

    actual val objects
        get() = buf.getObjects()

    actual constructor(): this(buf = N3Store())

    actual fun add(triple: Triple) = buf.add(triple)

    actual fun has(triple: Triple) = buf.has(triple)

    actual fun delete(triple: Triple) = buf.delete(triple)

    actual fun insert(block: TripleBuilder.() -> Unit) {
        TripleBuilder { subject, predicate, `object`: Any ->
            // this should be correct with the right usage
            buf.add(
                subject = subject,
                predicate = predicate,
                `object` = `object`.processed()
            )
        }.apply(block)
    }

    actual companion object;

    private fun Any.processed(): Term = when (this) {
        is TripleBuilder.Blank -> { processed() }
        is TripleBuilder.List -> { processed() }
        else /* Term hopefully */ -> {
            /* no processing needed */
            @Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
            this as Term
        }
    }

    private fun TripleBuilder.Blank.processed(): Term {
        // creating a blank node to represent this term
        val blank = buf.createBlankNode()
        // pairing all data members to this term as a subject
        data.forEach { (predicate, `object`) ->
            buf.add(
                subject = blank,
                predicate = predicate,
                `object` = `object`.processed()
            )
        }
        return blank
    }

    private fun TripleBuilder.List.processed(): Term {
        // TODO have to do this manually according to the RDF list spec (start & rest w/ blank nodes)
        return "NOT YET IMPLEMENTED".asLiteral()
    }

}
