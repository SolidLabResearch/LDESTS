package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.rdf.TripleProvider

abstract class Publisher {

    /**
     * The context of the publisher, providing e.g. the base URI of this publisher (ex. `localhost:3000`)
     */
    abstract val context: RDFBuilder.Context

    /**
     * Fetches data related to the provided path (depending on the type of publisher)
     */
    abstract suspend fun fetch(path: String): TripleProvider?

    /**
     * Publishes the data retrieved by executing the provided lambda.
     * Expected path is the complete path after host, like `stream/` or `stream/fragment`
     */
    abstract fun publish(path: String, data: RDFBuilder.() -> Unit)

    /**
     * Publishes all data currently in cache, if any and if possible
     */
    open suspend fun flush() {
        /* nothing to do by default */
    }

    /**
     * The buffers this stream is currently subscribed to
     */
    private val bufs = mutableSetOf<PublishBuffer>()

    /**
     * Starts publishing everything the buffer receives. Keeps listening until `unsubscribe` with
     *  the same buffer is called or the program terminates
     */
    suspend fun subscribe(buffer: PublishBuffer) = with(buffer) {
        if (attach()) {
            bufs.add(this)
        }
    }

    /**
     * Starts publishing everything created by the publishable item. Keeps listening until `unsubscribe` with
     *  the same publisher is called
     */
    fun unsubscribe(buffer: PublishBuffer) = with(buffer) {
        detach()
        bufs.remove(this)
    }

    open fun close() {
        val it = bufs.iterator()
        while (it.hasNext()) { unsubscribe(it.next()) }
    }

}
