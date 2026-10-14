package com.tidyfy.application.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link FakeTaskScheduler}: garantem que ele segue o contrato da porta.
 */
class FakeTaskSchedulerTest {

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private static Helper helper(HelperId id, String name) {
        return new FolderOrganizerHelper(id, new HelperName(name), SCHEDULE, true, Path.of("C:\\Pasta"));
    }

    @Test
    void registerStoresTaskAndRecordsCall() {
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        Helper helper = helper(HelperId.newId(), "A");

        scheduler.register(helper);

        assertTrue(scheduler.isRegistered(helper.id()));
        assertEquals(List.of(helper.id()), scheduler.registered());
        assertEquals(List.of(helper), scheduler.registerCalls());
    }

    @Test
    void registerReplacesExistingTaskOfSameHelper() {
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        HelperId id = HelperId.newId();
        Helper first = helper(id, "A");
        Helper second = helper(id, "B");

        scheduler.register(first);
        scheduler.register(second);

        assertEquals(List.of(id), scheduler.registered());
        assertEquals(second, scheduler.registeredHelper(id));
        assertEquals(List.of(first, second), scheduler.registerCalls());
    }

    @Test
    void unregisterRemovesTaskAndRecordsCall() {
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        Helper helper = helper(HelperId.newId(), "A");
        scheduler.register(helper);

        scheduler.unregister(helper.id());

        assertFalse(scheduler.isRegistered(helper.id()));
        assertNull(scheduler.registeredHelper(helper.id()));
        assertEquals(List.of(helper.id()), scheduler.unregisterCalls());
    }

    @Test
    void unregisterWithoutTaskIsNotAnError() {
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        HelperId id = HelperId.newId();

        scheduler.unregister(id);

        assertTrue(scheduler.registered().isEmpty());
        assertEquals(List.of(id), scheduler.unregisterCalls());
    }

    @Test
    void failNextRegisterWithThrowsOnceWithoutChangingState() {
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        scheduler.failNextRegisterWith(new UncheckedIOException(new java.io.IOException("falha")));
        Helper helper = helper(HelperId.newId(), "A");

        assertThrows(UncheckedIOException.class, () -> scheduler.register(helper));
        assertFalse(scheduler.isRegistered(helper.id()));
        assertTrue(scheduler.registerCalls().isEmpty());

        scheduler.register(helper);
        assertTrue(scheduler.isRegistered(helper.id()));
    }
}
