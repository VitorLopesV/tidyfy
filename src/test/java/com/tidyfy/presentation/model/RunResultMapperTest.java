package com.tidyfy.presentation.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa a conversão do registro de uma execução manual no resultado exibido.
 */
class RunResultMapperTest {

    private static final Instant NOW = Instant.parse("2026-06-30T12:00:00Z");

    private static ExecutionRecord record(HelperAction... actions) {
        return new ExecutionRecord(HelperId.newId(), "Organizar", NOW, List.of(actions));
    }

    private static HelperAction moved(String name) {
        return new HelperAction(ActionType.MOVED, Path.of("C:\\a\\" + name), Path.of("C:\\a\\Imagens\\" + name), null);
    }

    @Test
    void recordWithoutActionsIsASuccessWithNothingToDo() {
        RunResult result = RunResultMapper.toResult("Organizar", record());

        assertTrue(result.success());
        assertEquals("Execução concluída", result.title());
        assertEquals("Organizar rodou, mas não havia nada a fazer.", result.message());
        assertNull(result.details());
    }

    @Test
    void recordWithOneActionSaysOneAction() {
        RunResult result = RunResultMapper.toResult("Organizar", record(moved("a.png")));

        assertTrue(result.success());
        assertEquals("Organizar rodou e fez 1 ação. Os detalhes estão no histórico.", result.message());
    }

    @Test
    void recordWithManyActionsCountsThem() {
        RunResult result = RunResultMapper.toResult("Organizar", record(moved("a.png"), moved("b.png")));

        assertEquals("Organizar rodou e fez 2 ações. Os detalhes estão no histórico.", result.message());
    }

    @Test
    void failedActionMakesTheResultAFailureWithDetails() {
        RunResult result = RunResultMapper.toResult("Organizar", record(moved("a.png"),
                new HelperAction(ActionType.FAILED, Path.of("C:\\a\\b.zip"), null, "Acesso negado"),
                new HelperAction(ActionType.FAILED, null, null, "Pasta apagada")));

        assertFalse(result.success());
        assertEquals("A execução falhou", result.title());
        assertEquals("C:\\a\\b.zip: Acesso negado\nPasta apagada", result.details());
    }

    @Test
    void missingHelperIsAFailure() {
        RunResult result = RunResultMapper.missing("Organizar");

        assertFalse(result.success());
        assertEquals("Ajudante não encontrado", result.title());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> RunResultMapper.toResult(null, record()));
        assertThrows(NullPointerException.class, () -> RunResultMapper.toResult("x", null));
        assertThrows(NullPointerException.class, () -> RunResultMapper.missing(null));
    }
}
