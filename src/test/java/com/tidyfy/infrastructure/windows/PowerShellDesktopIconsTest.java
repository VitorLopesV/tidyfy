package com.tidyfy.infrastructure.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.port.DesktopState;
import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.IconPosition;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes de {@link PowerShellDesktopIcons} com um executor de mentira; nenhum PowerShell real roda.
 */
class PowerShellDesktopIconsTest {

    private static final String HEADER =
            "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nAREA=0,0,1920,1008\r\nCELL=112,123\r\n";

    @TempDir
    Path tempDirectory;

    /** Comandos recebidos pelo executor, em ordem. */
    private final List<List<String>> calls = new ArrayList<>();

    /** Conteúdo do script no momento em que o executor foi chamado. */
    private final List<String> scriptsSeen = new ArrayList<>();

    /** Conteúdo do arquivo de posições no momento em que o executor foi chamado. */
    private final List<String> movesSeen = new ArrayList<>();

    /** Respostas a devolver, na ordem das chamadas. */
    private final Queue<CommandResult> responses = new LinkedList<>();

    private PowerShellDesktopIcons desktop;

    @BeforeEach
    void setUp() {
        desktop = new PowerShellDesktopIcons(command -> {
            calls.add(command);
            scriptsSeen.add(readIfPresent(command, "-File"));
            movesSeen.add(readIfPresent(command, "-MovesFile"));
            return responses.remove();
        }, tempDirectory);
    }

    private static String readIfPresent(List<String> command, String option) {
        int index = command.indexOf(option);
        if (index < 0) {
            return null;
        }
        try {
            return Files.readString(Path.of(command.get(index + 1)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String iconLine(String name, int x, int y) {
        String encoded = Base64.getEncoder().encodeToString(name.getBytes(StandardCharsets.UTF_8));
        return "ICON\t" + encoded + "\t" + x + "\t" + y + "\r\n";
    }

    private long filesInTempDirectory() throws IOException {
        try (Stream<Path> files = Files.list(tempDirectory)) {
            return files.count();
        }
    }

    @Test
    void readBuildsAHiddenNonInteractiveFileCommand() {
        responses.add(new CommandResult(0, HEADER));

        desktop.read();

        List<String> command = calls.get(0);
        assertEquals(List.of("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                "-ExecutionPolicy", "Bypass", "-File", command.get(8), "-Mode", "list"), command);
        assertTrue(command.get(8).endsWith(".ps1"));
        assertTrue(Path.of(command.get(8)).startsWith(tempDirectory));
    }

    @Test
    void theScriptHandedToPowerShellIsTheBundledOne() {
        responses.add(new CommandResult(0, HEADER));

        desktop.read();

        String script = scriptsSeen.get(0);
        assertTrue(script.contains("SysListView32"));
        assertTrue(script.contains("SetProcessDPIAware"));
        assertTrue(script.contains("LVM_SETITEMPOSITION"));
    }

    @Test
    void readParsesIconsAreaGridAndOptions() {
        responses.add(new CommandResult(0, "AUTOARRANGE=true\r\nSNAPTOGRID=true\r\nAREA=0,0,1920,1008\r\n"
                + "CELL=112,123\r\n" + iconLine("TLauncher", 827, 648) + iconLine("Programação", 32, 125)));

        DesktopState state = desktop.read();

        assertEquals(List.of(new DesktopIcon("TLauncher", new IconPosition(827, 648)),
                new DesktopIcon("Programação", new IconPosition(32, 125))), state.icons());
        assertEquals(new DesktopGrid(0, 0, 1920, 1008, 112, 123), state.grid());
        assertTrue(state.autoArrange());
        assertTrue(state.snapToGrid());
    }

    @Test
    void readAcceptsAnEmptyDesktopAndNegativeCoordinates() {
        responses.add(new CommandResult(0,
                "AUTOARRANGE=false\nSNAPTOGRID=false\nAREA=-1920,0,1920,1040\nCELL=96,96\n"));

        DesktopState state = desktop.read();

        assertTrue(state.icons().isEmpty());
        assertEquals(new DesktopGrid(-1920, 0, 1920, 1040, 96, 96), state.grid());
        assertFalse(state.autoArrange());
    }

    @Test
    void readIgnoresLinesItDoesNotKnow() {
        responses.add(new CommandResult(0, "aviso qualquer\r\n" + HEADER));

        assertTrue(desktop.read().icons().isEmpty());
    }

    @Test
    void readFailsWithTheScriptMessageWhenTheExitCodeIsNotZero() {
        responses.add(new CommandResult(1, "A area de trabalho nao esta disponivel (sem sessao grafica).\r\n"));

        UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> desktop.read());

        assertTrue(failure.getMessage().contains("sem sessao grafica"));
        assertTrue(failure.getMessage().contains("exit code 1"));
    }

    @Test
    void readFailsWhenARequiredLineIsMissing() {
        responses.add(new CommandResult(0, "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nCELL=112,123\r\n"));

        UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> desktop.read());

        assertTrue(failure.getMessage().contains("missing AREA="));
    }

    @Test
    void readFailsOnEveryMissingOrMalformedLine() {
        for (String output : List.of(
                "",
                "SNAPTOGRID=false\r\nAREA=0,0,10,10\r\nCELL=1,1\r\n",
                "AUTOARRANGE=false\r\nAREA=0,0,10,10\r\nCELL=1,1\r\n",
                "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nAREA=0,0,10,10\r\n",
                "AUTOARRANGE=talvez\r\nSNAPTOGRID=false\r\nAREA=0,0,10,10\r\nCELL=1,1\r\n",
                "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nAREA=0,0,10\r\nCELL=1,1\r\n",
                "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nAREA=0,0,x,10\r\nCELL=1,1\r\n",
                "AUTOARRANGE=false\r\nSNAPTOGRID=false\r\nAREA=0,0,10,10\r\nCELL=0,0\r\n",
                HEADER + "ICON\tbm9tZQ==\t1\r\n",
                HEADER + "ICON\t%%%\t1\t2\r\n",
                HEADER + "ICON\t\t1\t2\r\n")) {
            responses.add(new CommandResult(0, output));
            assertThrows(UncheckedIOException.class, () -> desktop.read(), output);
        }
    }

    @Test
    void readRemovesTheTemporaryScript() throws IOException {
        responses.add(new CommandResult(0, HEADER));
        desktop.read();
        assertEquals(0, filesInTempDirectory());

        responses.add(new CommandResult(1, "erro"));
        assertThrows(UncheckedIOException.class, () -> desktop.read());
        assertEquals(0, filesInTempDirectory());
    }

    @Test
    void moveWritesOneLinePerIconAndPassesTheFileToTheScript() {
        responses.add(new CommandResult(0, "MOVED=2\r\n"));

        desktop.move(List.of(new DesktopIcon("Programação", new IconPosition(10, 20)),
                new DesktopIcon("Steam", new IconPosition(0, 143))));

        List<String> command = calls.get(0);
        assertEquals("-Mode", command.get(9));
        assertEquals("move", command.get(10));
        assertEquals("-MovesFile", command.get(11));
        assertEquals("Programação\t10\t20\nSteam\t0\t143\n", movesSeen.get(0));
    }

    @Test
    void moveRemovesTheTemporaryFiles() throws IOException {
        responses.add(new CommandResult(0, "MOVED=1\r\n"));

        desktop.move(List.of(new DesktopIcon("A", new IconPosition(1, 2))));

        assertEquals(0, filesInTempDirectory());
    }

    @Test
    void moveFailsAndCleansUpWhenTheScriptFails() throws IOException {
        responses.add(new CommandResult(1, "Icone nao encontrado na area de trabalho: A\r\n"));

        UncheckedIOException failure = assertThrows(UncheckedIOException.class,
                () -> desktop.move(List.of(new DesktopIcon("A", new IconPosition(1, 2)))));

        assertTrue(failure.getMessage().contains("Icone nao encontrado"));
        assertEquals(0, filesInTempDirectory());
    }

    @Test
    void moveWithNoPlacementsDoesNotRunAnything() {
        desktop.move(List.of());

        assertTrue(calls.isEmpty());
    }

    @Test
    void moveRefusesNamesThatWouldBreakTheMovesFile() {
        assertThrows(IllegalArgumentException.class,
                () -> desktop.move(List.of(new DesktopIcon("a\tb", new IconPosition(1, 2)))));
        assertThrows(IllegalArgumentException.class,
                () -> desktop.move(List.of(new DesktopIcon("a\nb", new IconPosition(1, 2)))));
        assertThrows(IllegalArgumentException.class,
                () -> desktop.move(List.of(new DesktopIcon("a\rb", new IconPosition(1, 2)))));
        assertTrue(calls.isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new PowerShellDesktopIcons(null, tempDirectory));
        assertThrows(NullPointerException.class, () -> new PowerShellDesktopIcons(command -> null, null));
        assertThrows(NullPointerException.class, () -> desktop.move(null));
    }
}
