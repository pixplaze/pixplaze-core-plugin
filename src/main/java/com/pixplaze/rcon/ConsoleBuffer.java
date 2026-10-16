package com.pixplaze.rcon;

import com.pixplaze.message.MessageBuffer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

/// Строки лога сервера: источник — {@link LoggerRconFilter} на root-логгере log4j.
public class ConsoleBuffer extends MessageBuffer {

    private static final int HISTORY_CAPACITY = 256;

    /// Фильтр, отданный log4j. Ссылка нужна, чтобы снять его в {@link #detach()}:
    /// конфигурация log4j принадлежит серверу и живёт дольше плагина, а оставленный в ней
    /// фильтр держит classloader выгруженного плагина (ошибка `zip file closed`).
    private LoggerRconFilter attachedFilter;

    public ConsoleBuffer() {
        super(HISTORY_CAPACITY);
    }

    @Override
    public void attach() {
        if (attachedFilter != null) {
            return;
        }

        attachedFilter = new LoggerRconFilter(this);
        rootLoggerConfig().addFilter(attachedFilter);
    }

    /// Снимает фильтр с root-логгера и отпускает подписчиков. Обязателен в
    /// {@code onDisable()}: иначе после выгрузки плагина log4j продолжит вызывать
    /// {@link LoggerRconFilter} на каждую строку лога, тот полезет за классами в уже
    /// закрытый jar, и сервер начнёт сыпать `IllegalStateException: zip file closed`.
    @Override
    public void detach() {
        if (attachedFilter != null) {
            rootLoggerConfig().removeFilter(attachedFilter);
            attachedFilter = null;
        }

        super.detach();
    }

    private static LoggerConfig rootLoggerConfig() {
        return ((LoggerContext) LogManager.getContext(false))
                .getConfiguration()
                .getLoggerConfig(LogManager.ROOT_LOGGER_NAME);
    }
}
