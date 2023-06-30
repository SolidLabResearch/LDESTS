package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.core.Stream.Companion.fragment
import be.ugent.idlab.predict.ldests.core.Stream.Companion.resource
import be.ugent.idlab.predict.ldests.core.Stream.Companion.stream
import be.ugent.idlab.predict.ldests.rdf.Turtle
import be.ugent.idlab.predict.ldests.util.log

// TODO: make this an actual (sealed) class, w/ auth, base url (host? used for checking in post method)
object Publisher {

    private val buffer = mutableMapOf<String, String>()

    // maybe instead of `suspend`, custom scope here for publishing?
    suspend fun post(url: String, content: Turtle.() -> Unit) {
        buffer[url] = Turtle { content() }
    }

    suspend fun flush() {
        log("Publishing remaining items (${buffer.size} items remaining)")
        // TODO: actual publishing
        buffer.forEach { (url, buffer) ->
            log("For target `$url`:\n${buffer.trim()}")
        }
    }

    suspend fun Stream.publish() {
        post(url) { stream(this@publish) }
    }

    suspend fun Stream.Fragment.publish() {
        post(url) { fragment(this@publish) }
    }

    suspend fun Stream.Fragment.Resource.publish() {
        post(url) { resource(this@publish) }
    }

}