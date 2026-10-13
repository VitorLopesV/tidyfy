package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void throwsWhenCountEqualsTheLimitForEveryType() {
        for (HelperType type : HelperType.values()) {
            assertThrows(HelperLimitReachedException.class, () -> type.assertCanCreate(20));
        }
    }

    @Test
    void throwsWhenCountIsAboveTheLimit() {
        assertThrows(HelperLimitReachedException.class, () -> HelperType.CLEANER.assertCanCreate(21));
    }

    @Test
    void doesNotThrowBelowTheLimitForEveryType() {
        for (HelperType type : HelperType.values()) {
            assertDoesNotThrow(() -> type.assertCanCreate(0));
            assertDoesNotThrow(() -> type.assertCanCreate(19));
        }
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
