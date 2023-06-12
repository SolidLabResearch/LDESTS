package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.lib.rdf.N3StreamParser
import be.ugent.idlab.predict.ldests.lib.rdf.N3Triple

/**
 * Maps an input stream of the text representation of triples to actual triples
 */
actual fun InputStream<String>.mapToTriples(): InputStream<N3Triple> {
    val parser =  N3StreamParser()
    pipe(parser)
    return parser
}
