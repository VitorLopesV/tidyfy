package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes das regras de {@link Schedule}.
 */
class ScheduleTest {

    private static final LocalDate START = LocalDate.of(2026, 1, 15);

    @Test
    void rejectsIntervalSmallerThanOne() {
        assertThrows(IllegalArgumentException.class, () -> new Schedule(0, List.of(LocalTime.NOON), START));
        assertThrows(IllegalArgumentException.class, () -> new Schedule(-3, List.of(LocalTime.NOON), START));
    }

    @Test
    void acceptsIntervalOfOneAndHugeIntervals() {
        assertEquals(1, new Schedule(1, List.of(LocalTime.NOON), START).interval());
        assertEquals(Integer.MAX_VALUE, new Schedule(Integer.MAX_VALUE, List.of(LocalTime.NOON), START).interval());
    }

    @Test
    void rejectsEmptyTimes() {
        assertThrows(IllegalArgumentException.class, () -> new Schedule(1, List.of(), START));
    }

    @Test
    void rejectsRepeatedTimes() {
        List<LocalTime> repeated = List.of(LocalTime.of(8, 0), LocalTime.of(9, 0), LocalTime.of(8, 0));
        assertThrows(IllegalArgumentException.class, () -> new Schedule(1, repeated, START));
    }

    @Test
    void keepsTimesInAscendingOrder() {
        Schedule schedule = new Schedule(2, List.of(LocalTime.of(18, 30), LocalTime.of(7, 0), LocalTime.of(12, 0)), START);
        assertEquals(List.of(LocalTime.of(7, 0), LocalTime.of(12, 0), LocalTime.of(18, 30)), schedule.times());
    }

    @Test
    void keepsStartDate() {
        assertEquals(START, new Schedule(1, List.of(LocalTime.NOON), START).startDate());
    }

    @Test
    void timesAreImmutableAndIndependentOfTheOriginalList() {
        List<LocalTime> original = new ArrayList<>(List.of(LocalTime.NOON));
        Schedule schedule = new Schedule(1, original, START);
        original.add(LocalTime.MIDNIGHT);
        assertEquals(List.of(LocalTime.NOON), schedule.times());
        assertThrows(UnsupportedOperationException.class, () -> schedule.times().add(LocalTime.MIDNIGHT));
    }

    @Test
    void rejectsNullTimesAndNullStartDate() {
        assertThrows(NullPointerException.class, () -> new Schedule(1, null, START));
        assertThrows(NullPointerException.class, () -> new Schedule(1, List.of(LocalTime.NOON), null));
    }

    private static Schedule schedule(int interval, ScheduleUnit unit, LocalDate start) {
        return new Schedule(interval, unit, List.of(LocalTime.NOON), start);
    }

    @Test
    void legacyConstructorIsInDays() {
        assertEquals(ScheduleUnit.DAY, new Schedule(3, List.of(LocalTime.NOON), START).unit());
    }

    @Test
    void rejectsNullUnit() {
        assertThrows(NullPointerException.class, () -> new Schedule(1, null, List.of(LocalTime.NOON), START));
    }

    @Test
    void rejectsIntervalsTheUnitDoesNotAccept() {
        assertThrows(IllegalArgumentException.class, () -> schedule(5, ScheduleUnit.MONTH, START));
        assertThrows(IllegalArgumentException.class, () -> schedule(2, ScheduleUnit.YEAR, START));
        assertThrows(IllegalArgumentException.class, () -> schedule(0, ScheduleUnit.WEEK, START));
    }

    @Test
    void acceptsEachUnitWithAValidInterval() {
        assertEquals(2, schedule(2, ScheduleUnit.WEEK, START).interval());
        assertEquals(6, schedule(6, ScheduleUnit.MONTH, START).interval());
        assertEquals(1, schedule(1, ScheduleUnit.YEAR, START).interval());
    }

    @Test
    void dailyOccurrencesAreEveryIntervalDays() {
        Schedule schedule = schedule(3, ScheduleUnit.DAY, LocalDate.of(2026, 3, 10));
        assertEquals(LocalDate.of(2026, 3, 10), schedule.occurrence(0));
        assertEquals(LocalDate.of(2026, 3, 13), schedule.occurrence(1));
        assertTrue(schedule.occursOn(LocalDate.of(2026, 3, 16)));
        assertFalse(schedule.occursOn(LocalDate.of(2026, 3, 15)));
    }

    @Test
    void weeklyOccurrencesKeepTheWeekdayOfTheStartDate() {
        Schedule schedule = schedule(2, ScheduleUnit.WEEK, LocalDate.of(2026, 3, 10));
        assertEquals(LocalDate.of(2026, 3, 24), schedule.occurrence(1));
        assertTrue(schedule.occursOn(LocalDate.of(2026, 4, 7)));
        assertFalse(schedule.occursOn(LocalDate.of(2026, 3, 17)));
        assertFalse(schedule.occursOn(LocalDate.of(2026, 3, 25)));
    }

    @Test
    void monthlyOccurrencesKeepTheDayOfTheMonth() {
        Schedule schedule = schedule(3, ScheduleUnit.MONTH, LocalDate.of(2026, 1, 15));
        assertEquals(LocalDate.of(2026, 4, 15), schedule.occurrence(1));
        assertEquals(LocalDate.of(2027, 1, 15), schedule.occurrence(4));
        assertTrue(schedule.occursOn(LocalDate.of(2026, 7, 15)));
        assertFalse(schedule.occursOn(LocalDate.of(2026, 5, 15)));
    }

    @Test
    void monthlyOccurrencesFromTheEndOfTheMonthFallOnTheLastDay() {
        Schedule schedule = schedule(1, ScheduleUnit.MONTH, LocalDate.of(2026, 1, 31));
        assertEquals(LocalDate.of(2026, 2, 28), schedule.occurrence(1));
        assertEquals(LocalDate.of(2026, 3, 31), schedule.occurrence(2));
        assertEquals(LocalDate.of(2026, 4, 30), schedule.occurrence(3));
        assertEquals(LocalDate.of(2028, 2, 29), schedule.occurrence(25));
    }

    @Test
    void yearlyOccurrencesKeepTheDate() {
        Schedule schedule = schedule(1, ScheduleUnit.YEAR, LocalDate.of(2026, 3, 10));
        assertEquals(LocalDate.of(2027, 3, 10), schedule.occurrence(1));
        assertTrue(schedule.occursOn(LocalDate.of(2030, 3, 10)));
        assertFalse(schedule.occursOn(LocalDate.of(2030, 3, 11)));
    }

    @Test
    void yearlyOccurrencesFromLeapDayFallOnFebruaryTwentyEighthInCommonYears() {
        Schedule schedule = schedule(1, ScheduleUnit.YEAR, LocalDate.of(2028, 2, 29));
        assertEquals(LocalDate.of(2029, 2, 28), schedule.occurrence(1));
        assertEquals(LocalDate.of(2032, 2, 29), schedule.occurrence(4));
    }

    @Test
    void indexOnOrAfterFindsTheFirstOccurrenceNotBeforeTheDate() {
        Schedule schedule = schedule(2, ScheduleUnit.WEEK, LocalDate.of(2026, 3, 10));
        assertEquals(0, schedule.indexOnOrAfter(LocalDate.of(2026, 1, 1)));
        assertEquals(0, schedule.indexOnOrAfter(LocalDate.of(2026, 3, 10)));
        assertEquals(1, schedule.indexOnOrAfter(LocalDate.of(2026, 3, 11)));
        assertEquals(1, schedule.indexOnOrAfter(LocalDate.of(2026, 3, 24)));
        assertEquals(2, schedule.indexOnOrAfter(LocalDate.of(2026, 3, 25)));
    }

    @Test
    void indexOnOrAfterWorksForEndOfMonthStarts() {
        Schedule schedule = schedule(1, ScheduleUnit.MONTH, LocalDate.of(2026, 1, 31));
        assertEquals(1, schedule.indexOnOrAfter(LocalDate.of(2026, 2, 28)));
        assertEquals(2, schedule.indexOnOrAfter(LocalDate.of(2026, 3, 1)));
    }
}
