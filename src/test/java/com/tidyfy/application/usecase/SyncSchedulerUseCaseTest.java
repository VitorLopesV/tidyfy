package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.application.port.HelperRepository;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link SyncSchedulerUseCase}, com os fakes em memória das portas.
 */
class SyncSchedulerUseCaseTest {

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private FakeHelperRepository repository;
    private FakeTaskScheduler scheduler;
    private SyncSchedulerUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeHelperRepository();
        scheduler = new FakeTaskScheduler();
        useCase = new SyncSchedulerUseCase(repository, scheduler);
    }

    private Helper saved(String name, boolean enabled) {
        Helper helper = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName(name), SCHEDULE, enabled, Path.of("C:\\" + name));
        repository.with(helper);
        return helper;
    }

    @Test
    void doesNothingWithoutHelpersOrTasks() {
        SyncResult result = useCase.execute();

        assertEquals(new SyncResult(0, 0, 0, List.of()), result);
        assertTrue(result.succeeded());
        assertTrue(scheduler.registerCalls().isEmpty());
        assertTrue(scheduler.unregisterCalls().isEmpty());
    }

    @Test
    void registersTheMissingTaskOfAnEnabledHelper() {
        Helper helper = saved("A", true);

        SyncResult result = useCase.execute();

        assertTrue(scheduler.isRegistered(helper.id()));
        assertEquals(1, result.registered());
    }

    @Test
    void registersAgainTheExistingTaskOfAnEnabledHelper() {
        Helper helper = saved("A", true);
        scheduler.register(helper);

        SyncResult result = useCase.execute();

        assertEquals(List.of(helper, helper), scheduler.registerCalls());
        assertEquals(List.of(helper.id()), scheduler.registered());
        assertEquals(1, result.registered());
    }

    @Test
    void removesTheTaskOfADisabledHelper() {
        Helper helper = saved("A", false);
        scheduler.register(helper);

        SyncResult result = useCase.execute();

        assertFalse(scheduler.isRegistered(helper.id()));
        assertEquals(1, result.removed());
        assertEquals(0, result.registered());
    }

    @Test
    void keepsADisabledHelperWithoutTaskUnscheduled() {
        Helper helper = saved("A", false);

        SyncResult result = useCase.execute();

        assertFalse(scheduler.isRegistered(helper.id()));
        assertTrue(result.succeeded());
    }

    @Test
    void removesOrphanTasksOfHelpersThatNoLongerExist() {
        Helper kept = saved("A", true);
        Helper orphan = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName("B"), SCHEDULE, true, Path.of("C:\\B"));
        scheduler.addOrphanTask(orphan);

        SyncResult result = useCase.execute();

        assertEquals(List.of(kept.id()), scheduler.registered());
        assertEquals(1, result.orphansRemoved());
        assertEquals(List.of(orphan.id()), scheduler.unregisterCalls());
    }

    @Test
    void aFailureInOneHelperDoesNotStopTheOthers() {
        Helper first = saved("A", true);
        Helper second = saved("B", true);
        RuntimeException failure = new UncheckedIOException(new IOException("denied"));
        scheduler.failNextRegisterWith(failure);

        SyncResult result = useCase.execute();

        assertFalse(scheduler.isRegistered(first.id()));
        assertTrue(scheduler.isRegistered(second.id()));
        assertEquals(1, result.registered());
        assertFalse(result.succeeded());
        assertEquals(1, result.failures().size());
        assertEquals(first.id(), result.failures().get(0).id());
        assertSame(failure, result.failures().get(0).error());
    }

    @Test
    void aFailureRemovingADisabledHelperIsReportedAndTheRestContinues() {
        Helper disabled = saved("A", false);
        Helper enabled = saved("B", true);
        scheduler.failNextUnregisterWith(new IllegalStateException("denied"));

        SyncResult result = useCase.execute();

        assertEquals(0, result.removed());
        assertEquals(1, result.registered());
        assertTrue(scheduler.isRegistered(enabled.id()));
        assertEquals(disabled.id(), result.failures().get(0).id());
    }

    @Test
    void aFailureListingTasksIsReportedWithoutId() {
        Helper helper = saved("A", true);
        scheduler.failNextRegisteredIdsWith(new IllegalStateException("no schtasks"));

        SyncResult result = useCase.execute();

        assertTrue(scheduler.isRegistered(helper.id()));
        assertEquals(1, result.registered());
        assertEquals(1, result.failures().size());
        assertNull(result.failures().get(0).id());
    }

    @Test
    void aFailureRemovingAnOrphanIsReportedAndTheOtherOrphansAreRemoved() {
        Helper firstOrphan = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName("X"), SCHEDULE, true, Path.of("C:\\X"));
        Helper secondOrphan = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName("Y"), SCHEDULE, true, Path.of("C:\\Y"));
        scheduler.addOrphanTask(firstOrphan);
        scheduler.addOrphanTask(secondOrphan);
        scheduler.failNextUnregisterWith(new IllegalStateException("denied"));

        SyncResult result = useCase.execute();

        assertEquals(1, result.orphansRemoved());
        assertEquals(firstOrphan.id(), result.failures().get(0).id());
        assertFalse(scheduler.isRegistered(secondOrphan.id()));
    }

    @Test
    void failingToReadTheRepositoryPropagates() {
        HelperRepository broken = (HelperRepository) Proxy.newProxyInstance(
                HelperRepository.class.getClassLoader(), new Class<?>[] {HelperRepository.class},
                (proxy, method, args) -> {
                    throw new UncheckedIOException(new IOException("corrupt"));
                });
        SyncSchedulerUseCase failing = new SyncSchedulerUseCase(broken, scheduler);

        assertThrows(UncheckedIOException.class, failing::execute);
    }

    @Test
    void rejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new SyncSchedulerUseCase(null, scheduler));
        assertThrows(NullPointerException.class, () -> new SyncSchedulerUseCase(repository, null));
    }
}
