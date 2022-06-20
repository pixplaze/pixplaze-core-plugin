package com.pixplaze.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.pixplaze.http.annotations.GetHandler;
import com.pixplaze.http.annotations.PostHandler;
import com.pixplaze.http.server.QueryParams;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Utils;

import com.sun.net.httpserver.HttpExchange;

import org.apache.commons.lang.math.NumberUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Logger;

public class RconHttpController implements HttpController {

	private final PixplazeRootsAPI plugin;
	private final Logger logger;
	private final Gson gson = new Gson();

	public RconHttpController(PixplazeRootsAPI plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	@Override
	public void beforeEach(HttpExchange exchange) {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
	}

	@GetHandler("/rcon/lines")
	public List<String> handleGetRconLines(HttpExchange exchange, QueryParams params) throws IOException {
		final var MAX_LINES_COUNT = plugin.getConsoleBuffer().getSize();

		if (!processRequestToken(exchange, params)) throw new RuntimeException("Ti pidor");
		return PixplazeRootsAPI.getInstance().getConsoleBuffer().getHistory(params.getAsInt("count"));

//		var rb = new ResponseBodyBuilder();
//
//		/*
//		 * Получение количества возвращаемых строчек консоли.
//		 *
//		 * Если это количество не было указано в запросе или равняется нулю, то запрос вернёт
//		 * максимальное количество строк.
//		 */
//		var count = 0;
//		if (!params.has("count")) {
//			count = MAX_LINES_COUNT;
//		} else if (!NumberUtils.isNumber(params.getAsString("count"))) {
//			rb.setError("InvalidCountError").setMessage("Parameter count must be number!");
//			sendResponse(exchange, HttpStatus.BAD_REQUEST.getCode(), rb.getFinal());
//		} else if (params.getAsInt("count") == 0) {
//			count = MAX_LINES_COUNT;
//		} else {
//			count = params.getAsInt("count");
//		}
//
//		var lines = PixplazeRootsAPI.getInstance().getConsoleBuffer().getHistory(count);
//
//		// Попытка отправки результата запроса
//		try {
//			rb.setResponse(linesToJsonResponse(lines)).setMessage("Command request success");
//			sendResponse(exchange, HttpStatus.OK.getCode(), rb.getFinal());
//		} catch (Exception e) {
//			logger.warning(e.getMessage());
//			rb.setError(e.getClass().getTypeName()).setMessage(e.getMessage());
//			sendResponse(exchange, HttpStatus.INTERNAL_ERROR.getCode(), rb.getFinal());
//		}
	}

	@PostHandler("/rcon/command")
	public void handlePostRconCommand(HttpExchange exchange, QueryParams params) throws IOException {
		if (!processRequestToken(exchange, params)) return;

		var rb = new ResponseBodyBuilder();

		// Получение отправленной команды и проверка её на валидность
		var line = "";
		if (!params.has("line")) {
			rb.setError("InvalidParamError").setMessage("No parameter line");
			sendResponse(exchange, HttpStatus.BAD_REQUEST.getCode(), rb.getFinal());
			return;
		} else {
			line = params.getAsString("line");
		}

		if ("".equals(line)) {
			rb.setError("InvalidParamError").setMessage("Parameter line cannot be empty");
			sendResponse(exchange, HttpStatus.BAD_REQUEST.getCode(), rb.getFinal());
			return;
		}

		// Попытка выполнения команды и отправки ответа с результатом на запрос
		try {
			dispatchCommand(plugin.getServer().getConsoleSender(), line);
			rb.setMessage("Command sent successfully");
			sendResponse(exchange, HttpStatus.OK.getCode(), rb.getFinal());
		} catch (Exception e) {
			logger.warning("[ERROR]:\tError on sending command!");
			logger.warning(e.getMessage());
			rb.setMessage("Command execution error");
			sendResponse(exchange, HttpStatus.OK.getCode(), rb.getFinal());
		}
	}

	private void sendResponse(HttpExchange exchange, int code, String responseBody) throws IOException {
		OutputStream outputStream = exchange.getResponseBody();

		byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
		int length = bytes.length;

		exchange.sendResponseHeaders(code, length);

		outputStream.write(bytes);
		outputStream.flush();
		outputStream.close();
		exchange.close();
	}

	private JsonObject linesToJsonResponse(List<String> lines) {
		JsonObject jsonResponse = new JsonObject();
		JsonArray jsonLines = gson.toJsonTree(lines).getAsJsonArray();
		jsonResponse.add("lines", jsonLines);
		return jsonResponse;
	}

	private boolean processRequestToken(HttpExchange exchange, QueryParams params) throws IOException {
		ResponseBodyBuilder rb = new ResponseBodyBuilder();
		if (!params.has("access-token")) {
			rb.setError("InvalidTokenError").setMessage("Token is required");
			sendResponse(exchange, HttpStatus.UNAUTHORIZED.getCode(), rb.getFinal());
			return false;
		} else if (!Utils.checkToken(params.getAsString("access-token"))) {
			rb.setError("InvalidTokenError").setMessage("Access token is invalid");
			sendResponse(exchange, HttpStatus.UNAUTHORIZED.getCode(), rb.getFinal());
			return false;
		}
		return true;
	}

	private void dispatchCommand(CommandSender sender, String command) {
		logger.warning("COMMAND SENT FROM HTTP-RCON:");
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					plugin.getServer().dispatchCommand(sender, command);
				} catch (Exception e) {
					logger.warning(e.getMessage());
				}
			}
		}.runTask(plugin);
	}
}
