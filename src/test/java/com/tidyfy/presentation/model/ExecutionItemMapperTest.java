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
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testa a conversão dos registros do histórico nos itens da tela.
 */
class ExecutionItemMapperTest {

    private static final Instant NOON_UTC = Instant.parse("2026-03-05T12:30:00Z");

    private final ExecutionItemMapper mapper = new ExecutionItemMapper(ZoneId.of("UTC"));

    @Test
    void formatsDateAndTimeInTheGivenZone() {
        ExecutionItem utc = mapper.map(record(NOON_UTC));
        ExecutionItem saoPaulo = new ExecutionItemMapper(ZoneId.of("America/Sao_Paulo")).map(record(NOON_UTC));

        assertEquals("05/03/2026 12:30", utc.executedAt());
        assertEquals("05/03/2026 09:30", saoPaulo.executedAt());
    }

    @Test
    void movedActionShowsSourceNameAndDestinationFolder() {
        HelperAction moved = new HelperAction(ActionType.MOVED, Path.of("C:\\Downloads\\foto.jpg"),
                Path.of("C:\\Downloads\\Imagens\\foto.jpg"), null);

        ExecutionAction action = mapper.map(record(NOON_UTC, moved)).actions().get(0);

        assertEquals("foto.jpg \u2192 Imagens\\foto.jpg", action.text());
        assertEquals(moved.source() + " → " + moved.target(), action.detail());
        assertFalse(action.failure());
    }

    @Test
    void trashedAndDuplicateActionsShowTheFileName() {
        HelperAction trashed = new HelperAction(ActionType.TRASHED, Path.of("C:\\Temp\\velho.log"), null, null);
        HelperAction duplicate = new HelperAction(ActionType.DUPLICATE_TRASHED, Path.of("C:\\Downloads\\a.pdf"), null,
                null);

        List<ExecutionAction> actions = mapper.map(record(NOON_UTC, trashed, duplicate)).actions();

        assertEquals("Enviado à lixeira: velho.log", actions.get(0).text());
        assertEquals("Duplicado enviado à lixeira: a.pdf", actions.get(1).text());
        assertFalse(actions.get(0).failure());
        assertFalse(actions.get(1).failure());
    }

    @Test
    void recycleBinEmptiedShowsTheQuantityMessage() {
        HelperAction emptied = new HelperAction(ActionType.RECYCLE_BIN_EMPTIED, null, null, "12 itens removidos");
        HelperAction noMessage = new HelperAction(ActionType.RECYCLE_BIN_EMPTIED, null, null, null);

        List<ExecutionAction> actions = mapper.map(record(NOON_UTC, emptied, noMessage)).actions();

        assertEquals("Lixeira esvaziada: 12 itens removidos", actions.get(0).text());
        assertEquals("Lixeira esvaziada", actions.get(1).text());
        assertNull(actions.get(0).detail());
    }

    @Test
    void failureShowsTheMessageAndIsFlagged() {
        HelperAction failed = new HelperAction(ActionType.FAILED, Path.of("C:\\Temp\\preso.txt"), null,
                "Acesso negado");
        HelperAction general = new HelperAction(ActionType.FAILED, null, null, "A pasta não existe mais");
        HelperAction noMessage = new HelperAction(ActionType.FAILED, null, null, null);

        ExecutionItem item = mapper.map(record(NOON_UTC, failed, general, noMessage));

        assertEquals("Falha em preso.txt: Acesso negado", item.actions().get(0).text());
        assertEquals("Falha: A pasta não existe mais", item.actions().get(1).text());
        assertEquals("Falha: motivo desconhecido", item.actions().get(2).text());
        assertTrue(item.actions().stream().allMatch(ExecutionAction::failure));
        assertTrue(item.hasFailure());
        assertEquals("3 ações, 3 com falha", item.summary());
    }

    @Test
    void summaryCountsTheActions() {
        HelperAction trashed = new HelperAction(ActionType.TRASHED, Path.of("C:\\Temp\\a"), null, null);

        assertEquals("Nada a fazer", mapper.map(record(NOON_UTC)).summary());
        assertEquals("1 ação", mapper.map(record(NOON_UTC, trashed)).summary());
        assertEquals("3 ações", mapper.map(record(NOON_UTC, trashed, trashed, trashed)).summary());
        assertFalse(mapper.map(record(NOON_UTC, trashed)).hasFailure());
    }

    @Test
    void emptyExecutionHasNoActions() {
        assertTrue(mapper.map(record(NOON_UTC)).actions().isEmpty());
    }

    @Test
    void keepsTheOrderOfTheActions() {
        HelperAction first = new HelperAction(ActionType.TRASHED, Path.of("C:\\Temp\\1"), null, null);
        HelperAction second = new HelperAction(ActionType.TRASHED, Path.of("C:\\Temp\\2"), null, null);
        HelperAction third = new HelperAction(ActionType.TRASHED, Path.of("C:\\Temp\\3"), null, null);

        List<ExecutionAction> actions = mapper.map(record(NOON_UTC, third, first, second)).actions();

        assertEquals(List.of("Enviado à lixeira: 3", "Enviado à lixeira: 1", "Enviado à lixeira: 2"),
                actions.stream().map(ExecutionAction::text).toList());
    }

    @Test
    void mapAllOrdersFromTheNewestToTheOldest() {
        ExecutionRecord old = record(NOON_UTC.minusSeconds(86_400));
        ExecutionRecord recent = record(NOON_UTC);
        ExecutionRecord middle = record(NOON_UTC.minusSeconds(3_600));

        List<ExecutionItem> items = mapper.mapAll(List.of(old, recent, middle));

        assertEquals(List.of("05/03/2026 12:30", "05/03/2026 11:30", "04/03/2026 12:30"),
                items.stream().map(ExecutionItem::executedAt).toList());
    }

    @Test
    void mapAllOfNothingIsEmpty() {
        assertTrue(mapper.mapAll(List.of()).isEmpty());
    }

    @Test
    void rejectsNullArguments() {
        assertThrows(NullPointerException.class, () -> new ExecutionItemMapper(null));
        assertThrows(NullPointerException.class, () -> mapper.map(null));
        assertThrows(NullPointerException.class, () -> mapper.mapAll(null));
    }

    private static ExecutionRecord record(Instant at, HelperAction... actions) {
        return new ExecutionRecord(new HelperId(java.util.UUID.randomUUID()), "Organizar Downloads", at,
                List.of(actions));
    }
}
