package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Testes de {@link DesktopIcon}, {@link IconPosition} e {@link DesktopGrid}.
 */
class DesktopIconTest {

    @Test
    void keepsNameAndPosition() {
        DesktopIcon icon = new DesktopIcon("Programação", new IconPosition(32, 125));

        assertEquals("Programação", icon.name());
        assertEquals(new IconPosition(32, 125), icon.position());
    }

    @Test
    void rejectsNullNameAndPosition() {
        assertThrows(NullPointerException.class, () -> new DesktopIcon(null, new IconPosition(0, 0)));
        assertThrows(NullPointerException.class, () -> new DesktopIcon("A", null));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopIcon("  ", new IconPosition(0, 0)));
    }

    @Test
    void gridKeepsTheMeasures() {
        DesktopGrid grid = new DesktopGrid(0, 0, 1920, 1008, 112, 123);

        assertEquals(1920, grid.width());
        assertEquals(1008, grid.height());
        assertEquals(112, grid.cellWidth());
        assertEquals(123, grid.cellHeight());
    }

    @Test
    void gridAcceptsAnOffsetForTheMainMonitor() {
        DesktopGrid grid = new DesktopGrid(1920, 0, 1920, 1080, 112, 123);

        assertEquals(1920, grid.left());
        assertEquals(0, grid.top());
    }

    @Test
    void gridRejectsNonPositiveMeasures() {
        assertThrows(IllegalArgumentException.class, () -> new DesktopGrid(0, 0, 0, 100, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> new DesktopGrid(0, 0, 100, 0, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> new DesktopGrid(0, 0, 100, 100, 0, 10));
        assertThrows(IllegalArgumentException.class, () -> new DesktopGrid(0, 0, 100, 100, 10, 0));
    }
}
