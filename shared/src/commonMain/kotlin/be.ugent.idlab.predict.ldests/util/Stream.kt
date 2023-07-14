package be.ugent.idlab.predict.ldests.util

import be.ugent.idlab.predict.ldests.rdf.Triple

expect interface Stream

expect open class InputStream<T>: Stream

/**
 * Creates a single-entry stream of the provided value, useful if "slow" data insertions are happening
 */
expect fun Triple.streamify(): InputStream<Triple>

/**
 * Creates a multi-entry stream of the provided values, useful if "segmented" data insertions are happening
 */
expect fun Iterable<Triple>.streamify(): InputStream<Triple>

/**
 * Suspends until the stream finished
 */
expect suspend fun Stream.join()

expect fun fromFile(filepath: String): InputStream<String>

expect fun InputStream<String>.mapToTriples(): InputStream<Triple>

expect inline fun <T> InputStream<T>.consume(crossinline block: (T) -> Unit): Stream
