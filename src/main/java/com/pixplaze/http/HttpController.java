package com.pixplaze.http;

import com.pixplaze.annotations.RequestHandler;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.util.logging.Logger;

public abstract class HttpController {
	protected final Logger logger;
	protected final PixplazeRootsAPI plugin;
	private final HttpServer httpServer;

	protected HttpController(RconHttpServer rconHttpServer) {
		this.httpServer = rconHttpServer.getParent();
		this.plugin = PixplazeRootsAPI.getInstance();
		this.logger = PixplazeRootsAPI.getInstance().getLogger();
	}

	protected void beforeEach(HttpExchange exchange) {}

	protected final void init(HttpController controller) {
		mapHandlers(controller);
	}

	private void mapHandlers(HttpController controller) {
		var allMethods = controller.getClass().getMethods();
		var contextMapper = new ContextMapper();

		for (var method: allMethods) {
			if (method.isAnnotationPresent(RequestHandler.class)) {
				var annotation = method.getAnnotation(RequestHandler.class);
				contextMapper.mapContext(annotation.path(), annotation.method(), method);
			}
		}

		contextMapper.getContextMapping().forEach((context, mapping) -> {
			httpServer.createContext(context, exchange -> {
				beforeEach(exchange);
				try {
					switch (exchange.getRequestMethod()) {
						case "GET" -> {
							var getHandler = mapping.get(Methods.GET);
							if (getHandler != null) getHandler.invoke(controller, exchange);
						}
						case "POST" -> {
							var postHandler = mapping.get(Methods.POST);
							if (postHandler != null) postHandler.invoke(controller, exchange);
						}
						case "PUT" -> {
							var putHandler = mapping.get(Methods.PUT);
							if (putHandler != null) putHandler.invoke(controller, exchange);
						}
						case "DELETE" -> {
							var deleteHandler = mapping.get(Methods.DELETE);
							if (deleteHandler != null) deleteHandler.invoke(controller, exchange);
						}
					}
				} catch (Throwable e) {
					logger.warning(e.getMessage());
				}
				exchange.close();
			});
		});

		var contexts = contextMapper.getContextMapping();
		var contextsCount = contexts.size();
		logger.warning("Count of contexts: %s".formatted(contextsCount));
		contexts.forEach((path, mapping) -> {
			logger.warning("Context path: %s".formatted(path));
			mapping.forEach((restMethods, method) -> {
				logger.warning("\tContext method: %s".formatted(restMethods));
				logger.warning("\tContext handler: %s".formatted(method.getName()));
			});
			logger.warning("");
		});
	}
}
