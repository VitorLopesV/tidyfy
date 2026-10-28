package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tidyfy.domain.DesktopLayout;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Testa os textos dos layouts da área de trabalho.
 */
class DesktopLayoutTextTest {

    @Test
    void everyLayoutHasALabelAndADescription() {
        for (DesktopLayout layout : DesktopLayout.values()) {
            assertFalse(DesktopLayoutText.label(layout).isBlank());
            assertFalse(DesktopLayoutText.description(layout).isBlank());
        }
    }

    @Test
    void labelsAreDistinct() {
        Set<String> labels = new HashSet<>();
        Arrays.stream(DesktopLayout.values()).forEach(layout -> labels.add(DesktopLayoutText.label(layout)));

        assertEquals(DesktopLayout.values().length, labels.size());
    }

    @Test
    void labelsAreInPortuguese() {
        assertEquals("Esquerda", DesktopLayoutText.label(DesktopLayout.LEFT));
        assertEquals("Por tipo", DesktopLayoutText.label(DesktopLayout.BY_TYPE_LEFT));
    }

    @Test
    void nullLayoutIsRejected() {
        assertThrows(NullPointerException.class, () -> DesktopLayoutText.label(null));
        assertThrows(NullPointerException.class, () -> DesktopLayoutText.description(null));
    }
}
