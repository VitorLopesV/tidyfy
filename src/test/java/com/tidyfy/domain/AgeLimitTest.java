package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link AgeLimit} e {@link AgeReference}.
 */
class AgeLimitTest {

    private static final Instant NOW = Instant.parse("2026-01-31T12:00:00Z");

    @Test
    void rejectsZeroDays() {
        assertThrows(IllegalArgumentException.class, () -> new AgeLimit(0));
    }

    @Test
    void rejectsNegativeDays() {
        assertThrows(IllegalArgumentException.class, () -> new AgeLimit(-5));
    }

    @Test
    void acceptsOneDay() {
        assertEquals(1, new AgeLimit(1).days());
    }

    @Test
    void isNotExceededByRecentMoment() {
        assertFalse(new AgeLimit(30).isExceededBy(Instant.parse("2026-01-30T12:00:00Z"), NOW));
    }

    @Test
    void isNotExceededWhenAgeEqualsLimit() {
        assertFalse(new AgeLimit(30).isExceededBy(Instant.parse("2026-01-01T12:00:00Z"), NOW));
    }

    @Test
    void isExceededWhenAgeIsJustOverLimit() {
        assertTrue(new AgeLimit(30).isExceededBy(Instant.parse("2026-01-01T11:59:59Z"), NOW));
    }

    @Test
    void isExceededByVeryOldMoment() {
        assertTrue(new AgeLimit(7).isExceededBy(Instant.parse("2020-01-01T00:00:00Z"), NOW));
    }

    @Test
    void isNotExceededByMomentInTheFuture() {
        assertFalse(new AgeLimit(1).isExceededBy(Instant.parse("2026-02-10T00:00:00Z"), NOW));
    }

    @Test
    void rejectsNullMoment() {
        assertThrows(NullPointerException.class, () -> new AgeLimit(1).isExceededBy(null, NOW));
    }

    @Test
    void rejectsNullNow() {
        assertThrows(NullPointerException.class, () -> new AgeLimit(1).isExceededBy(NOW, null));
    }

    @Test
    void ageReferenceHasCreatedAndModified() {
        assertEquals(2, AgeReference.values().length);
        assertEquals(AgeReference.CREATED, AgeReference.valueOf("CREATED"));
        assertEquals(AgeReference.MODIFIED, AgeReference.valueOf("MODIFIED"));
    }
}
