package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FileCategory}.
 */
class FileCategoryTest {

    private final FileCategory images = new FileCategory("Imagens", "Imagens",
        Set.of(new FileExtension("jpg"), new FileExtension("png")));

    @Test
    void matchesFileWithUppercaseExtension() {
        assertTrue(images.matches(Path.of("C:\\Users\\x\\Foto.JPG")));
    }

    @Test
    void doesNotMatchFileWithoutExtension() {
        assertFalse(images.matches(Path.of("Makefile")));
    }

    @Test
    void doesNotMatchUnknownExtension() {
        assertFalse(images.matches(Path.of("musica.mp3")));
    }

    @Test
    void doesNotMatchHiddenFileWithoutExtension() {
        assertFalse(images.matches(Path.of(".jpg")));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class,
            () -> new FileCategory(" ", "Imagens", Set.of(new FileExtension("jpg"))));
    }

    @Test
    void rejectsBlankTargetFolder() {
        assertThrows(IllegalArgumentException.class,
            () -> new FileCategory("Imagens", "", Set.of(new FileExtension("jpg"))));
    }

    @Test
    void rejectsEmptyExtensions() {
        assertThrows(IllegalArgumentException.class, () -> new FileCategory("Imagens", "Imagens", Set.of()));
    }

    @Test
    void keepsExtensionsImmutable() {
        assertThrows(UnsupportedOperationException.class,
            () -> images.extensions().add(new FileExtension("gif")));
    }
}
