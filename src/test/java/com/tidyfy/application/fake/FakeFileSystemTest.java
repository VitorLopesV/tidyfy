package com.tidyfy.application.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link FakeFileSystem}: garantem que ele segue o contrato da porta.
 */
class FakeFileSystemTest {

    private static final Path ROOT = Path.of("C:\\Downloads");
    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant MODIFIED = Instant.parse("2026-02-01T00:00:00Z");

    @Test
    void listEntriesReturnsOnlyDirectChildrenWithDates() {
        FakeFileSystem fs = new FakeFileSystem()
                .addFile(ROOT.resolve("a.txt"), "x", CREATED, MODIFIED)
                .addFolder(ROOT.resolve("Sub"))
                .addFile(ROOT.resolve("Sub").resolve("b.txt"), "y", CREATED, MODIFIED);

        List<FolderEntry> entries = fs.listEntries(ROOT);

        assertEquals(2, entries.size());
        FolderEntry file = entries.stream().filter(e -> !e.directory()).findFirst().orElseThrow();
        assertEquals(ROOT.resolve("a.txt"), file.path());
        assertEquals(CREATED, file.createdAt());
        assertEquals(MODIFIED, file.modifiedAt());
        assertTrue(entries.stream().anyMatch(e -> e.directory() && e.path().equals(ROOT.resolve("Sub"))));
    }

    @Test
    void listEntriesOfEmptyFolderIsEmpty() {
        assertTrue(new FakeFileSystem().addFolder(ROOT).listEntries(ROOT).isEmpty());
    }

    @Test
    void listEntriesOfMissingFolderFails() {
        assertThrows(UncheckedIOException.class, () -> new FakeFileSystem().listEntries(ROOT));
    }

    @Test
    void existsCoversFilesFoldersAndParents() {
        FakeFileSystem fs = new FakeFileSystem().addFile(ROOT.resolve("a.txt"), "x", CREATED, MODIFIED);

        assertTrue(fs.exists(ROOT.resolve("a.txt")));
        assertTrue(fs.exists(ROOT));
        assertFalse(fs.exists(ROOT.resolve("b.txt")));
    }

    @Test
    void sameContentComparesContents() {
        FakeFileSystem fs = new FakeFileSystem()
                .addFile(ROOT.resolve("a.txt"), "igual", CREATED, MODIFIED)
                .addFile(ROOT.resolve("b.txt"), "igual", CREATED, MODIFIED)
                .addFile(ROOT.resolve("c.txt"), "outro", CREATED, MODIFIED);

        assertTrue(fs.sameContent(ROOT.resolve("a.txt"), ROOT.resolve("b.txt")));
        assertFalse(fs.sameContent(ROOT.resolve("a.txt"), ROOT.resolve("c.txt")));
    }

    @Test
    void moveCreatesTargetFolderAndRecordsMove() {
        Path source = ROOT.resolve("a.pdf");
        Path target = ROOT.resolve("Documentos").resolve("a.pdf");
        FakeFileSystem fs = new FakeFileSystem().addFile(source, "x", CREATED, MODIFIED);

        fs.move(new FileMove(source, target));

        assertFalse(fs.exists(source));
        assertTrue(fs.exists(ROOT.resolve("Documentos")));
        assertEquals("x", fs.contentOf(target));
        assertEquals(List.of(new FileMove(source, target)), fs.moves());
    }

    @Test
    void moveKeepsDatesOfTheFile() {
        Path source = ROOT.resolve("a.pdf");
        Path target = ROOT.resolve("Docs").resolve("a.pdf");
        FakeFileSystem fs = new FakeFileSystem().addFile(source, "x", CREATED, MODIFIED);

        fs.move(new FileMove(source, target));

        FolderEntry moved = fs.listEntries(ROOT.resolve("Docs")).get(0);
        assertEquals(CREATED, moved.createdAt());
        assertEquals(MODIFIED, moved.modifiedAt());
    }

    @Test
    void moveDoesNotOverwriteOccupiedTarget() {
        Path source = ROOT.resolve("a.pdf");
        Path target = ROOT.resolve("b.pdf");
        FakeFileSystem fs = new FakeFileSystem()
                .addFile(source, "a", CREATED, MODIFIED)
                .addFile(target, "b", CREATED, MODIFIED);

        assertThrows(UncheckedIOException.class, () -> fs.move(new FileMove(source, target)));

        assertEquals("a", fs.contentOf(source));
        assertEquals("b", fs.contentOf(target));
        assertTrue(fs.moves().isEmpty());
    }

    @Test
    void moveOfMissingSourceFails() {
        FakeFileSystem fs = new FakeFileSystem().addFolder(ROOT);

        assertThrows(UncheckedIOException.class,
                () -> fs.move(new FileMove(ROOT.resolve("x"), ROOT.resolve("y"))));
    }

    @Test
    void moveOfFolderCarriesItsContents() {
        Path folder = ROOT.resolve("Fotos");
        Path target = ROOT.resolve("Pastas").resolve("Fotos");
        FakeFileSystem fs = new FakeFileSystem().addFile(folder.resolve("1.jpg"), "img", CREATED, MODIFIED);

        fs.move(new FileMove(folder, target));

        assertFalse(fs.exists(folder));
        assertEquals("img", fs.contentOf(target.resolve("1.jpg")));
    }

    @Test
    void moveToTrashRemovesItemAndRecordsIt() {
        Path file = ROOT.resolve("a.txt");
        FakeFileSystem fs = new FakeFileSystem().addFile(file, "x", CREATED, MODIFIED);

        fs.moveToTrash(file);

        assertFalse(fs.exists(file));
        assertEquals(List.of(file), fs.trashed());
    }

    @Test
    void moveToTrashOfFolderRemovesItsContents() {
        Path folder = ROOT.resolve("Sub");
        FakeFileSystem fs = new FakeFileSystem().addFile(folder.resolve("a.txt"), "x", CREATED, MODIFIED);

        fs.moveToTrash(folder);

        assertFalse(fs.exists(folder.resolve("a.txt")));
        assertTrue(fs.exists(ROOT));
    }

    @Test
    void moveToTrashOfMissingPathFails() {
        assertThrows(UncheckedIOException.class, () -> new FakeFileSystem().moveToTrash(ROOT.resolve("x")));
    }

    @Test
    void failNextWriteWithThrowsOnceAndChangesNothing() {
        Path file = ROOT.resolve("a.txt");
        FakeFileSystem fs = new FakeFileSystem().addFile(file, "x", CREATED, MODIFIED);
        fs.failNextWriteWith(new IllegalStateException("sem permissão"));

        assertThrows(IllegalStateException.class, () -> fs.moveToTrash(file));
        assertTrue(fs.exists(file));
        assertTrue(fs.trashed().isEmpty());

        fs.moveToTrash(file);
        assertEquals(List.of(file), fs.trashed());
    }
}
