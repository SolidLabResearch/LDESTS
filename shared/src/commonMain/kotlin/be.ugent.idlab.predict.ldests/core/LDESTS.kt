package be.ugent.idlab.predict.ldests.core

import be.ugent.idlab.predict.ldests.rdf.file
import kotlinx.coroutines.*

class LDESTS {

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Unconfined + job)

    fun append(filename: String) {
        scope.launch {
            file(filename) {
                println("Found triple with subject ${it.subject.value}!")
            }
            println("Finished!")
        }
    }

    fun close() {
        job.cancelChildren()
    }

}