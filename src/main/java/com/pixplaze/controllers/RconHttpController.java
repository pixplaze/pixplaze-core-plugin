package com.pixplaze.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import com.pixplaze.http.HttpController;
import com.pixplaze.http.HttpStatus;
import com.pixplaze.http.annotations.GetHandler;
import com.pixplaze.http.annotations.PostHandler;
import com.pixplaze.http.exceptions.HttpException;
import com.pixplaze.http.server.QueryParams;
import com.pixplaze.plugin.PixplazeCorePlugin;
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

	private final PixplazeCorePlugin plugin;
	private final Logger logger;
	private final Gson gson = new Gson();

	public RconHttpController(PixplazeCorePlugin plugin, Logger logger) {
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

		validateAccessToken(params);

		/*
		 * Получение количества возвращаемых строчек консоли.
		 *
		 * Если это количество не было указано в запросе или равняется нулю, то запрос вернёт
		 * максимальное количество строк.
		 */
		int count = params.getAsInt("count").orElse(MAX_LINES_COUNT);

		if (params.has("count") && count == MAX_LINES_COUNT && count != 0)
			throw new HttpException(HttpStatus.BAD_REQUEST, "Parameter count must be an integer!");

		if (count == 0) count = MAX_LINES_COUNT;

		return PixplazeCorePlugin.getInstance().getConsoleBuffer().getHistory(count);
	}

	@PostHandler("/rcon/command")
	public String handlePostRconCommand(HttpExchange exchange, QueryParams params) throws IOException {
		validateAccessToken(params);

		params.getAsString("line")
				.ifPresentOrElse(this::onCommandProvided, this::onCommandMissed);

		return PixplazeCorePlugin.getInstance().getConsoleBuffer().getHistory(-1).get(0);
	}

	private void onCommandProvided(String command) {
		if (command.isBlank())
			throw new HttpException(HttpStatus.BAD_REQUEST, "Query parameter \"line\" is blank!");

		try {
			dispatchCommand(plugin.getServer().getConsoleSender(), command);
		} catch (Exception e) {
			throw new HttpException(HttpStatus.INTERNAL_ERROR, e.getMessage());
		}
	}

	private void onCommandMissed() {
		throw new HttpException(HttpStatus.BAD_REQUEST, "Required query parameter \"line\" is missed!");
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

	private void validateAccessToken(QueryParams params) throws IOException {
		params.getAsString("access-token")
				.ifPresentOrElse(this::onTokenProvided, this::onTokenMissed);
	}

	private void onTokenProvided(final String token) {
		if (Utils.checkToken(token)) return;
		throw new HttpException(HttpStatus.UNAUTHORIZED, "Token is invalid. Access Denied.");
	}

	private void onTokenMissed() {
		throw new HttpException(HttpStatus.BAD_REQUEST, "Required query parameter \"access-token\" is missed!");
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
