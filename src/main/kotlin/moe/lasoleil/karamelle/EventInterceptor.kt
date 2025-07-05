package moe.lasoleil.karamelle

interface EventInterceptor<T : Any> {
    val type: Class<T>

    val priority: Int

    val isActive: Boolean

    operator fun invoke(event: T)

    companion object {
        @JvmField
        val PRIORITY_ORDER = Comparator<EventInterceptor<*>> { o1, o2 ->
            o2.priority.compareTo(o1.priority)
        }

        @JvmSynthetic
        @JvmStatic
        inline operator fun <reified T : Any> invoke(
            priority: Int = 0,
            crossinline isActive: () -> Boolean = { true },
            crossinline block: (T) -> Unit,
        ): EventInterceptor<T> = object : EventInterceptor<T> {
            override val type: Class<T> get() = T::class.java
            override val priority: Int get() = priority
            override val isActive: Boolean get() = isActive()
            override fun invoke(event: T) {
                block(event)
            }
        }

        @JvmSynthetic
        @JvmStatic
        inline operator fun <reified T : Any> invoke(
            priority: Int = 0,
            isActive: Boolean = true,
            crossinline block: (T) -> Unit,
        ): EventInterceptor<T> = invoke(priority, { isActive }, block)
    }
}
