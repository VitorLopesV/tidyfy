package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link ExecutionHistoryJsonMapper}.
 */
class ExecutionHistoryJsonMapperTest {

    /** Conversor testado */
    private final ExecutionHistoryJsonMapper mapper = new ExecutionHistoryJsonMapper();

    private ExecutionRecord record(String instant, HelperAction... actions) {
        return new ExecutionRecord(HelperId.newId(), "Organizar Downloads", Instant.parse(instant),
                Arrays.asList(actions));
    }

    /**
     * Ida e volta preserva todos os tipos de ação e os campos opcionais.
     */
    @Test
    void roundTripPreservesEveryActionType() {
        List<HelperAction> actions = Arrays.stream(ActionType.values())
                .map(type -> new HelperAction(type, Path.of("C:\\Users\\ana\\a.txt"),
                        Path.of("C:\\Users\\ana\\Documentos\\a.txt"), "msg " + type))
                .toList();
        ExecutionRecord original = new ExecutionRecord(HelperId.newId(), "Ajudante", Instant.parse("2026-10-14T12:00:00Z"),
                actions);

        List<ExecutionRecord> read = mapper.fromJson(mapper.toJson(List.of(original)));

        assertEquals(List.of(original), read);
        assertEquals(ActionType.values().length, read.get(0).actions().size());
    }

    /**
     * Origem, destino e mensagem ausentes viram null e voltam como null.
     */
    @Test
    void roundTripKeepsMissingSourceTargetAndMessage() {
        ExecutionRecord original = record("2026-10-14T12:00:00Z",
                new HelperAction(ActionType.RECYCLE_BIN_EMPTIED, null, null, null));

        String json = mapper.toJson(List.of(original));
        HelperAction action = mapper.fromJson(json).get(0).actions().get(0);

        assertNull(action.source());
        assertNull(action.target());
        assertNull(action.message());
        assertTrue(json.contains("\"source\" : null"));
    }

    /**
     * Campos opcionais omitidos do JSON são lidos como null.
     */
    @Test
    void readsActionsWithOptionalFieldsOmitted() {
        String id = UUID.randomUUID().toString();
        String json = "{\"version\":1,\"records\":[{\"helperId\":\"" + id + "\",\"helperName\":\"X\","
                + "\"executedAt\":\"2026-10-14T12:00:00Z\",\"actions\":[{\"type\":\"FAILED\"}]}]}";

        HelperAction action = mapper.fromJson(json).get(0).actions().get(0);

        assertEquals(new HelperAction(ActionType.FAILED, null, null, null), action);
    }

    /**
     * Instant com fração de segundo é preservado.
     */
    @Test
    void roundTripPreservesInstantFraction() {
        ExecutionRecord original = record("2026-10-14T12:00:00.123456789Z");

        assertEquals(List.of(original), mapper.fromJson(mapper.toJson(List.of(original))));
    }

    /**
     * Vários registros mantêm a ordem.
     */
    @Test
    void roundTripPreservesOrderOfManyRecords() {
        List<ExecutionRecord> records = List.of(record("2026-10-14T12:00:00Z"), record("2026-10-13T12:00:00Z"),
                record("2026-10-15T12:00:00Z"));

        assertEquals(records, mapper.fromJson(mapper.toJson(records)));
    }

    /**
     * Texto nulo, vazio ou em branco é histórico vazio; lista vazia também.
     */
    @Test
    void blankOrNullIsEmptyHistory() {
        assertTrue(mapper.fromJson(null).isEmpty());
        assertTrue(mapper.fromJson("").isEmpty());
        assertTrue(mapper.fromJson("  \n ").isEmpty());
        assertTrue(mapper.fromJson(mapper.toJson(List.of())).isEmpty());
    }

    /**
     * JSON inválido, não objeto, versão e campos inválidos geram HistoryJsonException.
     */
    @Test
    void invalidDocumentsThrowHistoryJsonException() {
        String id = UUID.randomUUID().toString();
        String head = "{\"version\":1,\"records\":[{\"helperId\":\"" + id
                + "\",\"helperName\":\"X\",\"executedAt\":\"2026-10-14T12:00:00Z\",\"actions\":";
        List<String> invalid = List.of(
                "{nao e json",
                "[]",
                "{\"records\":[]}",
                "{\"version\":2,\"records\":[]}",
                "{\"version\":1}",
                "{\"version\":1,\"records\":[1]}",
                head + "[{\"type\":\"DESCONHECIDO\"}]}]}",
                head + "[{\"source\":\"a\"}]}]}",
                head + "[{\"type\":\"MOVED\",\"source\":1}]}]}",
                head + "[3]}]}",
                head + "null}]}",
                "{\"version\":1,\"records\":[{\"helperId\":\"nao-uuid\",\"helperName\":\"X\","
                        + "\"executedAt\":\"2026-10-14T12:00:00Z\",\"actions\":[]}]}",
                "{\"version\":1,\"records\":[{\"helperId\":\"" + id + "\",\"helperName\":\"X\","
                        + "\"executedAt\":\"ontem\",\"actions\":[]}]}");

        for (String json : invalid) {
            assertThrows(HistoryJsonException.class, () -> mapper.fromJson(json), json);
        }
    }
}
