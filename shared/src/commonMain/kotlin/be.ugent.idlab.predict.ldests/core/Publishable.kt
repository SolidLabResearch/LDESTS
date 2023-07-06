package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.util.warn

abstract class Publishable(
    // own, relative, path after the host (e.g. `stream/`, `stream/fragment/resource`
    override val name: String
): RDFBuilder.Element {

    internal var buffer: PublishBuffer? = null
        private set

    enum class PublisherAttachResult {
        SUCCESS, FAILURE, NEW
    }

    internal open suspend fun onPublisherAttached(publisher: Publisher): PublisherAttachResult {
        // no compat checking by default
        return PublisherAttachResult.SUCCESS
    }

    internal suspend fun onCreate(publisher: Publisher) {
        publisher.publish(name) { onCreate(publisher) }
    }

    protected open fun RDFBuilder.onCreate(publisher: Publisher) {
        /* nothing to do by default */
    }

    protected suspend fun publish(block: RDFBuilder.() -> Unit) {
        buffer
            ?.emit(path = name, data = block)
            ?: run {
                warn("A publishable type experienced spillage (no buffer attached)!")
            }
    }

    fun attach(buffer: PublishBuffer) {
        this.buffer = buffer
        buffer.onAttached(this)
    }

}