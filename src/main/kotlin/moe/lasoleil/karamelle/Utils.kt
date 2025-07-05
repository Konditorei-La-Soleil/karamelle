package moe.lasoleil.karamelle

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.CompletableFuture

@Suppress("NOTHING_TO_INLINE")
internal inline fun validatePriority(interceptor: EventInterceptor<*>) {
    require(interceptor.priority != Int.MIN_VALUE)
    require(interceptor.priority != Int.MAX_VALUE)
}

internal inline fun <T> completableFuture(block: (CompletableFuture<T>) -> Unit): CompletableFuture<T> =
    CompletableFuture<T>().also(block)

internal class CommonTailInterceptor<E : Any>(override val type: Class<E>) : EventInterceptor<E> {
    override val isActive: Boolean get() = true

    override val priority: Int get() = Int.MIN_VALUE

    private val instantFlow = MutableSharedFlow<E>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val flow: SharedFlow<E> get() = instantFlow

    override fun invoke(event: E) {
        instantFlow.tryEmit(event)
    }
}

internal inline fun <reified T> Array<out T>.insertAt(index: Int, element: T): Array<out T> {
    val result = arrayOfNulls<T>(size + 1)
    copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = index)
    result[index] = element
    copyInto(result, destinationOffset = index + 1, startIndex = index, endIndex = size)
    @Suppress("UNCHECKED_CAST")
    return result as Array<out T>
}

internal inline fun <reified T> Array<out T>.removeAt(index: Int): Array<out T> {
    val result = arrayOfNulls<T>(size - 1)
    copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = index)
    copyInto(result, destinationOffset = index, startIndex = index + 1, endIndex = size)
    @Suppress("UNCHECKED_CAST")
    return result as Array<out T>
}

/**
 * Search
 */
internal fun <T> Array<out T>.searchItem(centralIndex: Int, item: T, comparator: Comparator<in T>): Int {
    var i = centralIndex
    while (i >= 0) {
        val cur = this[i]
        if (comparator.compare(cur, item) != 0)
            break
        if (cur == item)
            return i
        i--
    }
    i = centralIndex + 1
    while (i < size) {
        val cur = this[i]
        if (comparator.compare(cur, item) != 0)
            break
        if (cur == item)
            return i
        i++
    }
    return -1
}
