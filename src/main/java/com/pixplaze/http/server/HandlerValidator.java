package com.pixplaze.http.server;

import com.pixplaze.http.exceptions.InvalidRequestHandler;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.pixplaze.util.Types.stringifyTypes;

/**
 * Интерфейс описывающий валидатор метода-обработчика запроса.
 * Наследники интерфейса проверяют на корректность методы-обработчики
 * в контроллерах запросов ({@link com.pixplaze.http.HttpController}).
 *
 * Вызываются в {@link com.pixplaze.http.server.ContextMapper}, при маппинге
 * метода и контекста.
 *
 * При неправильных аргументах или возвращаемом типе метода-обработчика,
 * выбрасывает исключение, при этом, контекст не создаётся.
 *
 * @see SoftHandlerValidator
 * @see StrongHandlerValidator
 *
 * @since 0.1.1-indev
 */
public interface HandlerValidator {
	/**
	 * Общая валидация обработчика {@code method}.
	 * По умолчанию проверяет корректность аргументов метода
	 * и возвращаемый тип.
	 *
	 * @param method метод-обработчик, проверяемый на корректность.
	 *
	 * @exception InvalidRequestHandler если любое из условий
	 * корректности не соблюдено.
	 */
	default void validate(Method method) {
		if (!isParameterTypesValid(method)) {
			throw new InvalidRequestHandler(
					"Invalid handler params in method %s.%s(%s)! Expected: %s"
					.formatted(
							method.getDeclaringClass().getCanonicalName(),
							method.getName(),
							stringifyTypes(method.getParameterTypes()),
							stringifyTypes(getRequiredParameterTypes())
					)
			);
		}

		if (!isReturnTypeValid(method))
			throw new InvalidRequestHandler(
					"Invalid handler return type in %s.%s(...)! Expected: %s."
					.formatted(
							method.getDeclaringClass().getCanonicalName(),
							method.getName(),
							getRequiredReturnType() != null? getRequiredReturnType() : "any not void"
					)
			);
	}

	/**
	 * Возвращает те типы из списка {@code requiredParameterTypes}, которые не
	 * принимает метод {@code method} как аргументы.
	 *
	 * @param method метод, в котором проверяется наличие аргументов;
	 * @param requiredParameterTypes типы аргументов, по которым проверяется метод.
	 *
	 * @return список типов, которых не хватает в {@code method}.
	 */
	static Class<?>[] getMissedRequiredParams(Method method, Class<?>[] requiredParameterTypes) {
		var methodTypes = method.getParameterTypes();

		var requiredPresent = new LinkedHashMap<Class<?>, Boolean>();

		outer:
		for (var requiredType: requiredParameterTypes) {
			for (var methodType: methodTypes) {
				if (methodType.isAssignableFrom(requiredType)) {
					requiredPresent.put(requiredType, true);
					continue outer;
				}
			}
			requiredPresent.put(requiredType, false);
		}

		return requiredPresent.entrySet()
				.stream()
				.filter(entry -> !entry.getValue())
				.map(Map.Entry::getKey).toArray(Class[]::new);
	}

	/**
	 * Метод, проверяющий корректность аргументов метода-обработчика {@code method}.
	 * @param method метод, аргументы которого проверяются.
	 * @return {@code true}, если аргументы прошли проверку, {@code false}
	 * - если нет.
	 */
	boolean isParameterTypesValid(Method method);

	/**
	 * Метод, проверяющий корректность возвращаемого типа метода-обработчика {@code method}.
	 * @param method метод, возвращаемый тип которого проверяется.
	 * @return {@code true}, если возвращаемый тип прошёл проверку, {@code false}
	 * - если нет.
	 */
	boolean isReturnTypeValid(Method method);

	/**
	 * Возвращает массив типов рекомендуемых арументов.
	 * @return рекомендуемый массив типов.
	 */
	Class<?>[] getRequiredParameterTypes();

	/**
	 * Возвращает тип рекомендованый для метода-обработчика, как возвращаемый тип.
	 * @return рекомендуемый возвращаемый тип метода-обработчика.
	 */
	Class<?> getRequiredReturnType();
}
