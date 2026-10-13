package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa o modelo de tela do ajudante.
 */
class HelperItemTest {

    @Test
    void keepsTimesSorted() {
        HelperItem item = item(Path.of("C:\\Pasta"), List.of(LocalTime.of(18, 0), LocalTime.of(9, 0)));

        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)), item.times());
    }

    @Test
    void targetsTheRecycleBinWhenThereIsNoFolder() {
        assertTrue(item(null, List.of(LocalTime.NOON)).targetsRecycleBin());
        assertFalse(item(Path.of("C:\\Pasta"), List.of(LocalTime.NOON)).targetsRecycleBin());
    }

    @Test
    void withEnabledChangesOnlyTheEnabledFlag() {
        HelperItem original = item(Path.of("C:\\Pasta"), List.of(LocalTime.NOON));

        HelperItem paused = original.withEnabled(false);

        assertFalse(paused.enabled());
        assertEquals(original.id(), paused.id());
        assertEquals(original.name(), paused.name());
        assertEquals(original.folder(), paused.folder());
        assertEquals(original.times(), paused.times());
    }

    @Test
    void requiresIdNameAndKind() {
        List<LocalTime> times = List.of(LocalTime.NOON);
        assertThrows(NullPointerException.class,
                () -> new HelperItem(null, "Nome", HelperKind.CLEANER, null, 1, times, true, null, 0));
        assertThrows(NullPointerException.class,
                () -> new HelperItem("id", null, HelperKind.CLEANER, null, 1, times, true, null, 0));
        assertThrows(NullPointerException.class,
                () -> new HelperItem("id", "Nome", null, null, 1, times, true, null, 0));
    }

    private static HelperItem item(Path folder, List<LocalTime> times) {
        return new HelperItem("id", "Organizar Downloads", HelperKind.ORGANIZER, folder, 1, times, true, "Hoje, 18:00", 0.5);
    }
}
