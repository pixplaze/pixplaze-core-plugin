package com.pixplaze.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.pixplaze.http.annotations.*;
import com.pixplaze.http.server.QueryParams;

/**
 * Интерфейс, описывающий контроллер запросов.
 * <br><br>
 * Контроллер запросов - обёртка над {@link HttpHandler}, позволяющая
 * разделить обработку запросов по разным методам,
 * аннотированным {@link GetHandler}, {@link PostHandler}, {@link PutHandler}
 * или {@link DeleteHandler}.
 * <br><br>
 * Каждый метод-обработчик должен быть аннотирован {@link GetHandler},
 * {@link PostHandler}, {@link PutHandler} или {@link DeleteHandler},
 * аннотацией, аргументов в которую передаётся путь, который обрабатывает
 * этот метод.
 * <br><br>
 * Рекомендуется, чтобы каждый метод-обработчик принимал два параметра:
 * {@link HttpExchange} и {@link QueryParams}.
 * <br><br>
 * Пример метода обработчика в теле {@code HttpController}:
 * <pre> {@code
 * @GetHandler("/index")
 * public void handleIndex(HttpExchange exchange, QueryParams params) {...}
 * } </pre>
 *
 * @see GetHandler
 * @see PostHandler
 * @see PutHandler
 * @see DeleteHandler
 * @see HttpHandler
 * @see QueryParams
 *
 * @since v0.1.0-indev
 */
public interface HttpController {
	/**
	 * <pre>
	 * Метод, выполняющийся перед любым методом-обработчиком.
	 * Например, в нём можно установить заголовки ответа, перед выполнением
	 * тела обработчика, которые будут применяться в пределах этого HttpController.
	 *
	 * По умолчанию пуст, что значит, что перед выполнением обработчика не происходит
	 * никаких действий.
	 * </pre>
	 * @param exchange объект обмена между сервером и клиентом, из которого можно
	 *                 извлекать данные запроса-ответа.
	 *
	 * @see HttpExchange
	 */
	default void beforeEach(HttpExchange exchange) {}
}
