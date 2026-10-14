package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link FileMove}.
 */
class FileMoveTest {

    private final Path source = Path.of("C:\\Users\\x\\Foto.jpg");
    private final Path target = Path.of("C:\\Users\\x\\Imagens\\Foto.jpg");

    @Test
    void keepsSourceAndTarget() {
        FileMove move = new FileMove(source, target);

        assertEquals(source, move.source());
        assertEquals(target, move.target());
    }

    @Test
    void movesWithSameValuesAreEqual() {
        assertEquals(new FileMove(source, target), new FileMove(source, target));
    }

    @Test
    void acceptsSameSourceAndTarget() {
        assertEquals(source, new FileMove(source, source).target());
    }

    @Test
    void rejectsNullSource() {
        assertThrows(NullPointerException.class, () -> new FileMove(null, target));
    }

    @Test
    void rejectsNullTarget() {
        assertThrows(NullPointerException.class, () -> new FileMove(source, null));
    }
}
