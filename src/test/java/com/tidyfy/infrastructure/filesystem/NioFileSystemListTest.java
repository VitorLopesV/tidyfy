package com.tidyfy.infrastructure.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FolderEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes da listagem de pasta do {@link NioFileSystem}.
 */
class NioFileSystemListTest {

    @TempDir
    Path folder;

    private final NioFileSystem fileSystem = new NioFileSystem();

    @Test
    void listsFilesAndFoldersWithDates() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "x");
        Path sub = Files.createDirectory(folder.resolve("b"));
        Instant modified = Instant.parse("2024-03-05T10:15:30Z");
        Files.setLastModifiedTime(file, FileTime.from(modified));

        List<FolderEntry> entries = fileSystem.listEntries(folder);

        assertEquals(2, entries.size());
        FolderEntry fileEntry = entries.get(0);
        assertEquals(file, fileEntry.path());
        assertFalse(fileEntry.directory());
        assertEquals(modified, fileEntry.modifiedAt());
        assertTrue(Duration.between(fileEntry.createdAt(), Instant.now()).abs().compareTo(Duration.ofMinutes(5)) < 0);
        FolderEntry folderEntry = entries.get(1);
        assertEquals(sub, folderEntry.path());
        assertTrue(folderEntry.directory());
    }

    @Test
    void doesNotEnterSubfolders() throws IOException {
        Path sub = Files.createDirectory(folder.resolve("sub"));
        Files.writeString(sub.resolve("inner.txt"), "x");

        List<FolderEntry> entries = fileSystem.listEntries(folder);

        assertEquals(1, entries.size());
        assertEquals(sub, entries.get(0).path());
    }

    @Test
    void returnsEmptyListForEmptyFolder() {
        assertTrue(fileSystem.listEntries(folder).isEmpty());
    }

    @Test
    void throwsWhenFolderDoesNotExist() {
        assertThrows(UncheckedIOException.class, () -> fileSystem.listEntries(folder.resolve("missing")));
    }

    @Test
    void throwsWhenPathIsAFile() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "x");

        assertThrows(UncheckedIOException.class, () -> fileSystem.listEntries(file));
    }

    @Test
    void ordersEntriesByNameIgnoringCase() throws IOException {
        Files.writeString(folder.resolve("c.txt"), "x");
        Files.writeString(folder.resolve("B.txt"), "x");
        Files.createDirectory(folder.resolve("a"));

        List<String> names = fileSystem.listEntries(folder).stream()
                .map(entry -> entry.path().getFileName().toString()).toList();

        assertEquals(List.of("a", "B.txt", "c.txt"), names);
    }
}
