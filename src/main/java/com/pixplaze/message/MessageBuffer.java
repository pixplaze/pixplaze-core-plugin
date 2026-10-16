package com.pixplaze.message;

import com.pixplaze.api.ext.data.message.MinecraftMessageInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/// Поток сообщений сервера с историей: источник (лог, события Bukkit) кладёт сообщения через
/// {@link #add}, подписчики (websocket-контроллеры) получают каждое новое, а подключившийся позже
/// забирает историю через {@link #getHistory()}.
///
/// Источник подключается в {@link #attach()} и обязательно отключается в {@link #detach()}
/// из {@code onDisable()}: он принадлежит серверу и живёт дольше плагина.
public abstract class MessageBuffer {

    private final MessageHistory<MinecraftMessageInfo> history;

    /// Сообщения приходят из потоков источника (log4j, асинхронный чат), а подписываются
    /// websocket-контроллеры, поэтому список должен переживать конкурентную модификацию.
    private final List<Consumer<MinecraftMessageInfo>> handlers = new CopyOnWriteArrayList<>();

    protected MessageBuffer(int capacity) {
        this.history = new MessageHistory<>(capacity);
    }

    /// Подключает буфер к источнику сообщений. Повторный вызов ничего не делает.
    public abstract void attach();

    /// Отключает буфер от источника и отпускает подписчиков. Наследник, переопределяя,
    /// сначала отключает свой источник, затем зовёт {@code super.detach()}.
    public void detach() {
        handlers.clear();
    }

    /// Новое сообщение: в историю и всем подписчикам.
    public void add(MinecraftMessageInfo message) {
        history.add(message);
        handlers.forEach(handler -> handler.accept(message));
    }

    public void addHandler(Consumer<MinecraftMessageInfo> handler) {
        handlers.add(handler);
    }

    /// Вся история, от самого старого сообщения к самому новому.
    public List<MinecraftMessageInfo> getHistory() {
        return history.snapshot();
    }

    /// Хвост истории, см. {@link MessageHistory#snapshot(int)}.
    public List<MinecraftMessageInfo> getHistory(int size) {
        return history.snapshot(size);
    }
}
