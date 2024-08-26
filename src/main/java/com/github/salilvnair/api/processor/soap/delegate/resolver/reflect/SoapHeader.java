package com.github.salilvnair.api.processor.soap.delegate.resolver.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE, FIELD})
public @interface SoapHeader {
    String name() default "";
    String namespace() default "";
    String namespaceUri() default "";
    String qNamespace() default "";
    String qNamespaceUri() default "";
    boolean inheritNamespace() default true;
    SoapHeaderAttribute[] attributes() default {};
}
