package com.pixplaze.message;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageHistoryTest {

    private static MessageHistory<String> filled(int capacity, int lines) {
        final var history = new MessageHistory<String>(capacity);
        for (int i = 1; i <= lines; i++) {
            history.add("line " + i);
        }

        return history;
    }

    @Test
    @DisplayName("Ёмкость должна быть положительной")
    void rejectsNonPositiveCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new MessageHistory<String>(0));
        assertThrows(IllegalArgumentException.class, () -> new MessageHistory<String>(-1));
    }

    @Test
    @DisplayName("Пустая история отдаёт пустые снимки")
    void emptyHistory() {
        final var history = new MessageHistory<String>(4);

        assertEquals(0, history.size());
        assertEquals(List.of(), history.snapshot());
        assertEquals(List.of(), history.snapshot(10));
        assertEquals(List.of(), history.snapshot(0));
        assertEquals(List.of(), history.snapshot(-3));
    }

    @Test
    @DisplayName("Строки хранятся в порядке добавления, пока есть место")
    void keepsInsertionOrder() {
        final var history = filled(4, 3);

        assertEquals(List.of("line 1", "line 2", "line 3"), history.snapshot());
        assertEquals(3, history.size());
    }

    @Test
    @DisplayName("При переполнении вытесняются самые старые строки")
    void evictsOldestOnOverflow() {
        final var history = filled(3, 5);

        assertEquals(List.of("line 3", "line 4", "line 5"), history.snapshot());
        assertEquals(3, history.size());
        assertEquals(3, history.capacity());
    }

    @Test
    @DisplayName("Положительный count отдаёт последние строки")
    void positiveCountReturnsTail() {
        final var history = filled(10, 5);

        assertEquals(List.of("line 4", "line 5"), history.snapshot(2));
        assertEquals(List.of("line 5"), history.snapshot(1));
    }

    @Test
    @DisplayName("count больше объёма истории отдаёт её целиком")
    void countGreaterThanSizeReturnsAll() {
        final var history = filled(10, 3);

        assertEquals(List.of("line 1", "line 2", "line 3"), history.snapshot(99));
    }

    @Test
    @DisplayName("Нулевой count отдаёт пустой список")
    void zeroCountReturnsEmpty() {
        final var history = filled(10, 3);

        assertEquals(List.of(), history.snapshot(0));
    }

    @Test
    @DisplayName("Отрицательный count отбрасывает самые старые строки")
    void negativeCountDropsOldest() {
        final var history = filled(10, 5);

        assertEquals(List.of("line 3", "line 4", "line 5"), history.snapshot(-2));
        // |count| больше объёма истории — отбрасывать нечего, остаётся пустой список
        assertEquals(List.of(), history.snapshot(-99));
    }

    @Test
    @DisplayName("Снимок — копия: его правка не трогает историю")
    void snapshotIsDetachedCopy() {
        final var history = filled(10, 2);

        final var snapshot = history.snapshot();
        snapshot.clear();

        assertEquals(List.of("line 1", "line 2"), history.snapshot());
        assertTrue(snapshot.isEmpty());
    }

    @Test
    @DisplayName("Размер никогда не превышает ёмкость")
    void sizeNeverExceedsCapacity() {
        final var history = filled(8, 1000);

        assertEquals(8, history.size());
        assertEquals(List.of("line 993", "line 994", "line 995", "line 996",
                "line 997", "line 998", "line 999", "line 1000"), history.snapshot());
    }
}
