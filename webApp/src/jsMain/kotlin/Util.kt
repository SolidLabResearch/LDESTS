import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

@OptIn(DelicateCoroutinesApi::class)
fun <T> promiseRun(block: suspend () -> T): Promise<T> {
    return GlobalScope.promise { block() }
}