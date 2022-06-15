package com.pixplaze.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.pixplaze.http.annotations.RequestHandler;
import com.pixplaze.http.server.QueryParams;

/**
 * Интерфейс, описывающий контроллер запросов.
 *
 * Контроллер запросов - обёртка над {@link HttpHandler}, позволяющая
 * разделить обработку запросов по разным методам,
 * аннотированным {@link RequestHandler}, вместо одного
 * {@link HttpHandler#handle(HttpExchange)} метода.
 *
 * Каждый метод обработчик должен быть аннотирован {@code RequestHandler(method, path)},
 * где method - метод HTTP-запроса которые он должен обрабатывать, а
 * path - путь, который он должен обрабатывать.
 *
 * Рекомендуется, чтобы каждый метод-обработчик принимал два параметра:
 * {@link HttpExchange} и {@link QueryParams}.
 *
 * Пример метода обработчика в теле {@code HttpController}:
 * <pre> {@code
 * @RequestHandler(method = "POST", path = "/index")
 * public void handleIndex(HttpExchange exchange, QueryParams params) {...}
 * } </pre>
 *
 * @see HttpHandler
 * @see RequestHandler
 * @see QueryParams
 *
 * @since v0.1.0-indev
 */
public interface HttpController {
	/**
	 * Метод, выполняющийся перед любым методом-обработчиком.
	 * Например, в нём можно установить заголовки ответа, перед выполнением
	 * тела обработчика, которые будут применяться в пределах этого HttpController.
	 *
	 * По умолчанию пуст, что значит, что перед выполнением обработчика не происходит
	 * никаких действий.
	 *
	 * @param exchange объект обмена между сервером и клиентом, из которого можно
	 *                 извлекать данные запроса-ответа.
	 *
	 * @see HttpExchange
	 */
	default void beforeEach(HttpExchange exchange) {}
}
