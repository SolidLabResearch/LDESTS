import be.ugent.idlab.predict.ldests.remote.Queries
import be.ugent.idlab.predict.ldests.remote.query
import be.ugent.idlab.predict.ldests.solid.SolidConnection

suspend fun main() {
    console.log("Attempting to connect with the Solid pod...")
    val conn = SolidConnection(url = "https://pod.playground.solidlab.be/") {
        rootDir = ""
    }
    query(
        url = "https://pod.playground.solidlab.be/",
        query = Queries.SPARQL_GET_ALL(limit = 5)
    ) {
        println("Received ${it.s}, ${it.p}, ${it.o}!")
    }
//    console.log("Found files ${conn.open()?.getFiles()}")
}
