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

    final class Helper {

        private Helper() {
        }

        private static final class MethodEventInterceptor implements EventInterceptor<Object> {
            private static final MethodHandles.Lookup lookup = MethodHandles.lookup();

            private final EventInterceptorHolder holder;
            private final Class<?> type;
            private final int priority;
            private final MethodHandle methodHandle;
            private final boolean noArg;

            MethodEventInterceptor(final @NotNull EventInterceptorHolder holder, final @NotNull Method method) throws IllegalArgumentException {
                InterceptorFunction annotation = method.getAnnotation(InterceptorFunction.class);
                if (annotation == null) {
                    throw new IllegalArgumentException("Method " + method.getName() + " is not an interceptor function");
                }

                this.holder = holder;
                this.priority = annotation.priority();
                this.noArg = method.getParameterCount() == 0;
                // If the type is void, use the type of the first parameter
                if (annotation.type() != void.class) {
                    type = annotation.type();
                } else if (noArg) {
                    throw new IllegalArgumentException("Method " + method.getName() + " has no parameters. You should specify the type of the event on annotation or use a method with one parameter.");
                } else {
                    type = method.getParameterTypes()[0];

                if (method.getParameterCount() > 1) {
                    throw new IllegalArgumentException("Method " + method.getName() + " has more than one parameter.");
                }

                try {
                    this.methodHandle = lookup.unreflect(method).bindTo(holder);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Method " + method.getName() + " is not accessible.", e);
                }
            }

            @SuppressWarnings("unchecked")
            @Override
            public @NotNull Class<@NotNull Object> getType() {
                return (Class<Object>) type;
            }

            @Override
            public int getPriority() {
                return priority;
            }

            @Override
            public boolean isActive() {
                return holder.interceptEvents();
            }

            @Override
            public void invoke(@NotNull Object event) throws Throwable {
                if (noArg) {
                    methodHandle.invokeExact();
                } else {
                    methodHandle.invoke(event);
                }
            }
        }

        public static @NotNull List<@NotNull EventInterceptor<?>> resolveInterceptors(final @NotNull EventInterceptorHolder holder) {
            List<EventInterceptor<?>> interceptors = new ArrayList<>();

            for (Method method : holder.getClass().getMethods()) {
                if (!method.isAnnotationPresent(InterceptorFunction.class)) continue;

                interceptors.add(new MethodEventInterceptor(holder, method));
            }

            return Collections.unmodifiableList(interceptors);
        }
    }

}
