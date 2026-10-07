package com.pixplaze.rcon;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ConsoleBuffer {

    private static final int HISTORY_CAPACITY = 256;

    private final ConsoleHistory history = new ConsoleHistory(HISTORY_CAPACITY);

    /// Обработчики вызывает log4j из своих потоков, а регистрируют их websocket-соединения,
    /// поэтому список должен переживать конкурентную модификацию.
    private final List<Consumer<String>> logEventHandlers = new CopyOnWriteArrayList<>();

    /// Фильтр, отданный log4j. Ссылка нужна, чтобы снять его в {@link #detachLogger()}:
    /// конфигурация log4j принадлежит серверу и живёт дольше плагина, а оставленный в ней
    /// фильтр держит classloader выгруженного плагина (ошибка `zip file closed`).
    private LoggerRconFilter attachedFilter;

    public void add(String line) {
        history.add(line);
        logEventHandlers.forEach(handler -> handler.accept(line));
    }

    public void addLogEventHandler(Consumer<String> logEventHandler) {
        logEventHandlers.add(logEventHandler);
    }

    public List<String> getHistory() {
        return history.snapshot();
    }

    /// Хвост истории, см. {@link ConsoleHistory#snapshot(int)}.
    public List<String> getHistory(int size) {
        return history.snapshot(size);
    }

    public void attachLogger() {
        if (attachedFilter != null) {
            return;
        }

        attachedFilter = new LoggerRconFilter(this);
        rootLoggerConfig().addFilter(attachedFilter);
    }

    /// Снимает фильтр с root-логгера и отпускает обработчики. Обязателен в
    /// {@code onDisable()}: иначе после выгрузки плагина log4j продолжит вызывать
    /// {@link LoggerRconFilter} на каждую строку лога, тот полезет за классами в уже
    /// закрытый jar, и сервер начнёт сыпать `IllegalStateException: zip file closed`.
    public void detachLogger() {
        if (attachedFilter != null) {
            rootLoggerConfig().removeFilter(attachedFilter);
            attachedFilter = null;
        }

        logEventHandlers.clear();
    }

    private static LoggerConfig rootLoggerConfig() {
        return ((LoggerContext) LogManager.getContext(false))
                .getConfiguration()
                .getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    }

    /// Ёмкость истории, а не текущее число строк — имя сохранено ради совместимости
    /// с прежним API.
    public int getSize() {
        return history.capacity();
    }
}
