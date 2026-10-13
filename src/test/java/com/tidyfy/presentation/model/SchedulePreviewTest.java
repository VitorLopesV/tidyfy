package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa os textos e as contas da prévia de frequência.
 */
class SchedulePreviewTest {

    @Test
    void describesDailyAndMultiDayIntervals() {
        assertEquals("Todo dia", SchedulePreview.every(1));
        assertEquals("A cada 3 dias", SchedulePreview.every(3));
    }

    @Test
    void unitIsSingularOnlyForOneDay() {
        assertEquals("dia", SchedulePreview.unit(1));
        assertEquals("dias", SchedulePreview.unit(2));
    }

    @Test
    void formatsTimesWithTwoDigits() {
        assertEquals("08:05", SchedulePreview.time(LocalTime.of(8, 5)));
    }

    @Test
    void summaryWithOneTime() {
        assertEquals("A cada 7 dias às 20:00.", SchedulePreview.summary(7, List.of(LocalTime.of(20, 0))));
    }

    @Test
    void summaryJoinsTwoTimesWithE() {
        assertEquals("Todo dia às 09:00 e 18:00.",
                SchedulePreview.summary(1, List.of(LocalTime.of(9, 0), LocalTime.of(18, 0))));
    }

    @Test
    void summaryListsSeveralTimesInOrder() {
        assertEquals("Todo dia às 07:00, 12:00 e 18:00.",
                SchedulePreview.summary(1, List.of(LocalTime.of(18, 0), LocalTime.of(7, 0), LocalTime.of(12, 0))));
    }

    @Test
    void summaryWithoutTimesShowsOnlyTheInterval() {
        assertEquals("A cada 2 dias.", SchedulePreview.summary(2, List.of()));
    }

    @Test
    void runsTodayAndThenEveryIntervalDays() {
        assertTrue(SchedulePreview.runsOn(0, 3));
        assertFalse(SchedulePreview.runsOn(1, 3));
        assertFalse(SchedulePreview.runsOn(2, 3));
        assertTrue(SchedulePreview.runsOn(3, 3));
    }

    @Test
    void countsRunsInTheNextFourteenDays() {
        assertEquals(28, SchedulePreview.runCount(1, 2));
        assertEquals(5, SchedulePreview.runCount(3, 1));
        assertEquals(2, SchedulePreview.runCount(7, 1));
        assertEquals(1, SchedulePreview.runCount(14, 1));
        assertEquals(1, SchedulePreview.runCount(30, 1));
    }

    @Test
    void runsLabelIsSingularOnlyForOneRun() {
        assertEquals("1 execução", SchedulePreview.runsLabel(1));
        assertEquals("5 execuções", SchedulePreview.runsLabel(5));
    }

    @Test
    void weekdaysUseShortPortugueseNames() {
        assertEquals("seg", SchedulePreview.weekday(DayOfWeek.MONDAY));
        assertEquals("sáb", SchedulePreview.weekday(DayOfWeek.SATURDAY));
        assertEquals("dom", SchedulePreview.weekday(DayOfWeek.SUNDAY));
    }
}
