package com.pixplaze.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.pixplaze.annotations.RequestHandler;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Utils;

import com.sun.net.httpserver.HttpExchange;

import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class RconHttpController implements HttpController {

	private final PixplazeRootsAPI plugin;
	private final Logger logger;

	public RconHttpController(PixplazeRootsAPI plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	@Override
	public void beforeEach(HttpExchange exchange) {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
	}

	@RequestHandler(method = Methods.GET, path = "/rcon/lines")
	public void handleLinesRequest(HttpExchange exchange) throws IOException {
		final var MAX_LINES_COUNT = plugin.getConsoleBuffer().getSize();
		Map<String, String> params;
		ResponseBodyBuilder rb = new ResponseBodyBuilder();

		try {
			params = parseParams(exchange.getRequestURI().getQuery());
		} catch (Exception e) {
			sendResponse(exchange, 400, rb.setError("ParamsParseError").setMessage("Error occurred while parsing params").getFinal());
			return;
		}

		if (!params.containsKey("access-token")) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());

			return;
		} else if (!Utils.checkToken(params.get("access-token"))) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
			return;
		}

		int count = 0;
		if (!params.containsKey("count")) {
			count = MAX_LINES_COUNT;
		} else {
			count = Integer.parseInt(params.get("count"));
		}

		if (count == 0) {
			sendResponse(exchange, 401, rb.setError("ZeroCountError").setMessage("Param count cannot be zero").getFinal());
			return;
		}

		List<String> lines = PixplazeRootsAPI.getInstance().getConsoleBuffer().getHistory(count);

		try {
			// fixme
			Gson gson = new Gson();
			JsonObject jsonResponse = new JsonObject();

			JsonArray jsonLines = gson.toJsonTree(lines).getAsJsonArray();

			jsonResponse.add("lines", jsonLines);
			//fixme

			rb.setResponse(jsonResponse).setMessage("Command request success");

			//            logger.warning("NOT pidoor");
			sendResponse(exchange, 200, rb.getFinal());
		} catch (Exception e) {
			//            logger.warning("pidoor");
			logger.warning(e.getMessage());
			sendResponse(exchange, 500, rb.setError(e.getClass().getTypeName()).setMessage(e.getMessage()).getFinal());
		}

	}

	@RequestHandler(method = Methods.POST, path = "/rcon/command")
	public void handleCommandRequest(HttpExchange exchange) throws IOException {
		Map<String, String> params;
		ResponseBodyBuilder rb = new ResponseBodyBuilder();

		try {
			params = parseParams(exchange.getRequestURI().getQuery());
		} catch (Exception e) {
			sendResponse(exchange, 400, rb.setError("ParamsParseError").setMessage("Error occurred while parsing params").getFinal());
			return;
		}

		if (!params.containsKey("access-token")) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());
			return;
		} else if (!Utils.checkToken(params.get("access-token"))) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
			return;
		}

		String line = "";
		if (!params.containsKey("line")) {
			sendResponse(exchange, 401, rb.setError("InvalidParamError").setMessage("No param line").getFinal());
			return;
		} else {
			line = params.get("line").replace("%20"," ");
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

	private Map<String, String> parseParams(String query) {
		if (query == null) {
			return null;
		}
		Map<String, String> result = new HashMap<>();
		for (String param : query.split("&")) {
			String[] entry = param.split("=");
			if (entry.length > 1) {
				result.put(entry[0], entry[1]);
			} else{
				result.put(entry[0], "");
			}
		}
		return result;
	}

	private void dispatchCommand(CommandSender sender, String command) {
		logger.warning("requested server command");
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					plugin.getServer().dispatchCommand(sender, command);
				} catch (Exception e) {
					logger.warning(e.getMessage());
				}
				logger.warning("Command sended");
			}
		}.runTask(plugin);
	}
}
