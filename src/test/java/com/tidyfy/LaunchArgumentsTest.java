package com.tidyfy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.HelperId;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link LaunchArguments}: a análise dos argumentos, sem abrir a interface.
 */
class LaunchArgumentsTest {

    /** UUID de exemplo. */
    private static final String ID = "3f2b8c1e-9d4a-4e7b-8a61-2c5d7e9f0a1b";

    @Test
    void noArgumentsOpensInterface() {
        LaunchArguments result = LaunchArguments.parse(new String[0]);

        assertEquals(LaunchArguments.Mode.INTERFACE, result.mode());
        assertNull(result.helperId());
        assertNull(result.error());
    }

    @Test
    void runWithUuidRunsTheHelper() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--run", ID});

        assertEquals(LaunchArguments.Mode.RUN, result.mode());
        assertEquals(new HelperId(UUID.fromString(ID)), result.helperId());
        assertNull(result.error());
    }

    @Test
    void runWithoutIdIsInvalid() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--run"});

        assertEquals(LaunchArguments.Mode.INVALID, result.mode());
        assertTrue(result.error().contains("--run <id>"));
    }

    @Test
    void runWithNonUuidIdIsInvalid() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--run", "abc"});

        assertEquals(LaunchArguments.Mode.INVALID, result.mode());
        assertTrue(result.error().contains("abc"));
    }

    @Test
    void unknownArgumentIsInvalid() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--foo"});

        assertEquals(LaunchArguments.Mode.INVALID, result.mode());
        assertTrue(result.error().contains("--foo"));
    }

    @Test
    void idWithoutRunFlagIsInvalid() {
        assertEquals(LaunchArguments.Mode.INVALID, LaunchArguments.parse(new String[] {ID}).mode());
    }

    @Test
    void removeTasksAloneIsTheRemoveTasksMode() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--remove-tasks"});

        assertEquals(LaunchArguments.Mode.REMOVE_TASKS, result.mode());
        assertNull(result.helperId());
        assertNull(result.error());
    }

    @Test
    void removeTasksWithExtraArgumentsIsInvalid() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--remove-tasks", "x"});

        assertEquals(LaunchArguments.Mode.INVALID, result.mode());
        assertTrue(result.error().contains("x"));
    }

    @Test
    void removeTasksAfterAnotherArgumentIsInvalid() {
        assertEquals(LaunchArguments.Mode.INVALID,
                LaunchArguments.parse(new String[] {"--run", "--remove-tasks"}).mode());
    }

    @Test
    void extraArgumentsAreInvalid() {
        LaunchArguments result = LaunchArguments.parse(new String[] {"--run", ID, "x", "y"});

        assertEquals(LaunchArguments.Mode.INVALID, result.mode());
        assertTrue(result.error().contains("x y"));
    }
}
