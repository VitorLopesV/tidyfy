package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link ScheduleCalculator}: próxima execução, execução anterior e progresso.
 */
class ScheduleCalculatorTest {

    private static final LocalDate START = LocalDate.of(2026, 3, 10);

    private static ScheduleCalculator at(String dateTime) {
        ZoneId zone = ZoneOffset.UTC;
        return new ScheduleCalculator(Clock.fixed(LocalDateTime.parse(dateTime).atZone(zone).toInstant(), zone));
    }

    private static Schedule schedule(int days, String... times) {
        return new Schedule(days, Arrays.stream(times).map(LocalTime::parse).toList(), START);
    }

    @Test
    void nextIsLaterTodayWhenTimeHasNotPassed() {
        assertEquals(LocalDateTime.parse("2026-03-10T14:00"),
                at("2026-03-10T09:00").nextExecution(schedule(1, "14:00")));
    }

    @Test
    void nextIsTomorrowAfterTheLastTimeOfTheDay() {
        assertEquals(LocalDateTime.parse("2026-03-11T08:00"),
                at("2026-03-10T20:00").nextExecution(schedule(1, "08:00", "14:00")));
    }

    @Test
    void nextPicksTheFirstTimeAfterNowAmongSeveral() {
        assertEquals(LocalDateTime.parse("2026-03-10T14:00"),
                at("2026-03-10T09:00").nextExecution(schedule(1, "08:00", "14:00", "20:00")));
    }

    @Test
    void timeEqualToNowCountsAsAlreadyExecuted() {
        assertEquals(LocalDateTime.parse("2026-03-10T20:00"),
                at("2026-03-10T14:00").nextExecution(schedule(1, "08:00", "14:00", "20:00")));
    }

    @Test
    void nextIsStartDayFirstTimeBeforeStart() {
        assertEquals(LocalDateTime.parse("2026-03-10T08:00"),
                at("2026-03-05T23:00").nextExecution(schedule(3, "08:00", "14:00")));
    }

    @Test
    void nextSkipsToTheNextValidDayWithIntervalGreaterThanOne() {
        assertEquals(LocalDateTime.parse("2026-03-13T08:00"),
                at("2026-03-11T10:00").nextExecution(schedule(3, "08:00")));
    }

    @Test
    void nextAfterLastTimeOnValidDayJumpsTheWholeInterval() {
        assertEquals(LocalDateTime.parse("2026-03-13T08:00"),
                at("2026-03-10T09:00").nextExecution(schedule(3, "08:00")));
    }

    @Test
    void nextCrossesMonthBoundary() {
        assertEquals(LocalDateTime.parse("2026-04-01T08:00"),
                at("2026-03-31T09:00").nextExecution(schedule(1, "08:00")));
    }

    @Test
    void nextCrossesYearBoundary() {
        assertEquals(LocalDateTime.parse("2027-01-01T08:00"),
                at("2026-12-31T23:59").nextExecution(schedule(1, "08:00")));
    }

    @Test
    void nextWithHugeIntervalIsStartDayPlusInterval() {
        assertEquals(LocalDateTime.parse("2027-03-10T08:00"),
                at("2026-06-01T08:00").nextExecution(schedule(365, "08:00")));
    }

    @Test
    void previousIsEmptyBeforeStart() {
        assertTrue(at("2026-03-09T23:00").previousExecution(schedule(1, "08:00")).isEmpty());
    }

    @Test
    void previousIsEmptyOnStartDayBeforeFirstTime() {
        assertTrue(at("2026-03-10T07:00").previousExecution(schedule(2, "08:00")).isEmpty());
    }

    @Test
    void previousIsTheTimeEqualToNowWhenAlreadyExecuted() {
        assertEquals(LocalDateTime.parse("2026-03-10T14:00"),
                at("2026-03-10T14:00").previousExecution(schedule(1, "08:00", "14:00", "20:00")).orElseThrow());
    }

    @Test
    void previousIsLastTimeOfThePreviousValidDay() {
        assertEquals(LocalDateTime.parse("2026-03-13T14:00"),
                at("2026-03-15T10:00").previousExecution(schedule(3, "08:00", "14:00")).orElseThrow());
    }

    @Test
    void previousBeforeFirstTimeOnLaterValidDayIsPreviousValidDay() {
        assertEquals(LocalDateTime.parse("2026-03-10T14:00"),
                at("2026-03-13T07:00").previousExecution(schedule(3, "08:00", "14:00")).orElseThrow());
    }

    @Test
    void progressIsZeroBeforeTheFirstExecution() {
        assertEquals(0.0, at("2026-03-05T12:00").progress(schedule(1, "08:00")));
        assertEquals(0.0, at("2026-03-10T07:00").progress(schedule(1, "08:00")));
    }

    @Test
    void progressIsZeroRightAtAnExecution() {
        assertEquals(0.0, at("2026-03-11T08:00").progress(schedule(1, "08:00")));
    }

    @Test
    void progressIsHalfInTheMiddleOfTheInterval() {
        assertEquals(0.5, at("2026-03-11T20:00").progress(schedule(1, "08:00")), 1e-9);
    }

    @Test
    void progressIsNearOneJustBeforeTheNextExecution() {
        double progress = at("2026-03-11T07:59").progress(schedule(1, "08:00"));
        assertTrue(progress > 0.99 && progress < 1.0);
    }

    @Test
    void progressWithSeveralTimesMeasuresBetweenConsecutiveTimes() {
        assertEquals(0.5, at("2026-03-10T11:00").progress(schedule(1, "08:00", "14:00")), 1e-9);
    }

    @Test
    void progressWithIntervalGreaterThanOneSpansTheWholeInterval() {
        assertEquals(0.5, at("2026-03-11T08:00").progress(schedule(2, "08:00")), 1e-9);
    }

    @Test
    void rejectsNullClockAndNullSchedule() {
        assertThrows(NullPointerException.class, () -> new ScheduleCalculator(null));
        assertThrows(NullPointerException.class, () -> at("2026-03-10T08:00").nextExecution(null));
        assertThrows(NullPointerException.class, () -> at("2026-03-10T08:00").previousExecution(null));
        assertThrows(NullPointerException.class, () -> at("2026-03-10T08:00").progress(null));
    }

    private static Schedule schedule(int interval, ScheduleUnit unit, LocalDate start, String... times) {
        return new Schedule(interval, unit, Arrays.stream(times).map(LocalTime::parse).toList(), start);
    }

    @Test
    void weeklyNextSkipsToTheNextOccurrenceWeekday() {
        Schedule weekly = schedule(2, ScheduleUnit.WEEK, START, "08:00");
        assertEquals(LocalDateTime.parse("2026-03-24T08:00"), at("2026-03-12T09:00").nextExecution(weekly));
        assertEquals(LocalDateTime.parse("2026-03-24T08:00"), at("2026-03-10T09:00").nextExecution(weekly));
        assertEquals(LocalDateTime.parse("2026-03-10T08:00"), at("2026-03-10T07:00").nextExecution(weekly));
    }

    @Test
    void weeklyPreviousIsTheLastOccurrenceAtTheLastTime() {
        Schedule weekly = schedule(2, ScheduleUnit.WEEK, START, "08:00", "18:00");
        assertEquals(LocalDateTime.parse("2026-03-10T18:00"),
                at("2026-03-20T12:00").previousExecution(weekly).orElseThrow());
        assertEquals(LocalDateTime.parse("2026-03-24T08:00"),
                at("2026-03-24T09:00").previousExecution(weekly).orElseThrow());
        assertTrue(at("2026-03-10T07:00").previousExecution(weekly).isEmpty());
    }

    @Test
    void monthlyNextAndPreviousFollowTheCalendar() {
        Schedule monthly = schedule(3, ScheduleUnit.MONTH, LocalDate.of(2026, 1, 15), "10:00");
        assertEquals(LocalDateTime.parse("2026-04-15T10:00"), at("2026-02-01T00:00").nextExecution(monthly));
        assertEquals(LocalDateTime.parse("2026-07-15T10:00"), at("2026-04-15T10:00").nextExecution(monthly));
        assertEquals(LocalDateTime.parse("2026-04-15T10:00"),
                at("2026-06-30T00:00").previousExecution(monthly).orElseThrow());
    }

    @Test
    void monthlyFromTheEndOfTheMonthUsesTheLastDay() {
        Schedule monthly = schedule(1, ScheduleUnit.MONTH, LocalDate.of(2026, 1, 31), "10:00");
        assertEquals(LocalDateTime.parse("2026-02-28T10:00"), at("2026-02-01T00:00").nextExecution(monthly));
        assertEquals(LocalDateTime.parse("2026-02-28T10:00"),
                at("2026-03-15T00:00").previousExecution(monthly).orElseThrow());
    }

    @Test
    void yearlyNextAndPreviousFollowTheAnniversary() {
        Schedule yearly = schedule(1, ScheduleUnit.YEAR, LocalDate.of(2026, 3, 10), "09:00");
        assertEquals(LocalDateTime.parse("2027-03-10T09:00"), at("2026-03-10T09:00").nextExecution(yearly));
        assertEquals(LocalDateTime.parse("2028-03-10T09:00"), at("2027-06-01T00:00").nextExecution(yearly));
        assertEquals(LocalDateTime.parse("2027-03-10T09:00"),
                at("2028-01-01T00:00").previousExecution(yearly).orElseThrow());
    }

    @Test
    void beforeTheStartDateNextIsTheFirstTimeOfTheStartDateForEveryUnit() {
        for (ScheduleUnit unit : ScheduleUnit.values()) {
            assertEquals(LocalDateTime.parse("2026-03-10T08:00"),
                    at("2026-01-01T00:00").nextExecution(schedule(1, unit, START, "08:00", "20:00")));
        }
    }

    @Test
    void weeklyProgressGrowsBetweenOccurrences() {
        Schedule weekly = schedule(1, ScheduleUnit.WEEK, START, "08:00");
        assertEquals(0.5, at("2026-03-13T20:00").progress(weekly), 0.001);
    }
}
