package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link DesktopArrangerHelper} e {@link DesktopLayout}.
 */
class DesktopArrangerHelperTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static DesktopArrangerHelper helper(DesktopLayout layout) {
        return new DesktopArrangerHelper(HelperId.newId(), new HelperName("Área"), SCHEDULE, true, layout);
    }

    @Test
    void typeIsDesktopArranger() {
        assertEquals(HelperType.DESKTOP_ARRANGER, helper(DesktopLayout.LEFT).type());
    }

    @Test
    void keepsTheDataAndIsAHelper() {
        Helper helper = helper(DesktopLayout.BORDERS);

        assertEquals(new HelperName("Área"), helper.name());
        assertEquals(SCHEDULE, helper.schedule());
        assertTrue(helper.enabled());
        assertEquals(DesktopLayout.BORDERS, ((DesktopArrangerHelper) helper).layout());
    }

    @Test
    void hasTheSixLayouts() {
        assertEquals(List.of(DesktopLayout.LEFT, DesktopLayout.RIGHT, DesktopLayout.TOP, DesktopLayout.BOTTOM,
                DesktopLayout.BORDERS, DesktopLayout.BY_TYPE_LEFT), List.of(DesktopLayout.values()));
    }

    @Test
    void rejectsNullLayout() {
        assertThrows(NullPointerException.class, () -> helper(null));
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new DesktopArrangerHelper(null, new HelperName("A"), SCHEDULE, true, DesktopLayout.TOP));
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class,
                () -> new DesktopArrangerHelper(HelperId.newId(), null, SCHEDULE, true, DesktopLayout.TOP));
    }

    @Test
    void rejectsNullSchedule() {
        assertThrows(NullPointerException.class,
                () -> new DesktopArrangerHelper(HelperId.newId(), new HelperName("A"), null, true, DesktopLayout.TOP));
    }

    @Test
    void withEnabledKeepsEverythingElse() {
        DesktopArrangerHelper original = helper(DesktopLayout.BOTTOM);

        DesktopArrangerHelper off = original.withEnabled(false);

        assertFalse(off.enabled());
        assertEquals(original.id(), off.id());
        assertEquals(original.name(), off.name());
        assertEquals(original.schedule(), off.schedule());
        assertEquals(DesktopLayout.BOTTOM, off.layout());
    }
}
