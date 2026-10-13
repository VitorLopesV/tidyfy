package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        assertEquals(1, new Schedule(1, List.of(LocalTime.NOON), START).intervalDays());
        assertEquals(Integer.MAX_VALUE, new Schedule(Integer.MAX_VALUE, List.of(LocalTime.NOON), START).intervalDays());
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
}
