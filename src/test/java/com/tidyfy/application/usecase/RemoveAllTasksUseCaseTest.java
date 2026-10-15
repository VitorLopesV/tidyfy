package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link RemoveAllTasksUseCase} com o agendador falso.
 */
class RemoveAllTasksUseCaseTest {

    private FakeTaskScheduler scheduler;
    private RemoveAllTasksUseCase useCase;

    /** Cria o agendador falso e o caso de uso. */
    @BeforeEach
    void setUp() {
        scheduler = new FakeTaskScheduler();
        useCase = new RemoveAllTasksUseCase(scheduler);
    }

    private FolderOrganizerHelper addTask(String name) {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName(name),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), true,
                Path.of("C:\\Users\\nome\\" + name));
        scheduler.register(helper);
        return helper;
    }

    @Test
    void removesAllRegisteredTasks() {
        addTask("A");
        addTask("B");
        addTask("C");

        RemoveTasksResult result = useCase.execute();

        assertEquals(3, result.removed());
        assertTrue(result.succeeded());
        assertEquals(List.of(), scheduler.registered());
    }

    @Test
    void noTasksIsSuccessWithZeroRemoved() {
        RemoveTasksResult result = useCase.execute();

        assertEquals(0, result.removed());
        assertTrue(result.succeeded());
        assertEquals(List.of(), scheduler.unregisterCalls());
    }

    @Test
    void failureOnOneTaskDoesNotStopTheOthers() {
        FolderOrganizerHelper first = addTask("A");
        FolderOrganizerHelper second = addTask("B");
        FolderOrganizerHelper third = addTask("C");
        UncheckedIOException failure = new UncheckedIOException(new IOException("acesso negado"));
        scheduler.failNextUnregisterWith(failure);

        RemoveTasksResult result = useCase.execute();

        assertEquals(2, result.removed());
        assertEquals(1, result.failures().size());
        assertEquals(first.id(), result.failures().get(0).id());
        assertSame(failure, result.failures().get(0).error());
        assertEquals(List.of(first.id()), scheduler.registered());
        assertEquals(List.of(first.id(), second.id(), third.id()), scheduler.unregisterCalls());
    }

    @Test
    void failureToListTasksIsReportedWithoutId() {
        addTask("A");
        UncheckedIOException failure = new UncheckedIOException(new IOException("schtasks falhou"));
        scheduler.failNextRegisteredIdsWith(failure);

        RemoveTasksResult result = useCase.execute();

        assertEquals(0, result.removed());
        assertEquals(1, result.failures().size());
        assertNull(result.failures().get(0).id());
        assertSame(failure, result.failures().get(0).error());
        assertEquals(List.of(), scheduler.unregisterCalls());
    }

    @Test
    void nullSchedulerIsRejected() {
        assertThrows(NullPointerException.class, () -> new RemoveAllTasksUseCase(null));
    }
}
