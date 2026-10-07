package com.pixplaze.api.state;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerStateSnapshotTest {

    @Test
    @DisplayName("Один игрок — его собственный пинг")
    void singleValue() {
        assertEquals(42L, ServerStateSnapshot.median(new int[]{42}));
    }

    @Test
    @DisplayName("Нечётное количество — среднее значение по порядку")
    void oddCount() {
        assertEquals(30L, ServerStateSnapshot.median(new int[]{10, 30, 900}));
    }

    @Test
    @DisplayName("Чётное количество — среднее двух серединных")
    void evenCount() {
        assertEquals(35L, ServerStateSnapshot.median(new int[]{10, 30, 40, 900}));
    }

    @Test
    @DisplayName("Чётное количество с дробной серединой округляется")
    void evenCountRounds() {
        // середина между 30 и 41 — это 35.5
        assertEquals(36L, ServerStateSnapshot.median(new int[]{10, 30, 41, 900}));
    }

    @Test
    @DisplayName("Выброс не портит показатель, в отличие от среднего")
    void outlierDoesNotSkew() {
        final var pings = new int[]{28, 30, 31, 33, 2000};

        // среднее было бы 424 мс
        assertEquals(31L, ServerStateSnapshot.median(pings));
    }
}
