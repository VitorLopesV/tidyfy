package com.tidyfy.infrastructure.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FileMove;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de existência, comparação de conteúdo e movimentação do {@link NioFileSystem}.
 */
class NioFileSystemMoveTest {

    @TempDir
    Path folder;

    private final NioFileSystem fileSystem = new NioFileSystem();

    @Test
    void movesFileCreatingTargetFolder() throws IOException {
        Path source = Files.writeString(folder.resolve("a.txt"), "conteudo");
        Path target = folder.resolve("Documentos").resolve("a.txt");

        fileSystem.move(new FileMove(source, target));

        assertFalse(Files.exists(source));
        assertEquals("conteudo", Files.readString(target));
    }

    @Test
    void movesWholeFolderWithContent() throws IOException {
        Path source = Files.createDirectories(folder.resolve("pasta").resolve("interna"));
        Files.writeString(source.resolve("um.txt"), "1");
        Files.writeString(source.getParent().resolve("dois.txt"), "2");
        Path target = folder.resolve("Pastas").resolve("pasta");

        fileSystem.move(new FileMove(source.getParent(), target));

        assertFalse(Files.exists(folder.resolve("pasta")));
        assertEquals("1", Files.readString(target.resolve("interna").resolve("um.txt")));
        assertEquals("2", Files.readString(target.resolve("dois.txt")));
    }

    @Test
    void doesNotOverwriteExistingTarget() throws IOException {
        Path source = Files.writeString(folder.resolve("a.txt"), "novo");
        Path target = Files.writeString(folder.resolve("b.txt"), "antigo");

        UncheckedIOException error = assertThrows(UncheckedIOException.class,
                () -> fileSystem.move(new FileMove(source, target)));

        assertInstanceOf(FileAlreadyExistsException.class, error.getCause());
        assertEquals("novo", Files.readString(source));
        assertEquals("antigo", Files.readString(target));
    }

    @Test
    void failsWhenSourceDoesNotExist() {
        Path source = folder.resolve("nao-existe.txt");
        Path target = folder.resolve("destino").resolve("nao-existe.txt");

        UncheckedIOException error = assertThrows(UncheckedIOException.class,
                () -> fileSystem.move(new FileMove(source, target)));

        assertInstanceOf(NoSuchFileException.class, error.getCause());
        assertFalse(Files.exists(target));
    }

    @Test
    void existsForFileFolderAndMissing() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "x");
        Path sub = Files.createDirectory(folder.resolve("sub"));

        assertTrue(fileSystem.exists(file));
        assertTrue(fileSystem.exists(sub));
        assertFalse(fileSystem.exists(folder.resolve("nada")));
    }

    @Test
    void sameContentForIdenticalFiles() throws IOException {
        Path first = Files.writeString(folder.resolve("a.txt"), "igual");
        Path second = Files.writeString(folder.resolve("b.txt"), "igual");

        assertTrue(fileSystem.sameContent(first, second));
    }

    @Test
    void sameContentFalseForSameSizeDifferentContent() throws IOException {
        Path first = Files.writeString(folder.resolve("a.txt"), "abcde");
        Path second = Files.writeString(folder.resolve("b.txt"), "abcdf");

        assertFalse(fileSystem.sameContent(first, second));
    }

    @Test
    void sameContentFalseForDifferentSizes() throws IOException {
        Path first = Files.writeString(folder.resolve("a.txt"), "abc");
        Path second = Files.writeString(folder.resolve("b.txt"), "abcd");

        assertFalse(fileSystem.sameContent(first, second));
    }

    @Test
    void sameContentTrueForTwoEmptyFiles() throws IOException {
        Path first = Files.createFile(folder.resolve("a.txt"));
        Path second = Files.createFile(folder.resolve("b.txt"));

        assertTrue(fileSystem.sameContent(first, second));
    }

    @Test
    void sameContentFailsForMissingPath() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "x");
        Path missing = folder.resolve("nada.txt");

        assertThrows(UncheckedIOException.class, () -> fileSystem.sameContent(file, missing));
        assertThrows(UncheckedIOException.class, () -> fileSystem.sameContent(missing, file));
    }

    @Test
    void sameContentFailsForFolder() throws IOException {
        Path file = Files.writeString(folder.resolve("a.txt"), "x");
        Path sub = Files.createDirectory(folder.resolve("sub"));

        assertThrows(UncheckedIOException.class, () -> fileSystem.sameContent(file, sub));
    }
}
