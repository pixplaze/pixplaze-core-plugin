package com.pixplaze.rcon;

import com.pixplaze.plugin.PixplazeCorePlugin;
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

    @Override
    public Result filter(LogEvent event) {
        var line = "";

        var loggerFullName = event.getLoggerName().split("\\.");
        var loggerName = loggerFullName[loggerFullName.length - 1];
        if (loggerName != null) {
            line = "[" + loggerName + "] ";
        }
        line += event.getMessage().getFormattedMessage();
        consoleBuffer.add(line);

        return event == null ? Result.NEUTRAL : isLoggable(event.getMessage().getFormattedMessage());
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