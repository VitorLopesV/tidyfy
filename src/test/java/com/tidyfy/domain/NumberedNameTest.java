package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link NumberedName}.
 */
class NumberedNameTest {

    @Test
    void keepsNameWhenFree() {
        assertEquals("teste.pdf", NumberedName.firstFree("teste.pdf", name -> false));
    }

    @Test
    void numbersBeforeExtensionWhenTaken() {
        assertEquals("teste(1).pdf", NumberedName.firstFree("teste.pdf", Set.of("teste.pdf")::contains));
    }

    @Test
    void climbsToFirstFreeNumber() {
        Set<String> taken = Set.of("teste.pdf", "teste(1).pdf", "teste(2).pdf");
        assertEquals("teste(3).pdf", NumberedName.firstFree("teste.pdf", taken::contains));
    }

    @Test
    void reusesGapInNumbering() {
        Set<String> taken = Set.of("teste.pdf", "teste(2).pdf");
        assertEquals("teste(1).pdf", NumberedName.firstFree("teste.pdf", taken::contains));
    }

    @Test
    void numbersFolderWithoutExtension() {
        assertEquals("Fotos(1)", NumberedName.firstFree("Fotos", Set.of("Fotos")::contains));
    }

    @Test
    void numbersBeforeLastExtensionWhenNameHasManyDots() {
        assertEquals("a.tar(1).gz", NumberedName.firstFree("a.tar.gz", Set.of("a.tar.gz")::contains));
    }

    @Test
    void treatsHiddenFileAsWithoutExtension() {
        assertEquals(".gitignore(1)", NumberedName.firstFree(".gitignore", Set.of(".gitignore")::contains));
    }

    @Test
    void treatsTrailingDotAsWithoutExtension() {
        assertEquals("nome.(1)", NumberedName.firstFree("nome.", Set.of("nome.")::contains));
    }

    @Test
    void rejectsNullOrEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> NumberedName.firstFree(null, name -> false));
        assertThrows(IllegalArgumentException.class, () -> NumberedName.firstFree("", name -> false));
    }

    @Test
    void rejectsNullExistsTest() {
        assertThrows(IllegalArgumentException.class, () -> NumberedName.firstFree("a.txt", null));
    }
}
