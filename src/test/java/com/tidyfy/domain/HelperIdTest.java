package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link HelperId}.
 */
class HelperIdTest {

    @Test
    void newIdGeneratesDifferentIds() {
        assertNotEquals(HelperId.newId(), HelperId.newId());
    }

    @Test
    void keepsTheGivenUuid() {
        UUID uuid = UUID.randomUUID();

        assertEquals(uuid, new HelperId(uuid).value());
    }

    @Test
    void rejectsNullUuid() {
        assertThrows(IllegalArgumentException.class, () -> new HelperId(null));
    }
}
