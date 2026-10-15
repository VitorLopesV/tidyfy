package com.tidyfy.infrastructure.windows;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.HelperId;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa a listagem de tarefas do {@link SchtasksTaskScheduler} com um {@link CommandRunner} falso:
 * o {@code schtasks} real nunca é executado.
 */
class SchtasksTaskSchedulerRegisteredIdsTest {

    private final HelperId first = HelperId.newId();
    private final HelperId second = HelperId.newId();
    private final List<List<String>> commands = new ArrayList<>();
    private CommandResult result = new CommandResult(0, "");

    private final SchtasksTaskScheduler scheduler = new SchtasksTaskScheduler(command -> {
        commands.add(command);
        return result;
    }, TaskCommand.of(Path.of("Tidyfy.exe")), Path.of("."));

    private static String line(String name) {
        return "\"" + name + "\",\"N/A\",\"Ready\"";
    }

    @Test
    void buildsTheListCommand() {
        assertEquals(List.of("schtasks", "/Query", "/FO", "CSV", "/NH"), scheduler.listCommand());
    }

    @Test
    void listsOnlyTheTidyfyTasksWithAHelperId() {
        result = new CommandResult(0, String.join("\r\n",
                line("\\Microsoft\\Windows\\Defrag\\ScheduledDefrag"),
                line("\\Tidyfy\\" + first.value()),
                line("\\OutraPasta\\" + second.value()),
                line("\\Tidyfy\\" + second.value()),
                ""));

        assertEquals(List.of(first, second), scheduler.registeredIds());
        assertEquals(List.of(scheduler.listCommand()), commands);
    }

    @Test
    void ignoresTasksWhoseNameIsNotAUuid() {
        result = new CommandResult(0, String.join("\n",
                line("\\Tidyfy\\manual"),
                line("\\Tidyfy\\" + first.value() + "\\1"),
                line("\\Tidyfy\\1-1-1-1-1"),
                line("\\Tidyfy\\" + second.value())));

        assertEquals(List.of(second), scheduler.registeredIds());
    }

    @Test
    void toleratesInvalidAndLocalizedLines() {
        result = new CommandResult(0, String.join("\n",
                "",
                "INFO: Nao ha tarefas agendadas disponiveis no momento.",
                "\"",
                "\"\\Tidyfy\\" + first.value(),
                "NomeDaTarefa,Proxima,Status",
                line("\\tidyfy\\" + second.value().toString().toUpperCase())));

        assertEquals(List.of(second), scheduler.registeredIds());
    }

    @Test
    void noTasksIsNotAnError() {
        result = new CommandResult(0, "INFO: There are no scheduled tasks presently available.");

        assertTrue(scheduler.registeredIds().isEmpty());
    }

    @Test
    void doesNotRepeatAnId() {
        result = new CommandResult(0, line("\\Tidyfy\\" + first.value()) + "\n" + line("\\Tidyfy\\" + first.value()));

        assertEquals(List.of(first), scheduler.registeredIds());
    }

    @Test
    void failsWhenTheCommandFails() {
        result = new CommandResult(1, "ERROR: Access is denied.");

        UncheckedIOException error = assertThrows(UncheckedIOException.class, scheduler::registeredIds);

        assertTrue(error.getMessage().contains("Access is denied"));
    }
}
