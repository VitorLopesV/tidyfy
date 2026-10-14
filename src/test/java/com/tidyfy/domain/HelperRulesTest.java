package com.tidyfy.domain;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes de {@link HelperRules}.
 */
class HelperRulesTest {

    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private final HelperRules rules = new HelperRules();

    private static Helper helper(String name) {
        return new FolderOrganizerHelper(HelperId.newId(), new HelperName(name), SCHEDULE, true, Path.of("pasta"));
    }

    @Test
    void rejectsNameAlreadyUsedByAnotherHelper() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Organizar Downloads"), List.of(existing)));
    }

    @Test
    void rejectsNameThatDiffersOnlyByCase() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("organizar downloads"), List.of(existing)));
    }

    @Test
    void rejectsNameThatDiffersOnlyBySurroundingSpaces() {
        Helper existing = helper("Organizar Downloads");

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("  Organizar Downloads "), List.of(existing)));
    }

    @Test
    void rejectsNameUsedByAnyOfTheOthers() {
        List<Helper> others = List.of(helper("Fotos"), helper("Limpeza"), helper("Documentos"));

        assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Limpeza"), others));
    }

    @Test
    void acceptsNameNotUsedByTheOthers() {
        List<Helper> others = List.of(helper("Fotos"), helper("Limpeza"));

        assertDoesNotThrow(() -> rules.assertCanSave(helper("Documentos"), others));
    }

    @Test
    void acceptsNameWhenThereAreNoOthers() {
        assertDoesNotThrow(() -> rules.assertCanSave(helper("Fotos"), List.of()));
    }

    @Test
    void acceptsOwnNameOnEditBecauseOthersExcludeTheHelperItself() {
        Helper edited = helper("Fotos");
        List<Helper> others = List.of(helper("Limpeza"));

        assertDoesNotThrow(() -> rules.assertCanSave(edited, others));
    }

    @Test
    void exceptionMessageMentionsTheName() {
        HelperNameAlreadyUsedException exception = assertThrows(HelperNameAlreadyUsedException.class,
                () -> rules.assertCanSave(helper("Fotos"), List.of(helper("fotos"))));

        assertTrue(exception.getMessage().contains("Fotos"));
    }

    @Test
    void nameAlreadyUsedExceptionIsADomainException() {
        assertInstanceOf(DomainException.class, new HelperNameAlreadyUsedException(new HelperName("Fotos")));
    }
}
