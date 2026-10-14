package com.tidyfy.infrastructure.windows;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Testes de {@link CommandResult}.
 */
class CommandResultTest {

    @Test
    void succeededIsTrueWhenExitCodeIsZero() {
        assertTrue(new CommandResult(0, "ok").succeeded());
    }

    @Test
    void succeededIsFalseWhenExitCodeIsNotZero() {
        assertFalse(new CommandResult(1, "erro").succeeded());
        assertFalse(new CommandResult(-1, "").succeeded());
    }

    @Test
    void rejectsNullOutput() {
        assertThrows(NullPointerException.class, () -> new CommandResult(0, null));
    }
}
