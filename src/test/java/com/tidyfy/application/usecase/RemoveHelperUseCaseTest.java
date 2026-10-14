package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeExecutionHistoryRepository;
import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link RemoveHelperUseCase}, com os fakes em memória das portas.
 */
class RemoveHelperUseCaseTest {

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private FakeHelperRepository repository;
    private FakeTaskScheduler scheduler;
    private RemoveHelperUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeHelperRepository();
        scheduler = new FakeTaskScheduler();
        useCase = new RemoveHelperUseCase(repository, scheduler);
    }

    private Helper existing(String name) {
        Helper helper = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName(name), SCHEDULE, true, Path.of("C:\\" + name));
        repository.with(helper);
        scheduler.register(helper);
        return helper;
    }

    @Test
    void removesHelperFromSchedulerAndRepository() {
        Helper helper = existing("A");

        useCase.execute(List.of(helper.id()));

        assertFalse(scheduler.isRegistered(helper.id()));
        assertTrue(repository.findById(helper.id()).isEmpty());
    }

    @Test
    void removesSeveralHelpersAtOnceAndKeepsTheOthers() {
        Helper a = existing("A");
        Helper b = existing("B");
        Helper c = existing("C");

        useCase.execute(List.of(a.id(), c.id()));

        assertEquals(List.of(b), repository.findAll());
        assertEquals(List.of(b.id()), scheduler.registered());
        assertEquals(List.of(a.id(), c.id()), scheduler.unregisterCalls());
    }

    @Test
    void unknownIdIsNotAnError() {
        Helper helper = existing("A");

        useCase.execute(List.of(HelperId.newId()));

        assertEquals(List.of(helper), repository.findAll());
        assertTrue(scheduler.isRegistered(helper.id()));
    }

    @Test
    void emptyCollectionDoesNothing() {
        Helper helper = existing("A");

        useCase.execute(List.of());

        assertEquals(List.of(helper), repository.findAll());
        assertTrue(scheduler.unregisterCalls().isEmpty());
    }

    @Test
    void keepsHistoryOfRemovedHelper() {
        Helper helper = existing("A");
        FakeExecutionHistoryRepository history = new FakeExecutionHistoryRepository();
        ExecutionRecord record = new ExecutionRecord(helper.id(), "A", Instant.now(), List.of());
        history.add(record);

        useCase.execute(List.of(helper.id()));

        assertEquals(List.of(record), history.findByHelper(helper.id()));
    }

    @Test
    void schedulerFailureKeepsHelperInRepositoryAndPropagates() {
        Helper helper = existing("A");
        RemoveHelperUseCase failingUseCase = new RemoveHelperUseCase(repository, new FailingUnregisterScheduler());

        assertThrows(UncheckedIOException.class, () -> failingUseCase.execute(List.of(helper.id())));

        assertEquals(List.of(helper), repository.findAll());
    }

    @Test
    void rejectsNullIds() {
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
    }

    @Test
    void rejectsNullDependencies() {
        assertThrows(NullPointerException.class, () -> new RemoveHelperUseCase(null, scheduler));
        assertThrows(NullPointerException.class, () -> new RemoveHelperUseCase(repository, null));
    }

    /**
     * Agendador cuja remoção sempre falha, para simular erro do schtasks.
     */
    private static final class FailingUnregisterScheduler implements TaskScheduler {

        @Override
        public void register(Helper helper) {
        }

        @Override
        public void unregister(HelperId id) {
            throw new UncheckedIOException(new IOException("falha"));
        }

        @Override
        public List<HelperId> registeredIds() {
            return List.of();
        }
    }
}
