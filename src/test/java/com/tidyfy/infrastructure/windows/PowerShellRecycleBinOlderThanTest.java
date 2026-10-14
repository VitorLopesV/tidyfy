package com.tidyfy.infrastructure.windows;

import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de {@link PowerShellRecycleBin#emptyOlderThan(Instant)} com um executor de mentira; nenhum
 * PowerShell real roda.
 */
class PowerShellRecycleBinOlderThanTest {

    /** Corte usado nos testes. */
    private static final Instant CUTOFF = Instant.parse("2024-05-01T10:15:30Z");

    /** Comandos recebidos pelo executor, em ordem. */
    private final List<List<String>> calls = new ArrayList<>();

    /** Resposta que o executor devolve. */
    private CommandResult response = new CommandResult(0, "0");

    /** Lixeira sob teste. */
    private final PowerShellRecycleBin bin = new PowerShellRecycleBin(command -> {
        calls.add(command);
        return response;
    });

    /** Devolve o script (último argumento) do comando. */
    private static String script(List<String> command) {
        return command.get(command.size() - 1);
    }

    @Test
    void buildsTheCommandAsArgumentListWithTheIsoCutoff() {
        List<String> command = bin.olderThanCommand(CUTOFF);

        assertEquals(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                "-Command", script(command)), command);
        assertTrue(script(command).contains("'2024-05-01T10:15:30.0000000Z'"));
        assertTrue(script(command).contains("System.Recycle.DateDeleted"));
        assertTrue(script(command).contains("Shell.Application"));
    }

    @Test
    void comparesStrictlyAndRemovesPermanently() {
        String script = script(bin.olderThanCommand(CUTOFF));

        assertTrue(script.contains("$deleted -lt $cutoff"));
        assertTrue(script.contains("Remove-Item -LiteralPath $path -Recurse -Force"));
        assertTrue(script.contains("Write-Output $removed"));
    }

    @Test
    void treatsUnspecifiedKindAsLocalTimeAndConvertsToUtc() {
        String script = script(bin.olderThanCommand(CUTOFF));

        int unspecified = script.indexOf("DateTimeKind]::Unspecified");
        int specifyLocal = script.indexOf("SpecifyKind($deleted, [System.DateTimeKind]::Local)");
        int toUtc = script.indexOf("$deleted.ToUniversalTime()");
        int compare = script.indexOf("$deleted -lt $cutoff");

        assertTrue(unspecified >= 0 && unspecified < specifyLocal);
        assertTrue(specifyLocal < toUtc);
        assertTrue(toUtc < compare);
    }

    @Test
    void neverReinterpretsTheDeletionDateAsUtc() {
        assertTrue(!script(bin.olderThanCommand(CUTOFF)).contains("DateTimeKind]::Utc)"));
    }

    @Test
    void keepsTheFractionOfTheCutoffInTheScript() {
        Instant cutoff = Instant.parse("2024-05-01T10:15:30.123456700Z");

        assertTrue(script(bin.olderThanCommand(cutoff)).contains("'2024-05-01T10:15:30.1234567Z'"));
    }

    @Test
    void roundsSubTickPrecisionUpSoTheStrictComparisonStaysExact() {
        Instant cutoff = Instant.parse("2024-05-01T10:15:30.123456701Z");

        assertTrue(script(bin.olderThanCommand(cutoff)).contains("'2024-05-01T10:15:30.1234568Z'"));
    }

    @Test
    void returnsTheNumberOfRemovedItems() {
        response = new CommandResult(0, "4\r\n");

        assertEquals(4, bin.emptyOlderThan(CUTOFF));
    }

    @Test
    void returnsZeroWhenNothingIsOlder() {
        response = new CommandResult(0, "0");

        assertEquals(0, bin.emptyOlderThan(CUTOFF));
    }

    @Test
    void runsExactlyOneCommand() {
        response = new CommandResult(0, "2");

        bin.emptyOlderThan(CUTOFF);

        assertEquals(List.of(bin.olderThanCommand(CUTOFF)), calls);
    }

    @Test
    void usesTheLastNonBlankLineOfTheOutput() {
        response = new CommandResult(0, "aviso\r\n 6 \r\n\r\n");

        assertEquals(6, bin.emptyOlderThan(CUTOFF));
    }

    @Test
    void invalidOutputThrowsWithTheOutputInTheMessage() {
        response = new CommandResult(0, "oops");

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> bin.emptyOlderThan(CUTOFF));

        assertTrue(e.getMessage().contains("oops"));
    }

    @Test
    void emptyOutputThrows() {
        response = new CommandResult(0, "");

        assertThrows(UncheckedIOException.class, () -> bin.emptyOlderThan(CUTOFF));
    }

    @Test
    void nonZeroExitThrowsWithTheOutputInTheMessage() {
        response = new CommandResult(1, "falhou feio");

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> bin.emptyOlderThan(CUTOFF));

        assertTrue(e.getMessage().contains("falhou feio"));
    }

    @Test
    void nullCutoffIsRejected() {
        assertThrows(NullPointerException.class, () -> bin.emptyOlderThan(null));
    }
}
