
import be.ugent.idlab.predict.ldests.log
import be.ugent.idlab.predict.ldests.solid.SolidConnection
import be.ugent.idlab.predict.ldests.solid.string

object Main {

    // running main in an object, so the logging func is available
    suspend fun main() {
        log("Attempting to connect with the Solid pod...")
        val conn = SolidConnection(url = "https://pod.playground.solidlab.be/")
        log("Found files ${conn.root.files().string()}")
        log("Found directories ${conn.root.directories().string()}")
        // refreshing
        conn.root.invalidate()
        log("Found files again ${conn.root.files().string()}")
        // opening the last directory, if any
        log("Reading the first file: ${conn.root.files().first().content().string()}")
        log("Reading the last file: ${conn.root.files().last().content().string()}")
        val dir = conn.root.directories().lastOrNull()
        log("Found even more files ${dir?.files()?.string()}")
        conn.close()
    }

}

suspend fun main() {
    // temp disabled so it acts as a library
//    Main.main()
}
