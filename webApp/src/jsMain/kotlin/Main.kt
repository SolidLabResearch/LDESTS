import be.ugent.idlab.predict.ldests.LDESTS
import kotlinx.coroutines.await

const val url  = "https://fragments.dbpedia.org/2015/en"

@ExperimentalJsExport
@JsExport
object Test {

    fun createTestLDES() = promiseRun {
        LDESTS.fromUrl(url)
    }

}

@OptIn(ExperimentalJsExport::class)
suspend fun main() {
    console.log("Testing the LDESTS implementation!")
    val ldests = Test.createTestLDES().await()
    console.log("Created the LDESTS stream: $ldests")
}
