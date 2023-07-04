package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.Turtle
import kotlinx.coroutines.flow.MutableSharedFlow

class PublishBuffer {

    // internal flow used to allow attached publishers to interact with the data
    private val flow = MutableSharedFlow<Pair<String, Turtle.(publisher: Publisher) -> Unit>>()

    suspend fun emit(path: String, data: Turtle.(publisher: Publisher) -> Unit) {
        flow.emit(path to data)
    }

    /**
     * Subscribes to the internal flow. Blocks as long as it is subscribed, so stopping a subscription is as simple
     *  as cancelling the job this subscription is put in
     */
    internal suspend fun Publisher.subscribe(action: suspend (path: String, item: Turtle.() -> Unit) -> Unit) {
        flow.collect { (path, block) -> action(path) { block(this@subscribe) } }
    }

}
