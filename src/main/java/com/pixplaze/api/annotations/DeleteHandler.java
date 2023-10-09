package com.pixplaze.api.annotations;

import io.javalin.http.HandlerType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface DeleteHandler {
    String value();
    HandlerType method() default HandlerType.DELETE;
}
