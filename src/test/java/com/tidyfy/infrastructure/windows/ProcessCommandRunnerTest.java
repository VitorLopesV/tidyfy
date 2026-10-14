package com.tidyfy.infrastructure.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link ProcessCommandRunner}. Nenhum processo real é iniciado: o iniciador é
 * substituído por um {@link FakeProcess}.
 */
class ProcessCommandRunnerTest {

    /** Tempo limite usado nos testes. */
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Processo falso com saída, código de saída e término configuráveis.
     */
    private static final class FakeProcess extends Process {

        private final byte[] output;
        private final int exitCode;
        private final boolean finishes;
        private boolean destroyed;
        private long waitedMillis = -1;

        FakeProcess(byte[] output, int exitCode, boolean finishes) {
            this.output = output;
            this.exitCode = exitCode;
            this.finishes = finishes;
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(output);
        }

        @Override
        public InputStream getErrorStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public int waitFor() {
            return exitCode;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) {
            waitedMillis = unit.toMillis(timeout);
            return finishes;
        }

        @Override
        public int exitValue() {
            return exitCode;
        }

        @Override
        public void destroy() {
            destroyed = true;
        }

        @Override
        public Process destroyForcibly() {
            destroyed = true;
            return this;
        }
    }

    private static ProcessCommandRunner runnerWith(FakeProcess process) {
        return new ProcessCommandRunner(TIMEOUT, StandardCharsets.UTF_8, command -> process);
    }

    @Test
    void readsOutputAndSuccessfulExitCode() {
        FakeProcess process = new FakeProcess("saida".getBytes(StandardCharsets.UTF_8), 0, true);

        CommandResult result = runnerWith(process).run(List.of("prog"));

        assertTrue(result.succeeded());
        assertEquals("saida", result.output());
        assertFalse(process.destroyed);
        assertEquals(TIMEOUT.toMillis(), process.waitedMillis);
    }

    @Test
    void returnsNonZeroExitCode() {
        FakeProcess process = new FakeProcess("erro".getBytes(StandardCharsets.UTF_8), 3, true);

        CommandResult result = runnerWith(process).run(List.of("prog"));

        assertEquals(3, result.exitCode());
        assertFalse(result.succeeded());
        assertEquals("erro", result.output());
    }

    @Test
    void passesTheCommandToTheStarter() {
        FakeProcess process = new FakeProcess(new byte[0], 0, true);
        List<List<String>> received = new java.util.ArrayList<>();
        var runner = new ProcessCommandRunner(TIMEOUT, StandardCharsets.UTF_8, command -> {
            received.add(command);
            return process;
        });

        runner.run(List.of("a", "b"));

        assertEquals(List.of(List.of("a", "b")), received);
    }

    @Test
    void readsOutputWithConfiguredCharset() {
        FakeProcess process = new FakeProcess("ação".getBytes(StandardCharsets.ISO_8859_1), 0, true);
        var runner = new ProcessCommandRunner(TIMEOUT, StandardCharsets.ISO_8859_1, command -> process);

        assertEquals("ação", runner.run(List.of("prog")).output());
    }

    @Test
    void destroysProcessAndThrowsWhenTimeoutExpires() {
        FakeProcess process = new FakeProcess(new byte[0], 0, false);

        UncheckedIOException e = assertThrows(UncheckedIOException.class,
                () -> runnerWith(process).run(List.of("prog")));

        assertTrue(process.destroyed);
        assertTrue(e.getMessage().contains("timed out"));
    }

    @Test
    void throwsWhenProcessCannotBeStarted() {
        var runner = new ProcessCommandRunner(TIMEOUT, StandardCharsets.UTF_8, command -> {
            throw new IOException("boom");
        });

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> runner.run(List.of("prog")));

        assertTrue(e.getMessage().contains("Could not start"));
        assertEquals("boom", e.getCause().getMessage());
    }

    @Test
    void rejectsEmptyCommand() {
        FakeProcess process = new FakeProcess(new byte[0], 0, true);

        assertThrows(IllegalArgumentException.class, () -> runnerWith(process).run(List.of()));
    }

    @Test
    void rejectsNonPositiveTimeout() {
        assertThrows(IllegalArgumentException.class, () -> new ProcessCommandRunner(Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> new ProcessCommandRunner(Duration.ofSeconds(-1)));
    }

    @Test
    void consoleCharsetIsAlwaysAvailable() {
        Charset charset = ProcessCommandRunner.consoleCharset();

        assertNotNull(charset);
    }

    @Test
    void commandRunnerCanBeReplacedByFake() {
        CommandRunner fake = command -> new CommandResult(0, String.join(" ", command));

        assertEquals("a b", fake.run(List.of("a", "b")).output());
    }
}
