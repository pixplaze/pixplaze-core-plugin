package com.pixplaze.exchange.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation marking the class as a REST API controller.
 * Classes annotated with this annotation must have only one
 * default constructor with no arguments.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RestController {
    String value();
}
