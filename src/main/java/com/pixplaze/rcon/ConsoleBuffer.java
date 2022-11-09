package com.pixplaze.rcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.util.ArrayList;
import java.util.List;

public class ConsoleBuffer {

    private FixedSizeQueue<String> history = new FixedSizeQueue<>();

    public void add(String line) {
        history.add(line);
    }

    public List<String> getHistory(int size) {
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
                addFilter(new LoggerRconFilter());
    }

    public int getSize() {
        return history.getMaxSize();
    }
}
