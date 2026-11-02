package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.AgeUnit;
import com.tidyfy.domain.ScheduleUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa os textos e as contas da prévia de frequência.
 */
class SchedulePreviewTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);

    @Test
    void describesDailyAndMultiDayIntervals() {
        assertEquals("Todo dia", SchedulePreview.every(1, ScheduleUnit.DAY));
        assertEquals("A cada 3 dias", SchedulePreview.every(3, ScheduleUnit.DAY));
    }

    @Test
    void unitIsSingularOnlyForOneDay() {
        assertEquals("dia", SchedulePreview.unit(1, ScheduleUnit.DAY));
        assertEquals("dias", SchedulePreview.unit(2, ScheduleUnit.DAY));
    }

    @Test
    void formatsTimesWithTwoDigits() {
        assertEquals("08:05", SchedulePreview.time(LocalTime.of(8, 5)));
    }

    @Test
    void summaryWithOneTime() {
        assertEquals("A cada 7 dias às 20:00.", SchedulePreview.summary(7, ScheduleUnit.DAY, List.of(LocalTime.of(20, 0))));
    }

    @Test
    void summaryJoinsTwoTimesWithE() {
        assertEquals("Todo dia às 09:00 e 18:00.",
                SchedulePreview.summary(1, ScheduleUnit.DAY, List.of(LocalTime.of(9, 0), LocalTime.of(18, 0))));
    }

    @Test
    void summaryListsSeveralTimesInOrder() {
        assertEquals("Todo dia às 07:00, 12:00 e 18:00.",
                SchedulePreview.summary(1, ScheduleUnit.DAY, List.of(LocalTime.of(18, 0), LocalTime.of(7, 0), LocalTime.of(12, 0))));
    }

    @Test
    void summaryWithoutTimesShowsOnlyTheInterval() {
        assertEquals("A cada 2 dias.", SchedulePreview.summary(2, ScheduleUnit.DAY, List.of()));
    }

    @Test
    void runsTodayAndThenEveryIntervalDays() {
        assertTrue(SchedulePreview.runsOn(TODAY, 0, 3, ScheduleUnit.DAY));
        assertFalse(SchedulePreview.runsOn(TODAY, 1, 3, ScheduleUnit.DAY));
        assertFalse(SchedulePreview.runsOn(TODAY, 2, 3, ScheduleUnit.DAY));
        assertTrue(SchedulePreview.runsOn(TODAY, 3, 3, ScheduleUnit.DAY));
    }

    @Test
    void countsRunsInTheNextFourteenDays() {
        assertEquals(28, SchedulePreview.runCount(TODAY, 1, ScheduleUnit.DAY, 2));
        assertEquals(5, SchedulePreview.runCount(TODAY, 3, ScheduleUnit.DAY, 1));
        assertEquals(2, SchedulePreview.runCount(TODAY, 7, ScheduleUnit.DAY, 1));
        assertEquals(1, SchedulePreview.runCount(TODAY, 14, ScheduleUnit.DAY, 1));
        assertEquals(1, SchedulePreview.runCount(TODAY, 30, ScheduleUnit.DAY, 1));
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

    @Test
    void ageUnitsAreSingularOnlyForOne() {
        assertEquals("dia", SchedulePreview.ageUnit(AgeUnit.DAY, 1));
        assertEquals("dias", SchedulePreview.ageUnit(AgeUnit.DAY, 2));
        assertEquals("semana", SchedulePreview.ageUnit(AgeUnit.WEEK, 1));
        assertEquals("semanas", SchedulePreview.ageUnit(AgeUnit.WEEK, 3));
        assertEquals("mês", SchedulePreview.ageUnit(AgeUnit.MONTH, 1));
        assertEquals("meses", SchedulePreview.ageUnit(AgeUnit.MONTH, 4));
        assertEquals("ano", SchedulePreview.ageUnit(AgeUnit.YEAR, 1));
        assertEquals("anos", SchedulePreview.ageUnit(AgeUnit.YEAR, 2));
    }

    @Test
    void describesEveryUnitInSingularAndPlural() {
        assertEquals("Toda semana", SchedulePreview.every(1, ScheduleUnit.WEEK));
        assertEquals("A cada 2 semanas", SchedulePreview.every(2, ScheduleUnit.WEEK));
        assertEquals("Todo mês", SchedulePreview.every(1, ScheduleUnit.MONTH));
        assertEquals("A cada 3 meses", SchedulePreview.every(3, ScheduleUnit.MONTH));
        assertEquals("Todo ano", SchedulePreview.every(1, ScheduleUnit.YEAR));
        assertEquals("A cada 2 anos", SchedulePreview.every(2, ScheduleUnit.YEAR));
    }

    @Test
    void frequencyUnitNamesFollowTheInterval() {
        assertEquals("semana", SchedulePreview.unit(1, ScheduleUnit.WEEK));
        assertEquals("meses", SchedulePreview.unit(2, ScheduleUnit.MONTH));
        assertEquals("mês", SchedulePreview.unit(1, ScheduleUnit.MONTH));
        assertEquals("ano", SchedulePreview.unit(1, ScheduleUnit.YEAR));
    }

    @Test
    void summaryUsesTheUnit() {
        assertEquals("A cada 2 semanas às 08:00.",
                SchedulePreview.summary(2, ScheduleUnit.WEEK, List.of(LocalTime.of(8, 0))));
    }

    @Test
    void weeklyRunsFallOnTheSameWeekdayAsToday() {
        assertTrue(SchedulePreview.runsOn(TODAY, 0, 1, ScheduleUnit.WEEK));
        assertFalse(SchedulePreview.runsOn(TODAY, 3, 1, ScheduleUnit.WEEK));
        assertTrue(SchedulePreview.runsOn(TODAY, 7, 1, ScheduleUnit.WEEK));
        assertFalse(SchedulePreview.runsOn(TODAY, 7, 2, ScheduleUnit.WEEK));
        assertEquals(2, SchedulePreview.runCount(TODAY, 1, ScheduleUnit.WEEK, 1));
        assertEquals(1, SchedulePreview.runCount(TODAY, 2, ScheduleUnit.WEEK, 1));
    }

    @Test
    void monthlyAndYearlyRunsOnlyTodayInTheNextFourteenDays() {
        assertEquals(1, SchedulePreview.runCount(TODAY, 1, ScheduleUnit.MONTH, 1));
        assertEquals(3, SchedulePreview.runCount(TODAY, 1, ScheduleUnit.YEAR, 3));
    }
}
