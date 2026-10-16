package com.pixplaze.message;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/// Кольцевой буфер сообщений (строк консоли, сообщений чата) фиксированной ёмкости: при
/// переполнении вытесняется самое старое. Внутри {@link ArrayDeque} — он сам устроен как кольцевой буфер, поэтому
/// добавление с вытеснением стоит O(1), а ручной арифметики с индексом головы не требуется.
///
/// Пишет сюда log4j из своих потоков, читают потоки Javalin, поэтому класс синхронизирован
/// внутри: снимок собирается целиком под замком и наружу отдаётся копия, которую можно
/// обходить без блокировок. Внешний замок владельцу не нужен.
public final class MessageHistory<T> {

    private final ArrayDeque<T> lines;
    private final int capacity;

    public MessageHistory(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException(
                    "Message history capacity must be positive, got %d".formatted(capacity));
        }

        this.capacity = capacity;
        this.lines = new ArrayDeque<>(capacity);
    }

    public synchronized void add(T line) {
        if (lines.size() >= capacity) {
            lines.pollFirst();
        }

        lines.addLast(line);
    }

    /// Вся история, от самой старой строки к самой новой.
    public synchronized List<T> snapshot() {
        return new ArrayList<>(lines);
    }

    /// Хвост истории:
    /// {@code count > 0} — последние {@code count} строк;
    /// {@code count < 0} — вся история, кроме {@code |count|} самых старых строк;
    /// {@code count == 0} — пустой список.
    /// Значение больше объёма истории отдаёт её целиком.
    public synchronized List<T> snapshot(int count) {
        final var all = new ArrayList<>(lines);

        if (count < 0) {
            count = all.size() + count;
        }

        if (count >= all.size()) {
            return all;
        }

        if (count <= 0) {
            return new ArrayList<>();
        }

        return new ArrayList<>(all.subList(all.size() - count, all.size()));
    }

    public synchronized int size() {
        return lines.size();
    }

    public int capacity() {
        return capacity;
    }
}
