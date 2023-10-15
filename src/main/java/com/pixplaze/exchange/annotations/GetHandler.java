package com.pixplaze.exchange.annotations;

import io.javalin.http.HandlerType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GetHandler {
    String value();
    HandlerType method() default HandlerType.GET;
}
