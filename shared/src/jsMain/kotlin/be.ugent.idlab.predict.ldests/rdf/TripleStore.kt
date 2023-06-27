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

}
