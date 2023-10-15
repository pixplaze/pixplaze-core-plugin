package com.pixplaze.exchange.exceptions;

import java.util.Arrays;
import java.util.stream.Collectors;

public class HandlerMappingException extends RuntimeException {
    public HandlerMappingException() {
        super("Invalid controller class!");
    }

    public HandlerMappingException(String message) {
        super(message);
    }

    public HandlerMappingException(Throwable cause) {
        super(cause);
    }

    public HandlerMappingException(Class<?> controllerClass, Class<?>[] parameters) {
        super("Invalid controller %s(%s)!".formatted(
                controllerClass.getSimpleName(),
                Arrays.stream(parameters)
                        .map(Class::getSimpleName)
                        .collect(Collectors.joining(", "))));
    }
}
