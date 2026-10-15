package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.HelperId;
import org.junit.jupiter.api.Test;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa a remoção de tarefas do {@link SchtasksTaskScheduler} com um {@link CommandRunner} falso:
 * o {@code schtasks} real nunca é executado.
 */
class SchtasksTaskSchedulerUnregisterTest {

    private final HelperId id = HelperId.newId();
    private final List<List<String>> commands = new ArrayList<>();
    private CommandResult deleteResult = new CommandResult(0, "SUCCESS");
    private CommandResult queryResult = new CommandResult(1, "ERROR: not found");

    private final SchtasksTaskScheduler scheduler = new SchtasksTaskScheduler(command -> {
        commands.add(command);
        return command.get(1).equals("/Delete") ? deleteResult : queryResult;
    }, TaskCommand.of(Path.of("Tidyfy.exe")), Path.of("."));

    @Test
    void buildsTheDeleteCommandForTheHelperTask() {
        assertEquals(List.of("schtasks", "/Delete", "/TN", "Tidyfy\\" + id.value(), "/F"),
                scheduler.deleteCommand(id));
    }

    @Test
    void buildsTheQueryCommandForTheHelperTask() {
        assertEquals(List.of("schtasks", "/Query", "/TN", "Tidyfy\\" + id.value()), scheduler.queryCommand(id));
    }

    @Test
    void onlyDeletesWhenTheDeleteSucceeds() {
        scheduler.unregister(id);

        assertEquals(List.of(scheduler.deleteCommand(id)), commands);
    }

    @Test
    void ignoresATaskThatDoesNotExist() {
        deleteResult = new CommandResult(1, "ERRO: O sistema nao pode encontrar o arquivo especificado.");

        scheduler.unregister(id);

        assertEquals(List.of(scheduler.deleteCommand(id), scheduler.queryCommand(id)), commands);
    }

    @Test
    void failsWithAUsefulMessageWhenTheTaskStillExists() {
        deleteResult = new CommandResult(1, "ERROR: Access is denied.");
        queryResult = new CommandResult(0, "Tidyfy\\" + id.value());

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> scheduler.unregister(id));

        assertTrue(e.getMessage().contains(id.value().toString()));
        assertTrue(e.getMessage().contains("exit code 1"));
        assertTrue(e.getMessage().contains("Access is denied"));
        assertEquals(List.of(scheduler.deleteCommand(id), scheduler.queryCommand(id)), commands);
    }
}
