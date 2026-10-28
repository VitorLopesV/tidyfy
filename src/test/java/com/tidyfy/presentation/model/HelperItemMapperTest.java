package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.CleanupTarget;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link HelperItemMapper}: conversão dos ajudantes e cálculo da próxima execução e do progresso.
 */
class HelperItemMapperTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");

    /** Terça-feira, 10/03/2026, 10:00. */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), ZONE);

    private static final LocalDate START = LocalDate.of(2026, 3, 1);

    private final HelperItemMapper mapper = new HelperItemMapper(CLOCK);

    @Test
    void convertsAnOrganizer() {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"),
                schedule(1, LocalTime.of(18, 0), LocalTime.of(9, 0)), true, Path.of("C:\\Downloads"));

        HelperItem item = mapper.toItem(helper);

        assertEquals(helper.id().value().toString(), item.id());
        assertEquals("Organizar", item.name());
        assertEquals(HelperKind.ORGANIZER, item.kind());
        assertEquals(Path.of("C:\\Downloads"), item.folder());
        assertEquals(1, item.intervalDays());
        assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)), item.times());
        assertTrue(item.enabled());
        assertNull(item.ageLimitDays());
        assertNull(item.ageReference());
        assertEquals(START, item.startDate());
    }

    @Test
    void convertsADesktopArranger() {
        DesktopArrangerHelper helper = new DesktopArrangerHelper(HelperId.newId(), new HelperName("Arrumar"),
                schedule(1, LocalTime.of(9, 0)), true, DesktopLayout.BY_TYPE_LEFT);

        HelperItem item = mapper.toItem(helper);

        assertEquals(HelperKind.DESKTOP, item.kind());
        assertEquals(DesktopLayout.BY_TYPE_LEFT, item.desktopLayout());
        assertNull(item.folder());
        assertTrue(item.targetsDesktop());
        assertFalse(item.targetsRecycleBin());
        assertEquals(START, item.startDate());
        assertTrue(item.withEnabled(false).targetsDesktop());
        assertEquals(DesktopLayout.BY_TYPE_LEFT, item.withEnabled(false).desktopLayout());
    }

    @Test
    void keepsTheScheduleStartDateOfACleaner() {
        HelperItem folder = mapper.toItem(cleaner(new FolderTarget(Path.of("C:\\Temp"), AgeReference.CREATED),
                null, true));
        HelperItem bin = mapper.toItem(cleaner(new RecycleBinTarget(), null, true));

        assertEquals(START, folder.startDate());
        assertEquals(START, bin.startDate());
    }

    @Test
    void convertsAFolderCleanerWithAgeLimitAndReference() {
        CleanerHelper helper = cleaner(new FolderTarget(Path.of("C:\\Temp"), AgeReference.MODIFIED),
                new AgeLimit(30), true);

        HelperItem item = mapper.toItem(helper);

        assertEquals(HelperKind.CLEANER, item.kind());
        assertEquals(Path.of("C:\\Temp"), item.folder());
        assertEquals(30, item.ageLimitDays());
        assertEquals(HelperAgeReference.MODIFIED, item.ageReference());
    }

    @Test
    void convertsAFolderCleanerThatCleansEverything() {
        CleanerHelper helper = cleaner(new FolderTarget(Path.of("C:\\Temp"), AgeReference.CREATED), null, true);

        HelperItem item = mapper.toItem(helper);

        assertNull(item.ageLimitDays());
        assertEquals(HelperAgeReference.CREATED, item.ageReference());
    }

    @Test
    void convertsARecycleBinCleanerWithNoFolder() {
        CleanerHelper helper = cleaner(new RecycleBinTarget(), new AgeLimit(7), true);

        HelperItem item = mapper.toItem(helper);

        assertEquals(HelperKind.CLEANER, item.kind());
        assertNull(item.folder());
        assertTrue(item.targetsRecycleBin());
        assertEquals(7, item.ageLimitDays());
        assertNull(item.ageReference());
    }

    @Test
    void copiesTheScheduleFields() {
        CleanerHelper helper = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"),
                schedule(4, LocalTime.of(8, 0), LocalTime.of(20, 0)), true, new RecycleBinTarget(), null);

        HelperItem item = mapper.toItem(helper);

        assertEquals(4, item.intervalDays());
        assertEquals(List.of(LocalTime.of(8, 0), LocalTime.of(20, 0)), item.times());
    }

    @Test
    void nextRunIsTodayWhenTheNextTimeIsLaterToday() {
        HelperItem item = mapper.toItem(organizer(schedule(1, LocalTime.of(9, 0), LocalTime.of(18, 0)), true));

        assertEquals("Hoje, 18:00", item.nextRun());
    }

    @Test
    void nextRunIsTomorrowWhenAllTimesAlreadyPassedToday() {
        HelperItem item = mapper.toItem(organizer(schedule(1, LocalTime.of(7, 0), LocalTime.of(9, 30)), true));

        assertEquals("Amanhã, 07:00", item.nextRun());
    }

    @Test
    void nextRunShowsTheDateBeyondTomorrow() {
        Schedule schedule = new Schedule(5, List.of(LocalTime.of(8, 0)), LocalDate.of(2026, 3, 8));

        HelperItem item = mapper.toItem(organizer(schedule, true));

        assertEquals("13/03, 08:00", item.nextRun());
    }

    @Test
    void nextRunIsTomorrowOnTheDayBeforeAValidDay() {
        Schedule schedule = new Schedule(5, List.of(LocalTime.of(8, 0)), START);

        HelperItem item = mapper.toItem(organizer(schedule, true));

        assertEquals("Amanhã, 08:00", item.nextRun());
    }

    @Test
    void progressIsBetweenZeroAndOne() {
        HelperItem item = mapper.toItem(organizer(schedule(1, LocalTime.of(9, 0), LocalTime.of(18, 0)), true));

        assertEquals(1.0 / 9.0, item.nextRunProgress(), 1e-9);
    }

    @Test
    void progressIsZeroBeforeTheFirstExecution() {
        Schedule schedule = new Schedule(1, List.of(LocalTime.of(18, 0)), LocalDate.of(2026, 3, 10));

        HelperItem item = mapper.toItem(organizer(schedule, true));

        assertEquals(0.0, item.nextRunProgress());
        assertEquals("Hoje, 18:00", item.nextRun());
    }

    @Test
    void disabledHelperHasNoNextRunAndZeroProgress() {
        HelperItem item = mapper.toItem(organizer(schedule(1, LocalTime.of(9, 0), LocalTime.of(18, 0)), false));

        assertTrue(!item.enabled());
        assertNull(item.nextRun());
        assertEquals(0.0, item.nextRunProgress());
    }

    @Test
    void convertsAnEmptyList() {
        assertTrue(mapper.toItems(List.of()).isEmpty());
    }

    @Test
    void convertsAListKeepingTheOrder() {
        List<Helper> helpers = List.of(
                organizer(schedule(1, LocalTime.of(18, 0)), true),
                cleaner(new RecycleBinTarget(), null, true));

        List<HelperItem> items = mapper.toItems(helpers);

        assertEquals(List.of(HelperKind.ORGANIZER, HelperKind.CLEANER), items.stream().map(HelperItem::kind).toList());
    }

    private static Schedule schedule(int intervalDays, LocalTime... times) {
        return new Schedule(intervalDays, List.of(times), START);
    }

    private static FolderOrganizerHelper organizer(Schedule schedule, boolean enabled) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"), schedule, enabled,
                Path.of("C:\\Downloads"));
    }

    private static CleanerHelper cleaner(CleanupTarget target, AgeLimit limit, boolean enabled) {
        return new CleanerHelper(HelperId.newId(), new HelperName("Limpar"),
                schedule(1, LocalTime.of(20, 0)), enabled, target, limit);
    }
}
