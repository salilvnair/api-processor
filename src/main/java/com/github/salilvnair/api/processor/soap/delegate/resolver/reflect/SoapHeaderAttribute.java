package com.github.salilvnair.api.processor.soap.delegate.resolver.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({FIELD})
public @interface SoapHeaderAttribute {
    String name();
    String value();
}
