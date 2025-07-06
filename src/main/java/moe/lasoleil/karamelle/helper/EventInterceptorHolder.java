package moe.lasoleil.karamelle.helper;

import moe.lasoleil.karamelle.EventInterceptor;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface EventInterceptorHolder {

    default boolean interceptEvents() {
        return true;
    }

    static @NotNull List<@NotNull EventInterceptor<?>> resolveInterceptors(final @NotNull EventInterceptorHolder holder) {
        List<EventInterceptor<?>> interceptors = new ArrayList<>();

        for (Method method : holder.getClass().getMethods()) {
            if (!method.isAnnotationPresent(InterceptorFunction.class)) continue;

            interceptors.add(new MethodEventInterceptor(holder, method));
        }

        return Collections.unmodifiableList(interceptors);
    }

}
