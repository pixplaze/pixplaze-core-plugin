package com.pixplaze.rcon;

import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

public class LoggerRconFilter extends AbstractFilter {
    private final ConsoleBuffer consoleBuffer;

    public LoggerRconFilter(ConsoleBuffer consoleBuffer) {
        this.consoleBuffer = consoleBuffer;
    }

    /// Строка лога как есть: автор — короткое имя логгера (последний сегмент), текст — сообщение
    /// с кодами форматирования, время — момент события log4j.
    @Override
    public Result filter(LogEvent event) {
        final var loggerName = event.getLoggerName();
        final var author = loggerName == null || loggerName.isEmpty()
                ? null
                : loggerName.substring(loggerName.lastIndexOf('.') + 1);
        consoleBuffer.add(new MinecraftMessageInfo(
                MinecraftMessageInfo.Type.CONSOLE,
                author,
                null,
                event.getMessage().getFormattedMessage(),
                event.getTimeMillis()));

        return isLoggable(event.getMessage().getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t) {
        msg.getFormattedMessage();
        return isLoggable(msg.getFormattedMessage());
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, String msg, Object... params) {
        return isLoggable(msg);
    }

    @Override
    public Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t) {
        return msg == null ? Result.NEUTRAL : isLoggable(msg.toString());
    }

    private Result isLoggable(String msg) {
        return Result.NEUTRAL;
    }
}