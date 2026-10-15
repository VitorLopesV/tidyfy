package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeTaskScheduler;
import com.tidyfy.application.usecase.RemoveAllTasksUseCase;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.logging.ExecutionLog;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link RemoveTasksRunner}: códigos de saída e linhas do log, com agendador falso e log real em
 * pasta temporária. Nunca toca no Agendador do Windows nem no AppData reais.
 */
class RemoveTasksRunnerTest {

    /** Instante fixo do relógio. */
    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    /** Pasta de logs temporária. */
    @TempDir
    Path logs;

    private FakeTaskScheduler scheduler;
    private ByteArrayOutputStream errBytes;
    private RemoveTasksRunner runner;

    /** Monta o executor com o agendador falso, relógio fixo e log real. */
    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        scheduler = new FakeTaskScheduler();
        errBytes = new ByteArrayOutputStream();
        runner = new RemoveTasksRunner(() -> new RemoveAllTasksUseCase(scheduler), new ExecutionLog(logs, clock),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));
    }

    private HelperId addTask(String name) {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName(name),
                new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1)), true,
                Path.of("C:\\Users\\nome\\" + name));
        scheduler.register(helper);
        return helper.id();
    }

    private String logContent() throws IOException {
        try (Stream<Path> files = Files.list(logs)) {
            return files.map(file -> {
                try {
                    return Files.readString(file);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }).collect(Collectors.joining());
        }
    }

    private String err() {
        return errBytes.toString(StandardCharsets.UTF_8);
    }

    @Test
    void removesTasksReturnsZeroAndLogsCount() throws IOException {
        addTask("A");
        addTask("B");

        int code = runner.run();

        assertEquals(0, code);
        assertEquals(List.of(), scheduler.registered());
        String log = logContent();
        assertTrue(log.contains("Remoção das tarefas do agendador iniciada"));
        assertTrue(log.contains("2 tarefa(s) removida(s), 0 falha(s)"));
        assertEquals("", err());
    }

    @Test
    void noTasksReturnsZeroAndLogsZeroRemoved() throws IOException {
        int code = runner.run();

        assertEquals(0, code);
        assertTrue(logContent().contains("0 tarefa(s) removida(s), 0 falha(s)"));
    }

    @Test
    void failureOnOneTaskReturnsOneAndLogsIt() throws IOException {
        HelperId failing = addTask("A");
        addTask("B");
        scheduler.failNextUnregisterWith(new UncheckedIOException(new IOException("acesso negado")));

        int code = runner.run();

        assertEquals(1, code);
        String log = logContent();
        assertTrue(log.contains("1 tarefa(s) removida(s), 1 falha(s)"));
        assertTrue(log.contains("Falha ao remover a tarefa " + failing.value()));
        assertTrue(log.contains("acesso negado"));
        assertTrue(err().contains("1 falha(s)"));
    }

    @Test
    void failureToListTasksReturnsOneAndLogsIt() throws IOException {
        scheduler.failNextRegisteredIdsWith(new UncheckedIOException(new IOException("schtasks indisponível")));

        int code = runner.run();

        assertEquals(1, code);
        String log = logContent();
        assertTrue(log.contains("Falha ao listar as tarefas"));
        assertTrue(log.contains("schtasks indisponível"));
    }

    @Test
    void unexpectedExceptionReturnsOneAndLogsIt() throws IOException {
        RemoveTasksRunner failing = new RemoveTasksRunner(() -> {
            throw new IllegalStateException("sem container");
        }, new ExecutionLog(logs, Clock.fixed(NOW, ZoneOffset.UTC)),
                new PrintStream(errBytes, true, StandardCharsets.UTF_8));

        assertEquals(1, failing.run());
        assertTrue(logContent().contains("sem container"));
    }
}
