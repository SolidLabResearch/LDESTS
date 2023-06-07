package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.TripleStore
import be.ugent.idlab.predict.ldests.rdf.file
import kotlinx.coroutines.*

class LDESTS {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    private val buffer = TripleStore()

    fun append(filename: String) {
        scope.launch {
            file(filename) { buffer.add(it) }
            println("Read ${buffer.size} triples!")
            val subj = buffer.subjects[5]
            println("Subject #4: ${subj.value}")
            println("All of its data:")
            buffer.forEach(
                subject = subj
            ) {
                println("${it.predicate.value} - ${it.`object`.value}")
            }
        }
    }

    suspend fun flush() {
        job.join()
    }

    suspend fun close() {
        job.cancelAndJoin()
    }

}