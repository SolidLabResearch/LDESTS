package be.ugent.idlab.predict.ldests.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import kotlin.js.Promise

@OptIn(DelicateCoroutinesApi::class)
inline fun <T> promise(
    scope: CoroutineScope = GlobalScope,
    crossinline block: suspend () -> T
): Promise<T> {
    return scope.promise { block() }
}

// helper alias to make types recognizable for JS export
inline fun <T> Collection<T>.arr() = toTypedArray()

inline fun <R, reified T> List<R>.map(transform: (R) -> T) = Array(size) { transform(this[it]) }
