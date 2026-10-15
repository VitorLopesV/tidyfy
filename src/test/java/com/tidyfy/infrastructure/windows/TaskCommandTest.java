package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.HelperId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testa a montagem da linha de argumentos da tarefa agendada.
 */
class TaskCommandTest {

    private static final HelperId ID = new HelperId(UUID.fromString("3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10"));

    @Test
    void packagedCommandHasOnlyRunAndTheId() {
        assertEquals("--run 3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10",
                TaskCommand.of(Path.of("Tidyfy.exe")).argumentsFor(ID));
    }

    @Test
    void argumentsWithoutSpacesAreNotQuoted() {
        TaskCommand command = new TaskCommand(Path.of("javaw.exe"), List.of("-cp", "a.jar", "com.tidyfy.Main"));

        assertEquals("-cp a.jar com.tidyfy.Main --run 3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10",
                command.argumentsFor(ID));
    }

    @Test
    void embeddedQuotesAreEscaped() {
        TaskCommand command = new TaskCommand(Path.of("javaw.exe"), List.of("a\"b"));

        assertEquals("\"a\\\"b\" --run 3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10", command.argumentsFor(ID));
    }

    @Test
    void baseArgumentsAreCopied() {
        List<String> args = new ArrayList<>(List.of("x"));
        TaskCommand command = new TaskCommand(Path.of("javaw.exe"), args);
        args.add("y");

        assertEquals(List.of("x"), command.baseArguments());
    }

    @Test
    void nullExecutableIsRejected() {
        assertThrows(NullPointerException.class, () -> new TaskCommand(null, List.of()));
    }
}
