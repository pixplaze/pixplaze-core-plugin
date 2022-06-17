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
	public void handleGetRconLines(HttpExchange exchange, QueryParams params) throws IOException {
		final var MAX_LINES_COUNT = plugin.getConsoleBuffer().getSize();
		ResponseBodyBuilder rb = new ResponseBodyBuilder();

		// Проверка отправленного токена
		if (!params.has("access-token")) {
			rb.setError("InvalidTokenError").setMessage("Token is required");
			sendResponse(exchange, HttpStatus.UNAUTHORIZED.getCode(), rb.getFinal());
			return;
		} else if (!Utils.checkToken(params.getAsString("access-token"))) {
			rb.setError("InvalidTokenError").setMessage("Access token is invalid");
			sendResponse(exchange, HttpStatus.UNAUTHORIZED.getCode(), rb.getFinal());
			return;
		}

		/*
		 * Получение количества возвращаемых строчек консоли.
		 *
		 * Если это количество не было указано в запросе или равняется нулю, то запрос вернёт
		 * максимальное количество строк.
		 */
		int count = 0;
		if (!params.has("count")) {
			count = MAX_LINES_COUNT;
		} else if (!NumberUtils.isNumber(params.getAsString("count"))) {
			rb.setError("InvalidCountError").setMessage("Parameter count must be number!");
			sendResponse(exchange, HttpStatus.BAD_REQUEST.getCode(), rb.getFinal());
		} else if (params.getAsInt("count") == 0) {
			count = MAX_LINES_COUNT;
		} else {
			count = params.getAsInt("count");
		}

		List<String> lines = PixplazeRootsAPI.getInstance().getConsoleBuffer().getHistory(count);

		// Попытка отправки результата запроса
		try {
			rb.setResponse(linesToJsonResponse(lines)).setMessage("Command request success");
			sendResponse(exchange, HttpStatus.OK.getCode(), rb.getFinal());
		} catch (Exception e) {
			logger.warning(e.getMessage());
			rb.setError(e.getClass().getTypeName()).setMessage(e.getMessage());
			sendResponse(exchange, HttpStatus.INTERNAL_ERROR.getCode(), rb.getFinal());
		}
	}

	@PostHandler("/rcon/command")
	public void handleCommandRequest(HttpExchange exchange, QueryParams params) throws IOException {
		ResponseBodyBuilder rb = new ResponseBodyBuilder();

		if (!params.has("access-token")) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());
			return;
		} else if (!Utils.checkToken(params.getAsString("access-token"))) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
			return;
		}

		String line = "";
		if (!params.has("line")) {
			sendResponse(exchange, 401, rb.setError("InvalidParamError").setMessage("No param line").getFinal());
			return;
		} else {
			line = params.getAsString("line");
		}

		if ("".equals(line)) {
			sendResponse(exchange, 401, rb.setError("InvalidParamError").setMessage("line is empty").getFinal());
			return;
		}

		try {
			dispatchCommand(plugin.getServer().getConsoleSender(), line);
			sendResponse(exchange, 200, rb.setMessage("Command sent successfully").getFinal());
		} catch (Exception e) {
			logger.warning("[ERROR]:\tError on sending command!");
			logger.warning(e.getMessage());
			sendResponse(exchange, 200, rb.setMessage("Command execution error").getFinal());
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

	private void dispatchCommand(CommandSender sender, String command) {
		logger.warning("Requested server command");
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					plugin.getServer().dispatchCommand(sender, command);
				} catch (Exception e) {
					logger.warning(e.getMessage());
				}
				logger.warning("Command sent");
			}
		}.runTask(plugin);
	}
}
