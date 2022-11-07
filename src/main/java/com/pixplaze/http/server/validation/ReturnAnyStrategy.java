package com.pixplaze.http.server.validation;

import com.pixplaze.http.server.QueryParams;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Method;

/**
 * Не проверяет возвращаемый тип.
 * Не проверяет порядок аргументов обработчика.
 * Рекомендуемые аргументы обработчика: {@link HttpExchange}, {@link QueryParams}.
 *
 * @since 0.1.1-indev
 */
public class ReturnAnyStrategy implements HandlerValidationStrategy {

	final Class<?>[] requiredParameterTypes = new Class[] {HttpExchange.class, QueryParams.class};

	@Override
	public boolean isParameterTypesValid(Method method) {
		return HandlerValidationStrategy.getMissedRequiredParams(method, requiredParameterTypes).length == 0;
	}

	@Override
	public boolean isReturnTypeValid(Method method) {
		return true;
	}

	@Override
	public Class<?>[] getRequiredParameterTypes() {
		return requiredParameterTypes;
	}

	@Override
	public Class<?> getRequiredReturnType() {
		return null;
	}
}
