package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Testes de {@link HelperName}.
 */
class HelperNameTest {

    @Test
    void rejectsNullName() {
        assertThrows(IllegalArgumentException.class, () -> new HelperName(null));
    }

    @Test
    void rejectsEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> new HelperName(""));
    }

    @Test
    void rejectsNameWithOnlySpaces() {
        assertThrows(IllegalArgumentException.class, () -> new HelperName("   \t "));
    }

    @Test
    void trimsSpacesAtTheEnds() {
        assertEquals("Fotos", new HelperName("  Fotos  ").value());
    }

    @Test
    void keepsSpacesInTheMiddle() {
        assertEquals("Minhas fotos", new HelperName(" Minhas fotos ").value());
    }

    @Test
    void namesWithDifferentPaddingAreEqual() {
        assertEquals(new HelperName("Fotos"), new HelperName(" Fotos "));
    }
}
