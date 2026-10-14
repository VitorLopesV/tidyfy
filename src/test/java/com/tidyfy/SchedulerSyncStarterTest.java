package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.SyncSchedulerUseCase;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.logging.ExecutionLog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link SchedulerSyncStarter}: a sincronização roda em segundo plano e nenhuma falha escapa,
 * só vai para o log (pasta temporária).
 */
class SchedulerSyncStarterTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-30T12:00:00Z"), ZoneOffset.UTC);

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    @TempDir
    Path logs;

    private String logContent() throws IOException {
        if (!Files.exists(logs)) {
            return "";
        }
        try (Stream<Path> files = Files.list(logs)) {
            return files.map(file -> {
                try {
                    return Files.readString(file);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }).collect(Collectors.joining());
        }
    }

    @Test
    void synchronizesInTheBackgroundOnADaemonThread() throws Exception {
        FakeHelperRepository repository = new FakeHelperRepository();
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        Helper helper = new FolderOrganizerHelper(
                HelperId.newId(), new HelperName("A"), SCHEDULE, true, Path.of("C:\\A"));
        repository.with(helper);
        SchedulerSyncStarter starter = new SchedulerSyncStarter(
                () -> new SyncSchedulerUseCase(repository, scheduler), new ExecutionLog(logs, CLOCK));

        Thread thread = starter.start();
        thread.join(5000);

        assertTrue(thread.isDaemon());
        assertTrue(scheduler.isRegistered(helper.id()));
    }

    @Test
    void logsTheFailuresOfIndividualTasks() throws Exception {
        FakeHelperRepository repository = new FakeHelperRepository();
        FakeTaskScheduler scheduler = new FakeTaskScheduler();
        repository.with(new FolderOrganizerHelper(
                HelperId.newId(), new HelperName("A"), SCHEDULE, true, Path.of("C:\\A")));
        scheduler.failNextRegisterWith(new IllegalStateException("denied"));
        SchedulerSyncStarter starter = new SchedulerSyncStarter(
                () -> new SyncSchedulerUseCase(repository, scheduler), new ExecutionLog(logs, CLOCK));

        starter.run();

        String content = logContent();
        assertTrue(content.contains("ERROR"));
        assertTrue(content.contains("denied"));
    }

    @Test
    void aFailureBuildingTheUseCaseIsLoggedAndNeverEscapes() throws Exception {
        SchedulerSyncStarter starter = new SchedulerSyncStarter(() -> {
            throw new IllegalStateException("no container");
        }, new ExecutionLog(logs, CLOCK));

        assertDoesNotThrow(starter::run);

        assertTrue(logContent().contains("no container"));
    }

    @Test
    void aSuccessfulSyncWritesNothingToTheLog() throws Exception {
        SchedulerSyncStarter starter = new SchedulerSyncStarter(
                () -> new SyncSchedulerUseCase(new FakeHelperRepository(), new FakeTaskScheduler()),
                new ExecutionLog(logs, CLOCK));

        starter.run();

        assertEquals("", logContent());
    }
}
