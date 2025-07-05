package moe.lasoleil.karamelle.helper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface InterceptorFunction {

    Class<?> type() default void.class;

    int priority() default 0;

}
