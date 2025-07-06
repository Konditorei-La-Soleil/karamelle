package moe.lasoleil.karamelle

import kotlinx.coroutines.flow.SharedFlow
import moe.lasoleil.karamelle.helper.EventInterceptorHolder
import java.util.IdentityHashMap
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class EventDispatcher @JvmOverloads constructor(
    val identifier: String = "default",
) {

    private val _state = AtomicReference(State.IDLE)

    enum class State {
        IDLE, BUSY, POSTING, MODIFYING
    }

    private sealed interface Operation {
        val newState: State

        class Register(
            val interceptor: EventInterceptor<*>,
            val completableFuture: CompletableFuture<Boolean>,
        ) : Operation {
            override val newState get() = State.MODIFYING
        }

        class RegisterBatch(
            vararg val interceptors: EventInterceptor<*>,
            val completableFuture: CompletableFuture<Int>,
        ) : Operation {
            override val newState get() = State.MODIFYING
        }

        class Unregister(
            val interceptor: EventInterceptor<*>,
            val completableFuture: CompletableFuture<Boolean>,
        ) : Operation {
            override val newState get() = State.MODIFYING
        }

        class UnregisterBatch(
            vararg val interceptors: EventInterceptor<*>,
            val completableFuture: CompletableFuture<Int>,
        ) : Operation {
            override val newState get() = State.MODIFYING
        }

        class PostEvent<E : Any>(
            val event: E,
            val completableFuture: CompletableFuture<E>,
        ) : Operation {
            override val newState get() = State.POSTING
        }

        class GetOrCreateFlow<E : Any>(
            val type: Class<E>,
            val continuation: Continuation<SharedFlow<E>>,
        ) : Operation {
            override val newState get() = State.MODIFYING
        }
    }

    private val interceptors = IdentityHashMap<Class<*>, Array<out EventInterceptor<*>>>()

    private val operationQueue = ConcurrentLinkedQueue<Operation>()

    private fun handleInsert0(interceptor: EventInterceptor<*>): Boolean {
        val type = interceptor.type
        val arrayOfInterceptors = interceptors[type]
        when {
            // 1. Nothing registered
            arrayOfInterceptors == null -> {
                // Added one is always before commonTail
                interceptors[type] = arrayOf(interceptor, CommonTailInterceptor(type))
            }
            // 2. Only commonTail
            arrayOfInterceptors.size == 1 -> {
                interceptors[type] = arrayOf(interceptor, arrayOfInterceptors[0])
            }
            // 3. normal
            else -> {
                // Binary search for position
                val index = arrayOfInterceptors.binarySearch(interceptor, EventInterceptor.Companion.PRIORITY_ORDER)
                // No such priority
                if (index < 0) {
                    interceptors[type] = arrayOfInterceptors.insertAt(index.inv(), interceptor)
                } else {
                    // Seek to check if it is already registered
                    if (arrayOfInterceptors.searchItem(index, interceptor, EventInterceptor.Companion.PRIORITY_ORDER) != -1) {
                        return false
                    }
                    interceptors[type] = arrayOfInterceptors.insertAt(index, interceptor)
                }
            }
        }
        return true
    }

    private fun handleRemove0(interceptor: EventInterceptor<*>): Boolean {
        val type = interceptor.type
        val arrayOfInterceptors = interceptors[type]
        return when {
            // Nothing registered or only commonTail
            arrayOfInterceptors == null || arrayOfInterceptors.size == 1 -> false
            else -> {
                val index = arrayOfInterceptors.binarySearch(interceptor, EventInterceptor.Companion.PRIORITY_ORDER)
                if (index < 0) {
                    return false
                }

                val searchIndex = arrayOfInterceptors.searchItem(index, interceptor, EventInterceptor.Companion.PRIORITY_ORDER)
                if (searchIndex < 0) {
                    false
                } else {
                    interceptors[type] = arrayOfInterceptors.removeAt(searchIndex)
                    true
                }
            }
        }
    }

    private fun handleOperation0(operation: Operation) {
        _state.set(operation.newState)
        when (operation) {
            is Operation.Register -> {
                operation.completableFuture.complete(
                    handleInsert0(operation.interceptor)
                )
            }
            is Operation.RegisterBatch -> {
                operation.completableFuture.complete(
                    operation.interceptors.count(::handleInsert0)
                )
            }
            is Operation.Unregister -> {
                operation.completableFuture.complete(
                    handleRemove0(operation.interceptor)
                )
            }
            is Operation.UnregisterBatch -> {
                operation.completableFuture.complete(
                    operation.interceptors.count(::handleRemove0)
                )
            }
            is Operation.PostEvent<*> -> {
                val event = operation.event
                val type = event.javaClass
                val completableFuture = operation.completableFuture as CompletableFuture<Any>
                val typedInterceptors = interceptors.getOrPut(type) { arrayOf(CommonTailInterceptor(type)) }
                @Suppress("UNCHECKED_CAST")
                typedInterceptors as Array<out EventInterceptor<Any>>
                for (interceptor in typedInterceptors) {
                    try {
                        if (interceptor.isActive) {
                            interceptor(event)
                        }
                    } catch (e: Exception) {
                        completableFuture.completeExceptionally(e)
                        break
                    }
                }
                if (!completableFuture.isDone) {
                    completableFuture.complete(event)
                }
            }
            is Operation.GetOrCreateFlow<*> -> {
                val type = operation.type
                val typedInterceptors = interceptors.getOrPut(type) { arrayOf(CommonTailInterceptor(type)) }
                @Suppress("UNCHECKED_CAST")
                val tail = typedInterceptors.last() as CommonTailInterceptor<Nothing>
                operation.continuation.resume(tail.flow)
            }
        }
        _state.set(State.BUSY)
    }

    private fun handleOperation(operation: Operation) {
        if (!_state.compareAndSet(State.IDLE, State.BUSY)) {
            operationQueue.add(operation)
            return
        }

        handleOperation0(operation)
        while (true) {
            handleOperation0(operationQueue.poll() ?: break)
        }
        _state.set(State.IDLE)
    }

    @get:JvmName("state")
    val state: State get() = _state.get()

    fun <T : Any> post(event: T): Future<T> {
        return completableFuture {
            handleOperation(Operation.PostEvent(event, it))
        }
    }

    fun register(interceptor: EventInterceptor<*>): Future<Boolean> {
        validatePriority(interceptor)
        return completableFuture {
            handleOperation(Operation.Register(interceptor, it))
        }
    }

    fun register(holder: EventInterceptorHolder): Future<Int> =
        registerAll(interceptors = EventInterceptorHolder.resolveInterceptors(holder).toTypedArray())

    fun registerAll(vararg interceptors: EventInterceptor<*>): Future<Int> {
        require(interceptors.isNotEmpty())
        interceptors.forEach(::validatePriority)
        return completableFuture {
            handleOperation(Operation.RegisterBatch(interceptors = interceptors, it))
        }
    }

    fun unregister(interceptor: EventInterceptor<*>): Future<Boolean> {
        validatePriority(interceptor)
        return completableFuture {
            handleOperation(Operation.Unregister(interceptor, it))
        }
    }

    fun unregister(holder: EventInterceptorHolder): Future<Int> =
        unregisterAll(interceptors = EventInterceptorHolder.resolveInterceptors(holder).toTypedArray())

    fun unregisterAll(vararg interceptors: EventInterceptor<*>): Future<Int> {
        require(interceptors.isNotEmpty())
        interceptors.forEach(::validatePriority)
        return completableFuture {
            handleOperation(Operation.UnregisterBatch(interceptors = interceptors, it))
        }
    }

    suspend fun <E : Any> eventFlow(type: Class<E>): SharedFlow<E> {
        return suspendCoroutine { continuation ->
            handleOperation(Operation.GetOrCreateFlow(type, continuation))
        }
    }

    override fun toString(): String {
        return "EventDispatcher(identifier='$identifier')"
    }

}