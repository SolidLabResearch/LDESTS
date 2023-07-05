package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.util.error
import be.ugent.idlab.predict.ldests.util.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class PublishBuffer {

    // internal flow used to allow attached publishers to interact with the data
    private val flow = MutableSharedFlow<Pair<String, Turtle.(publisher: Publisher) -> Unit>>()
    private val srcs = mutableListOf<Publishable>()

    suspend fun emit(path: String, data: Turtle.(publisher: Publisher) -> Unit) {
        flow.emit(path to data)
    }

    /**
     * Subscribes to the internal flow. Blocks as long as it is subscribed, so stopping a subscription is as simple
     *  as cancelling the job this subscription is put in
     */
    internal suspend fun Publisher.subscribe(
        scope: CoroutineScope,
        action: suspend (path: String, item: Turtle.() -> Unit) -> Unit
    ): Job? {
        log("Checking ${srcs.size} source(s) for ${this::class.simpleName}")
        run {
            // temporary scope so the collection can go out of scope after init
            val new = mutableListOf<Publishable>()
            srcs.forEach {
                when (it.onPublisherAttached(this@subscribe)) {
                    Publishable.PublisherAttachResult.SUCCESS -> { /* nothing to do */ }
                    Publishable.PublisherAttachResult.FAILURE -> {
                        error("`${this::class.simpleName}` could not subscribe! Failure caused by publishable `${it::class.simpleName}` instance.")
                        return null
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
        return scope.launch {
            flow.collect { (path, block) -> action(path) { block(this@subscribe) } }
        }
    }

    fun onAttached(publishable: Publishable) {
        srcs.add(publishable)
    }

}
