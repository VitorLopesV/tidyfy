package com.tidyfy.infrastructure.windows;

import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de {@link PowerShellRecycleBin#emptyAll()} com um executor de mentira; nenhum PowerShell real roda.
 */
class PowerShellRecycleBinEmptyAllTest {

    /** Comandos recebidos pelo executor, em ordem. */
    private final List<List<String>> calls = new ArrayList<>();

    /** Respostas a devolver, na ordem das chamadas. */
    private final Queue<CommandResult> responses = new LinkedList<>();

    /** Lixeira sob teste. */
    private final PowerShellRecycleBin bin = new PowerShellRecycleBin(command -> {
        calls.add(command);
        return responses.remove();
    });

    @Test
    void buildsTheCountCommandAsArgumentList() {
        List<String> command = bin.countCommand();

        assertEquals(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-Command", command.get(6)), command);
        assertTrue(command.get(6).contains("Shell.Application"));
        assertTrue(command.get(6).contains("Items().Count"));
    }

    @Test
    void buildsTheClearCommandWithForce() {
        List<String> command = bin.clearCommand();

        assertEquals("powershell.exe", command.get(0));
        assertEquals("-Command", command.get(5));
        assertEquals("Clear-RecycleBin -Force -ErrorAction SilentlyContinue", command.get(6));
    }

    @Test
    void returnsTheNumberOfItemsThatWereInTheBin() {
        responses.add(new CommandResult(0, "7\r\n"));
        responses.add(new CommandResult(0, ""));

        assertEquals(7, bin.emptyAll());
    }

    @Test
    void countsFirstAndThenClears() {
        responses.add(new CommandResult(0, "3"));
        responses.add(new CommandResult(0, ""));

        bin.emptyAll();

        assertEquals(List.of(bin.countCommand(), bin.clearCommand()), calls);
    }

    @Test
    void emptyBinReturnsZeroWithoutClearingAndWithoutError() {
        responses.add(new CommandResult(0, "0\r\n"));

        assertEquals(0, bin.emptyAll());
        assertEquals(List.of(bin.countCommand()), calls);
    }

    @Test
    void usesTheLastNonBlankLineOfTheOutput() {
        responses.add(new CommandResult(0, "aviso qualquer\r\n 5 \r\n\r\n"));
        responses.add(new CommandResult(0, ""));

        assertEquals(5, bin.emptyAll());
    }

    @Test
    void invalidCountOutputThrowsWithTheOutputInTheMessage() {
        responses.add(new CommandResult(0, "oops"));

        UncheckedIOException e = assertThrows(UncheckedIOException.class, bin::emptyAll);

        assertTrue(e.getMessage().contains("oops"));
        assertEquals(1, calls.size());
    }

    @Test
    void emptyOutputThrows() {
        responses.add(new CommandResult(0, ""));

        assertThrows(UncheckedIOException.class, bin::emptyAll);
    }

    @Test
    void nonZeroExitOnCountThrowsWithTheOutputInTheMessage() {
        responses.add(new CommandResult(1, "falhou feio"));

        UncheckedIOException e = assertThrows(UncheckedIOException.class, bin::emptyAll);

        assertTrue(e.getMessage().contains("falhou feio"));
    }

    @Test
    void nonZeroExitOnClearThrows() {
        responses.add(new CommandResult(0, "2"));
        responses.add(new CommandResult(1, "sem permissão"));

        UncheckedIOException e = assertThrows(UncheckedIOException.class, bin::emptyAll);

        assertTrue(e.getMessage().contains("sem permissão"));
    }

    @Test
    void emptyOlderThanIsNotImplementedYet() {
        assertThrows(UnsupportedOperationException.class, () -> bin.emptyOlderThan(Instant.EPOCH));
    }
}
