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
}
