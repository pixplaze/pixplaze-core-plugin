package com.pixplaze.http;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.pixplaze.http.annotations.RequestHandler;
import com.pixplaze.http.server.QueryParams;
import com.pixplaze.plugin.PixplazeRootsAPI;
import com.pixplaze.util.Utils;

import com.sun.net.httpserver.HttpExchange;

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

	public RconHttpController(PixplazeRootsAPI plugin, Logger logger) {
		this.plugin = plugin;
		this.logger = logger;
	}

	@Override
	public void beforeEach(HttpExchange exchange) {
		exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
	}

	@RequestHandler(method = Methods.GET, path = "/rcon/lines")
	public void handleLinesRequest(HttpExchange exchange, QueryParams params) throws IOException {
		final var MAX_LINES_COUNT = plugin.getConsoleBuffer().getSize();
		ResponseBodyBuilder rb = new ResponseBodyBuilder();

		if (!params.has("access-token")) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("No token!").getFinal());

			return;
		} else if (!Utils.checkToken(params.getAsString("access-token"))) {
			sendResponse(exchange, 401, rb.setError("InvalidTokenError").setMessage("Access token is invalid").getFinal());
			return;
		}

		int count = 0;
		if (!params.has("count")) {
			count = MAX_LINES_COUNT;
		} else {
			count = params.getAsInt("count");
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
