package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.RDFBuilder
import be.ugent.idlab.predict.ldests.util.error
import be.ugent.idlab.predict.ldests.util.log

class PublishBuffer(
    // the registered sources, used when attaching a publisher
    private val srcs: MutableList<Publishable> = mutableListOf()
) {

    private val publishers = mutableSetOf<Publisher>()

    suspend fun emit(path: String, data: RDFBuilder.() -> Unit) {
        publishers.forEach { it.publish(path, data) }
    }

    /**
     * Checks for compatibility between the publisher & publishing sources. Returns `true` upon successful
     */
    internal suspend fun Publisher.attach(): Boolean {
        log("Checking ${srcs.size} source(s) for ${this::class.simpleName}")
        run {
            // temporary scope so the collection can go out of scope after init
            val new = mutableListOf<Publishable>()
            srcs.forEach {
                when (it.onPublisherAttached(this@attach)) {
                    Publishable.PublisherAttachResult.SUCCESS -> { /* nothing to do */ }
                    Publishable.PublisherAttachResult.FAILURE -> {
                        error("`${this::class.simpleName}` could not subscribe! Failure caused by publishable `${it::class.simpleName}` instance.")
                        // failure, not attaching
                        return false
                    }
                    Publishable.PublisherAttachResult.NEW -> new.add(it)
                }
            }
            // no sources failed to attach, initialising the new ones
            new.forEach { publishable ->
                log("Initialising publishable `${publishable::class.simpleName}` for publisher `${this::class.simpleName}`")
                publishable.onCreate(publisher = this)
            }
        }
        // success, so adding it
        publishers.add(this)
        return true
    }

    fun Publisher.detach() {
        publishers.remove(this)
    }

    fun onAttached(publishable: Publishable) {
        srcs.add(publishable)
    }

}
