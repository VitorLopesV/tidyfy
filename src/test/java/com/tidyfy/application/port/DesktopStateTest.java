package com.tidyfy.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.IconPosition;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link DesktopState}.
 */
class DesktopStateTest {

    private static final DesktopGrid GRID = new DesktopGrid(0, 0, 1920, 1008, 112, 123);

    @Test
    void keepsTheData() {
        DesktopIcon icon = new DesktopIcon("Steam", new IconPosition(32, 248));

        DesktopState state = new DesktopState(List.of(icon), GRID, true, false);

        assertEquals(List.of(icon), state.icons());
        assertEquals(GRID, state.grid());
        assertTrue(state.autoArrange());
        assertFalse(state.snapToGrid());
    }

    @Test
    void copiesTheIconListSoLaterChangesDoNotLeakIn() {
        List<DesktopIcon> icons = new ArrayList<>();
        icons.add(new DesktopIcon("A", new IconPosition(0, 0)));
        DesktopState state = new DesktopState(icons, GRID, false, false);

        icons.add(new DesktopIcon("B", new IconPosition(1, 1)));

        assertEquals(1, state.icons().size());
        assertThrows(UnsupportedOperationException.class,
                () -> state.icons().add(new DesktopIcon("C", new IconPosition(2, 2))));
    }

    @Test
    void rejectsNullIconsAndGrid() {
        assertThrows(NullPointerException.class, () -> new DesktopState(null, GRID, false, false));
        assertThrows(NullPointerException.class, () -> new DesktopState(List.of(), null, false, false));
    }
}
