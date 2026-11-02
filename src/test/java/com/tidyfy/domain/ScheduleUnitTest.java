package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Testes dos intervalos aceitos por cada {@link ScheduleUnit}.
 */
class ScheduleUnitTest {

    @Test
    void dayAndWeekAcceptAnyPositiveInterval() {
        assertTrue(ScheduleUnit.DAY.accepts(1));
        assertTrue(ScheduleUnit.DAY.accepts(500));
        assertTrue(ScheduleUnit.WEEK.accepts(5));
        assertFalse(ScheduleUnit.DAY.accepts(0));
        assertFalse(ScheduleUnit.WEEK.accepts(-1));
    }

    @Test
    void monthAcceptsOnlyDivisorsOfTwelve() {
        for (int valid : new int[] {1, 2, 3, 4, 6, 12}) {
            assertTrue(ScheduleUnit.MONTH.accepts(valid));
        }
        for (int invalid : new int[] {0, 5, 7, 8, 9, 10, 11, 13, 24}) {
            assertFalse(ScheduleUnit.MONTH.accepts(invalid));
        }
    }

    @Test
    void yearAcceptsOnlyOne() {
        assertTrue(ScheduleUnit.YEAR.accepts(1));
        assertFalse(ScheduleUnit.YEAR.accepts(2));
    }

    @Test
    void increaseAndDecreaseStepByOneForDayAndWeek() {
        assertEquals(4, ScheduleUnit.DAY.increase(3));
        assertEquals(2, ScheduleUnit.WEEK.decrease(3));
        assertEquals(1, ScheduleUnit.DAY.decrease(1));
    }

    @Test
    void increaseAndDecreaseSkipToTheNextAcceptedMonthInterval() {
        assertEquals(6, ScheduleUnit.MONTH.increase(4));
        assertEquals(12, ScheduleUnit.MONTH.increase(6));
        assertEquals(12, ScheduleUnit.MONTH.increase(12));
        assertEquals(4, ScheduleUnit.MONTH.decrease(6));
        assertEquals(1, ScheduleUnit.MONTH.decrease(1));
    }

    @Test
    void yearHasNowhereToStep() {
        assertEquals(1, ScheduleUnit.YEAR.increase(1));
        assertEquals(1, ScheduleUnit.YEAR.decrease(1));
    }

    @Test
    void adjustKeepsAcceptedValuesAndRoundsOthersDown() {
        assertEquals(7, ScheduleUnit.DAY.adjust(7));
        assertEquals(1, ScheduleUnit.DAY.adjust(0));
        assertEquals(4, ScheduleUnit.MONTH.adjust(5));
        assertEquals(6, ScheduleUnit.MONTH.adjust(7));
        assertEquals(12, ScheduleUnit.MONTH.adjust(30));
        assertEquals(1, ScheduleUnit.MONTH.adjust(0));
        assertEquals(1, ScheduleUnit.YEAR.adjust(9));
    }
}
