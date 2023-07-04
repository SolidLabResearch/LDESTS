package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Turtle
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
    ): Job {
        log("Checking ${srcs.size} source(s) for ")
        srcs.none { it.onPublisherAttached(this@subscribe) == Publishable.PublisherAttachResult.FAILURE }
        return scope.launch {
            flow.collect { (path, block) -> action(path) { block(this@subscribe) } }
        }
    }

    fun onAttached(publishable: Publishable) {
        srcs.add(publishable)
    }

}
