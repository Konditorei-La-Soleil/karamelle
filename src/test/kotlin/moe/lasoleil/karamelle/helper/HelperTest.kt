package moe.lasoleil.karamelle.helper

import moe.lasoleil.karamelle.EventInterceptor
import kotlin.test.Test
import kotlin.test.assertEquals

class HelperTest {

    @Test
    fun `test holder with no-arg interceptor`() {
        var string = "initial"
        val example = object : EventInterceptorHolder {
            @InterceptorFunction(type = String::class)
            fun onString() {
                string = "noArg"
            }
        }
        val interceptors = EventInterceptorHolder.Helper.resolveInterceptors(example)

        assertEquals(1, interceptors.size)
        @Suppress("UNCHECKED_CAST")
        (interceptors[0] as EventInterceptor<Any>).invoke("new")
        assertEquals("noArg", string)
    }

    @Test
    fun `test holder with one-arg interceptor`() {
        var string = "initial"
        val example = object : EventInterceptorHolder {
            @InterceptorFunction
            fun onString(str: String) {
                string = str
            }
        }
        val interceptors = EventInterceptorHolder.Helper.resolveInterceptors(example)

        assertEquals(1, interceptors.size)
        @Suppress("UNCHECKED_CAST")
        (interceptors[0] as EventInterceptor<Any>).invoke("new")
        assertEquals("new", string)
    }


}