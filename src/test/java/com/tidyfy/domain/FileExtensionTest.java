package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FileExtension}.
 */
class FileExtensionTest {

    @Test
    void returnsLowercaseExtensionWithoutDot() {
        assertEquals(Optional.of(new FileExtension("jpg")), FileExtension.of("Foto.JPG"));
        assertEquals("jpg", FileExtension.of("Foto.JPG").orElseThrow().value());
    }

    @Test
    void returnsEmptyWhenFileHasNoExtension() {
        assertTrue(FileExtension.of("Makefile").isEmpty());
    }

    @Test
    void usesOnlyTheLastExtensionWhenThereAreSeveral() {
        assertEquals("gz", FileExtension.of("a.tar.gz").orElseThrow().value());
    }

    @Test
    void returnsEmptyWhenNameEndsWithDot() {
        assertTrue(FileExtension.of("arquivo.").isEmpty());
    }

    @Test
    void returnsEmptyForHiddenFileWithoutExtension() {
        assertTrue(FileExtension.of(".gitignore").isEmpty());
    }

    @Test
    void returnsExtensionOfHiddenFileThatHasOne() {
        assertEquals("txt", FileExtension.of(".notas.txt").orElseThrow().value());
    }

    @Test
    void returnsEmptyForNullName() {
        assertTrue(FileExtension.of(null).isEmpty());
    }

    @Test
    void returnsEmptyForEmptyOrBlankName() {
        assertTrue(FileExtension.of("").isEmpty());
        assertTrue(FileExtension.of("   ").isEmpty());
    }

    @Test
    void ignoresSpacesAroundTheName() {
        assertEquals("pdf", FileExtension.of("  relatorio.PDF  ").orElseThrow().value());
    }

    @Test
    void returnsEmptyWhenOnlySpacesFollowTheDot() {
        assertTrue(FileExtension.of("arquivo.   ").isEmpty());
    }

    @Test
    void normalizesValueGivenToConstructor() {
        assertEquals("jpg", new FileExtension(" .JPG ").value());
    }

    @Test
    void rejectsNullOrEmptyValueInConstructor() {
        assertThrows(IllegalArgumentException.class, () -> new FileExtension(null));
        assertThrows(IllegalArgumentException.class, () -> new FileExtension(""));
        assertThrows(IllegalArgumentException.class, () -> new FileExtension("."));
        assertThrows(IllegalArgumentException.class, () -> new FileExtension("   "));
    }

    @Test
    void extensionsWithDifferentCaseAreEqual() {
        assertEquals(new FileExtension("PNG"), new FileExtension("png"));
    }
}
