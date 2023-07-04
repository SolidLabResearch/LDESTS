package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.util.warn

abstract class Publishable(
    // own, relative, path after the host (e.g. `stream/`, `stream/fragment/resource`
    internal val path: String
) {

    internal var buffer: PublishBuffer? = null
        private set

    internal open suspend fun onBufferAttached() {
        // nothing to do ootb
    }

    protected suspend fun publish(block: Turtle.(publisher: Publisher) -> Unit) {
        buffer
            ?.emit(path = path, data = block)
            ?: run {
                warn("A publishable type experienced spillage (no buffer attached)!")
            }
    }

    suspend fun attach(buffer: PublishBuffer) {
        this.buffer = buffer
        onBufferAttached()
    }

}