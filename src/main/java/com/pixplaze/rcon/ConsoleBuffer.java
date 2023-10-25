package com.pixplaze.rcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ConsoleBuffer {

    private final FixedSizeQueue<String> history = new FixedSizeQueue<>();
    private final List<Consumer<String>> logEventHandlers = new ArrayList<>();

    public void add(String line) {
        history.add(line);
        logEventHandlers.forEach(handler -> handler.accept(line));
    }

    public void addLogEventHandler(Consumer<String> logEventHandler) {
        logEventHandlers.add(logEventHandler);
    }

    public List<String> getHistory() {
        return this.getHistory(this.history.size());
    }

    public List<String> getHistory(int size) {
        if (size < 0)
            size = history.size() + size;

        if (size >= history.size()) {
            return new ArrayList<>(history);
        }

        List<String> result = new ArrayList<>();
        for (int i = history.size() - size; i < history.size(); i++) {
            result.add(history.get(i));
        }

        return result;
    }

    public void attachLogger() {
        ((LoggerContext) LogManager.
                getContext(false)).getConfiguration().
                getLoggerConfig(LogManager.ROOT_LOGGER_NAME).
                addFilter(new LoggerRconFilter(this));
    }

    public int getSize() {
        return history.getMaxSize();
    }
}
