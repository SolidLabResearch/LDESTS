package be.ugent.idlab.predict.ldests.rdf

import be.ugent.idlab.predict.ldests.lib.rdf.N3Store
import be.ugent.idlab.predict.ldests.lib.rdf.N3Term
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple

actual typealias Triple = N3Triple
actual typealias Term = N3Term

actual class TripleStore(private val buf: N3Store) {

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

    actual fun forEach(
        subject: Term?,
        predicate: Term?,
        `object`: Term?,
        action: (Triple) -> Unit
    ) = buf.forEach(
        callback = action,
        subject = subject,
        predicate = predicate,
        `object` = `object`
    )

}
