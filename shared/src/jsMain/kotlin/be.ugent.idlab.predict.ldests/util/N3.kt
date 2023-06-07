package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.lib.node.WritableNodeStream
import be.ugent.idlab.predict.ldests.lib.rdf.N3StreamParser
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple

fun createStreamParser(
    target: WritableNodeStream<N3Triple?>
): N3StreamParser {
    val stream = N3StreamParser()
    stream.pipe(to = target)
    return stream
}
