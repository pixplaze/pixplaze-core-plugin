package com.pixplaze.http.server.validation;

import com.pixplaze.http.server.QueryParams;
import com.sun.net.httpserver.HttpExchange;

import java.lang.reflect.Method;

/**
 * Возвращаемый тип - {@code void};
 * Порядок аргументов метода-обработчика имеет значение;
 * Рекомендуемые аргументы обработчика: {@link HttpExchange}, {@link QueryParams}.
 *
 * @since 0.1.1-indev
 */
public class ReturnVoidStrategy implements HandlerValidationStrategy {

	private final Class<?> requiredReturnType = Void.TYPE;
	private final Class<?>[] requiredParameterTypes = new Class[] {HttpExchange.class, QueryParams.class};

	@Override
	public boolean isParameterTypesValid(Method method) {
		var methodTypes = method.getParameterTypes();
		var isAllValid = true;
		try {
			for (var i = 0; i < requiredParameterTypes.length; i++) {
				if (methodTypes[i] != requiredParameterTypes[i]) {
					isAllValid = false;
					break;
				}
			}
		} catch (IndexOutOfBoundsException e) {
			isAllValid = false;
		}
		return isAllValid;
	}

	@Override
	public boolean isReturnTypeValid(Method method) {
		return method.getReturnType().isAssignableFrom(requiredReturnType);
	}

	@Override
	public Class<?>[] getRequiredParameterTypes() {
		return requiredParameterTypes;
	}

	@Override
	public Class<?> getRequiredReturnType() {
		return requiredReturnType;
	}
}
