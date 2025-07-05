package moe.lasoleil.karamelle

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.CompletableFuture

/**
 * The user-defined priority must not be [Int.MIN_VALUE] or [Int.MAX_VALUE].
 * They are reserved for internal usages.
 */
@Suppress("NOTHING_TO_INLINE")
internal inline fun validatePriority(interceptor: EventInterceptor<*>) {
    require(interceptor.priority != Int.MIN_VALUE)
    require(interceptor.priority != Int.MAX_VALUE)
}

internal inline fun <T> completableFuture(block: (CompletableFuture<T>) -> Unit): CompletableFuture<T> =
    CompletableFuture<T>().also(block)

internal class CommonTailInterceptor<E : Any>(override val type: Class<E>) : EventInterceptor<E> {
    /**
     * Always active to send event to the flow.
     */
    override val isActive: Boolean get() = true

    /**
     * Trigger after all other interceptors.
     */
    override val priority: Int get() = Int.MIN_VALUE

    /**
     * Make sure [MutableSharedFlow.tryEmit] always returns true.
     */
    private val instantFlow = MutableSharedFlow<E>(onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /**
     * The [SharedFlow] that can be used to subscribe to events.
     */
    val flow: SharedFlow<E> get() = instantFlow

    override fun invoke(event: E) {
        instantFlow.tryEmit(event)
    }
}

/**
 * Inserts an element at the specified index in the array and returns a new array.
 *
 * @receiver original array.
 * @param index The index at which the element should be inserted. Must be in the range 0 to [Array.size].
 * @param element The element to be inserted into the array.
 * @return A new array containing all elements of the original array with the specified element inserted at the given index.
 */
internal inline fun <reified T> Array<out T>.insertAt(index: Int, element: T): Array<out T> {
    val result = arrayOfNulls<T>(size + 1)
    copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = index)
    result[index] = element
    copyInto(result, destinationOffset = index + 1, startIndex = index, endIndex = size)
    @Suppress("UNCHECKED_CAST")
    return result as Array<out T>
}

/**
 * Removes the element at the specified index from the array and returns a new array.
 *
 * @receiver original array.
 * @param index The index of the element to be removed. Must be in the range 0 to [Array.lastIndex].
 * @return A new array containing all elements of the original array except the one at the specified index.
 */
internal inline fun <reified T> Array<out T>.removeAt(index: Int): Array<out T> {
    val result = arrayOfNulls<T>(size - 1)
    copyInto(result, destinationOffset = 0, startIndex = 0, endIndex = index)
    copyInto(result, destinationOffset = index, startIndex = index + 1, endIndex = size)
    @Suppress("UNCHECKED_CAST")
    return result as Array<out T>
}

/**
 * Searches for the specified item in the array starting from the given central index.
 *
 * This method assumes that:
 * - The array is sorted according to [comparator].
 * - The items might be not [equals] although [comparator].[compareTo] returns 0.
 *
 * @param centralIndex The starting index for the search, typically obtained from a binary search.
 * @param item The item to search for in the array.
 * @param comparator The comparator used to compare array elements with the target item.
 * @return The index of the first occurrence of the item in the array, or -1 if the item is not found.
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
