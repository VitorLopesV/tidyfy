package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link HelperType}, {@link HelperLimitReachedException} e {@link DomainException}.
 */
class HelperTypeTest {

    @Test
    void limitPerTypeIsTwenty() {
        assertEquals(20, HelperType.MAX_PER_TYPE);
    }

    @Test
    void throwsWhenCountEqualsTheLimitForTypesWithManyHelpers() {
        for (HelperType type : manyHelpersTypes()) {
            assertThrows(HelperLimitReachedException.class, () -> type.assertCanCreate(20));
        }
    }

    @Test
    void throwsWhenCountIsAboveTheLimit() {
        assertThrows(HelperLimitReachedException.class, () -> HelperType.CLEANER.assertCanCreate(21));
    }

    @Test
    void doesNotThrowBelowTheLimitForTypesWithManyHelpers() {
        for (HelperType type : manyHelpersTypes()) {
            assertDoesNotThrow(() -> type.assertCanCreate(0));
            assertDoesNotThrow(() -> type.assertCanCreate(19));
        }
    }

    @Test
    void desktopArrangerAllowsTheFirstOne() {
        assertDoesNotThrow(() -> HelperType.DESKTOP_ARRANGER.assertCanCreate(0));
    }

    @Test
    void desktopArrangerRejectsASecondOne() {
        assertThrows(DesktopArrangerAlreadyExistsException.class,
                () -> HelperType.DESKTOP_ARRANGER.assertCanCreate(1));
        assertThrows(DesktopArrangerAlreadyExistsException.class,
                () -> HelperType.DESKTOP_ARRANGER.assertCanCreate(5));
    }

    @Test
    void desktopArrangerExceptionIsADomainExceptionWithAMessage() {
        DesktopArrangerAlreadyExistsException exception = assertThrows(DesktopArrangerAlreadyExistsException.class,
                () -> HelperType.DESKTOP_ARRANGER.assertCanCreate(1));
        assertTrue(exception instanceof DomainException);
        assertTrue(exception.getMessage().contains("Organizador da área de trabalho"));
    }

    @Test
    void otherTypesStillAllowManyHelpers() {
        assertDoesNotThrow(() -> HelperType.FOLDER_ORGANIZER.assertCanCreate(1));
        assertDoesNotThrow(() -> HelperType.CLEANER.assertCanCreate(1));
    }

    private static List<HelperType> manyHelpersTypes() {
        return List.of(HelperType.FOLDER_ORGANIZER, HelperType.CLEANER);
    }

    @Test
    void limitExceptionIsADomainExceptionAndMentionsTheType() {
        HelperLimitReachedException exception = assertThrows(HelperLimitReachedException.class,
                () -> HelperType.CLEANER.assertCanCreate(20));
        assertTrue(exception instanceof DomainException);
        assertTrue(exception.getMessage().contains("CLEANER"));
        assertTrue(exception.getMessage().contains("20"));
    }

    @Test
    void domainExceptionKeepsTheMessage() {
        assertEquals("regra violada", new DomainException("regra violada").getMessage());
    }
}
