package com.tidyfy.infrastructure.logging;

import com.tidyfy.domain.HelperId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes do {@link ExecutionLog}: formato das linhas, retenção, limite de tamanho e tolerância a falhas.
 */
class ExecutionLogTest {

    /** Instante fixo dos testes. */
    private static final Instant NOW = Instant.parse("2026-03-15T10:30:00Z");

    /** Pasta temporária de cada teste. */
    @TempDir
    Path temp;

    private static Clock clockAt(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static List<String> lines(Path file) throws IOException {
        return Files.readAllLines(file, StandardCharsets.UTF_8);
    }

    @Test
    void writesStartAndFinishToTheFileOfTheDay() throws IOException {
        Path logs = temp.resolve("logs");
        ExecutionLog log = new ExecutionLog(logs, clockAt(NOW));
        HelperId id = HelperId.newId();

        log.started(id);
        log.finished(id, 3);

        List<String> lines = lines(logs.resolve("tidyfy-2026-03-15.log"));
        assertEquals(2, lines.size());
        assertEquals("2026-03-15T10:30:00Z INFO [" + id.value() + "] Execução iniciada", lines.get(0));
        assertEquals("2026-03-15T10:30:00Z INFO [" + id.value() + "] Execução concluída: 3 ação(ões)", lines.get(1));
    }

    @Test
    void failureRecordsExceptionTypeMessageAndShortStack() throws IOException {
        Path logs = temp.resolve("logs");
        ExecutionLog log = new ExecutionLog(logs, clockAt(NOW));
        HelperId id = HelperId.newId();

        log.failed(id, new IllegalStateException("pasta apagada"));

        List<String> lines = lines(logs.resolve("tidyfy-2026-03-15.log"));
        assertTrue(lines.get(0).contains("ERROR [" + id.value() + "]"));
        assertTrue(lines.get(0).contains("java.lang.IllegalStateException: pasta apagada"));
        assertTrue(lines.size() > 1 && lines.size() <= 1 + ExecutionLog.STACK_LINES);
        assertTrue(lines.get(1).startsWith("\t"));
    }

    @Test
    void generalErrorIsWrittenWithoutHelper() throws IOException {
        Path logs = temp.resolve("logs");
        new ExecutionLog(logs, clockAt(NOW)).error("Argumentos inválidos", new IllegalArgumentException("x"));

        String first = lines(logs.resolve("tidyfy-2026-03-15.log")).get(0);
        assertTrue(first.contains("ERROR [-] Argumentos inválidos: java.lang.IllegalArgumentException: x"));
    }

    @Test
    void createsMissingLogsDirectory() {
        Path logs = temp.resolve("a").resolve("b").resolve("logs");

        new ExecutionLog(logs, clockAt(NOW)).started(HelperId.newId());

        assertTrue(Files.isDirectory(logs));
    }

    @Test
    void differentDaysGoToDifferentFiles() {
        Path logs = temp.resolve("logs");
        HelperId id = HelperId.newId();

        new ExecutionLog(logs, clockAt(NOW)).started(id);
        new ExecutionLog(logs, clockAt(NOW.plusSeconds(86400))).started(id);

        assertTrue(Files.exists(logs.resolve("tidyfy-2026-03-15.log")));
        assertTrue(Files.exists(logs.resolve("tidyfy-2026-03-16.log")));
    }

    @Test
    void deletesLogsOlderThanRetentionAndKeepsRecentAndForeignFiles() throws IOException {
        Path logs = Files.createDirectories(temp.resolve("logs"));
        Path old = Files.writeString(logs.resolve("tidyfy-2026-02-01.log"), "x");
        Path boundary = Files.writeString(logs.resolve("tidyfy-2026-02-13.log"), "x");
        Path recent = Files.writeString(logs.resolve("tidyfy-2026-03-10.log"), "x");
        Path foreign = Files.writeString(logs.resolve("notes-2020-01-01.log"), "x");
        Path other = Files.writeString(logs.resolve("tidyfy-2020-01-01.txt"), "x");
        Path badDate = Files.writeString(logs.resolve("tidyfy-abc.log"), "x");

        new ExecutionLog(logs, clockAt(NOW)).started(HelperId.newId());

        assertFalse(Files.exists(old));
        assertTrue(Files.exists(boundary));
        assertTrue(Files.exists(recent));
        assertTrue(Files.exists(foreign));
        assertTrue(Files.exists(other));
        assertTrue(Files.exists(badDate));
    }

    @Test
    void stopsAppendingAtSizeLimitAndRecordsLimitOnlyOnce() throws IOException {
        Path logs = Files.createDirectories(temp.resolve("logs"));
        Path file = logs.resolve("tidyfy-2026-03-15.log");
        Files.write(file, new byte[(int) ExecutionLog.MAX_FILE_BYTES - 10]);
        ExecutionLog log = new ExecutionLog(logs, clockAt(NOW));

        log.started(HelperId.newId());
        long afterFirst = Files.size(file);
        log.started(HelperId.newId());
        log.finished(HelperId.newId(), 1);

        assertEquals(afterFirst, Files.size(file));
        String content = Files.readString(file, StandardCharsets.UTF_8);
        assertEquals(1, content.split("limite do log do dia atingido", -1).length - 1);
        assertFalse(content.contains("Execução iniciada"));
    }

    @Test
    void ioFailureWhileWritingDoesNotThrow() throws IOException {
        Path notADirectory = Files.writeString(temp.resolve("logs"), "sou um arquivo");
        ExecutionLog log = new ExecutionLog(notADirectory, clockAt(NOW));
        HelperId id = HelperId.newId();

        assertDoesNotThrow(() -> {
            log.started(id);
            log.finished(id, 1);
            log.failed(id, new RuntimeException("falha"));
            log.error("erro", new RuntimeException("falha"));
        });
    }
}
