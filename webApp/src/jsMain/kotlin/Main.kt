import be.ugent.idlab.predict.ldests.log
import be.ugent.idlab.predict.ldests.solid.SolidConnection
import be.ugent.idlab.predict.ldests.solid.string

object Main {

    // running main in an object, so the logging func is available
    suspend fun main() {
        log("Attempting to connect with the Solid pod...")
        val conn = SolidConnection(url = "https://pod.playground.solidlab.be/")
        log("Found files ${conn.root.files.await().string()}")
        log("Found directories ${conn.root.directories.await().string()}")
        // opening the last directory, if any
        val dir = conn.root.directories.await().lastOrNull()
        log("Found even more files ${dir?.files?.await()?.string()}")
    }

}

suspend fun main() {
    Main.main()
}
