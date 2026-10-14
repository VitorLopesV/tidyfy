package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FolderEntry}.
 */
class FolderEntryTest {

    private final Path path = Path.of("C:\\Users\\x\\Foto.jpg");
    private final Instant created = Instant.parse("2026-01-01T10:00:00Z");
    private final Instant modified = Instant.parse("2026-02-01T10:00:00Z");

    @Test
    void keepsAllGivenValues() {
        FolderEntry entry = new FolderEntry(path, false, created, modified);

        assertEquals(path, entry.path());
        assertFalse(entry.directory());
        assertEquals(created, entry.createdAt());
        assertEquals(modified, entry.modifiedAt());
    }

    @Test
    void canRepresentADirectory() {
        assertTrue(new FolderEntry(path, true, created, modified).directory());
    }

    @Test
    void entriesWithSameValuesAreEqual() {
        assertEquals(new FolderEntry(path, false, created, modified), new FolderEntry(path, false, created, modified));
    }

    @Test
    void rejectsNullPath() {
        assertThrows(NullPointerException.class, () -> new FolderEntry(null, false, created, modified));
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThrows(NullPointerException.class, () -> new FolderEntry(path, false, null, modified));
    }

    @Test
    void rejectsNullModifiedAt() {
        assertThrows(NullPointerException.class, () -> new FolderEntry(path, false, created, null));
    }
}
