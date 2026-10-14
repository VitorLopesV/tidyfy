package com.tidyfy.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link HelperJsonMapper}: ida e volta, formato do documento e erros.
 */
class HelperJsonMapperTest {

    /** Frequência JSON válida */
    private static final String VALID_SCHEDULE =
            "{\"intervalDays\": 1, \"times\": [\"09:00\"], \"startDate\": \"2026-10-14\"}";

    /** Identificador válido em texto */
    private static final String VALID_ID = "3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10";

    /** Mapeador sob teste */
    private final HelperJsonMapper mapper = new HelperJsonMapper();

    /** Frequência de exemplo */
    private final Schedule schedule = new Schedule(2, List.of(LocalTime.of(9, 0), LocalTime.of(18, 30)),
            LocalDate.of(2026, 10, 14));

    /**
     * Monta um documento com a versão e os itens informados.
     */
    private String document(int version, String helpers) {
        return "{\"version\": " + version + ", \"helpers\": [" + helpers + "]}";
    }

    /**
     * Monta um item de organização com o tipo e a frequência informados.
     */
    private String organizer(String type, String scheduleJson) {
        return "{\"type\": \"" + type + "\", \"id\": \"" + VALID_ID + "\","
                + " \"name\": \"A\", \"enabled\": true, \"schedule\": " + scheduleJson
                + ", \"folder\": \"C:\\\\Users\\\\ana\\\\Downloads\"}";
    }

    /**
     * Monta um item de limpeza com o alvo e o trecho extra informados.
     */
    private String cleaner(String targetJson, String extra) {
        return "{\"type\": \"CLEANER\", \"id\": \"" + VALID_ID + "\", \"name\": \"L\","
                + " \"enabled\": true, \"schedule\": " + VALID_SCHEDULE + ", \"target\": " + targetJson + extra + "}";
    }

    /**
     * Ida e volta de um organizador de pasta.
     */
    @Test
    void roundTripsFolderOrganizer() {
        Helper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"), schedule, true,
                Path.of("C:\\Users\\ana\\Downloads"));
        assertEquals(List.of(helper), mapper.fromJson(mapper.toJson(List.of(helper))));
    }

    /**
     * Ida e volta de limpeza de pasta com limite de idade.
     */
    @Test
    void roundTripsCleanerWithFolderTargetAndAgeLimit() {
        Helper helper = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"), schedule, false,
                new FolderTarget(Path.of("C:\\Temp"), AgeReference.CREATED), new AgeLimit(30));
        assertEquals(List.of(helper), mapper.fromJson(mapper.toJson(List.of(helper))));
    }

    /**
     * Ida e volta de limpeza da lixeira sem limite de idade.
     */
    @Test
    void roundTripsCleanerWithRecycleBinAndNoAgeLimit() {
        CleanerHelper helper = new CleanerHelper(HelperId.newId(), new HelperName("Lixeira"), schedule, true,
                new RecycleBinTarget(), null);
        List<Helper> restored = mapper.fromJson(mapper.toJson(List.of(helper)));
        assertEquals(List.of(helper), restored);
        assertNull(((CleanerHelper) restored.get(0)).ageLimit());
    }

    /**
     * Vários ajudantes mantêm a ordem e a lista vazia é preservada.
     */
    @Test
    void roundTripsSeveralHelpersInOrder() {
        Helper first = new FolderOrganizerHelper(HelperId.newId(), new HelperName("B"), schedule, true,
                Path.of("C:\\B"));
        Helper second = new CleanerHelper(HelperId.newId(), new HelperName("A"), schedule, true,
                new RecycleBinTarget(), new AgeLimit(1));
        assertEquals(List.of(first, second), mapper.fromJson(mapper.toJson(List.of(first, second))));
        assertEquals(List.of(), mapper.fromJson(mapper.toJson(List.of())));
    }

    /**
     * O documento tem o campo de versão, é indentado e usa horários HH:mm.
     */
    @Test
    void writesVersionIndentedAndTimeFormat() throws Exception {
        Helper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"), schedule, true,
                Path.of("C:\\Users"));
        String json = mapper.toJson(List.of(helper));
        JsonNode root = new ObjectMapper().readTree(json);
        JsonNode item = root.get("helpers").get(0);
        assertEquals(HelperJsonMapper.CURRENT_VERSION, root.get("version").intValue());
        assertEquals("FOLDER_ORGANIZER", item.get("type").textValue());
        assertEquals("18:30", item.get("schedule").get("times").get(1).textValue());
        assertEquals("2026-10-14", item.get("schedule").get("startDate").textValue());
        assertTrue(json.contains("\n"));
    }

    /**
     * Lê os exemplos do formato documentado.
     */
    @Test
    void parsesDocumentedExample() {
        String json = """
                {
                  "version": 1,
                  "helpers": [
                    {
                      "type": "FOLDER_ORGANIZER",
                      "id": "3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10",
                      "name": "Organizar Downloads",
                      "enabled": true,
                      "schedule": {"intervalDays": 1, "times": ["09:00", "18:00"], "startDate": "2026-10-14"},
                      "folder": "C:\\\\Users\\\\ana\\\\Downloads"
                    },
                    {
                      "type": "CLEANER",
                      "id": "a91c5d72-0b3e-4f68-8d14-6e7f2c3b5a09",
                      "name": "Limpar Temporários",
                      "enabled": true,
                      "schedule": {"intervalDays": 7, "times": ["20:30"], "startDate": "2026-10-14"},
                      "target": {"type": "FOLDER", "folder": "C:\\\\Users\\\\ana\\\\Temp", "ageReference": "MODIFIED"},
                      "ageLimitDays": 30
                    },
                    {
                      "type": "CLEANER",
                      "id": "c47e0f13-9d2a-4b85-a6c1-3e8d5f7a2b64",
                      "name": "Esvaziar Lixeira",
                      "enabled": false,
                      "schedule": {"intervalDays": 3, "times": ["12:00"], "startDate": "2026-10-14"},
                      "target": {"type": "RECYCLE_BIN"},
                      "ageLimitDays": null
                    }
                  ]
                }
                """;
        List<Helper> helpers = mapper.fromJson(json);
        assertEquals(3, helpers.size());
        assertEquals(new FolderOrganizerHelper(new HelperId(UUID.fromString(VALID_ID)),
                new HelperName("Organizar Downloads"),
                new Schedule(1, List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)), LocalDate.of(2026, 10, 14)), true,
                Path.of("C:\\Users\\ana\\Downloads")), helpers.get(0));
        CleanerHelper folderCleaner = (CleanerHelper) helpers.get(1);
        assertEquals(new FolderTarget(Path.of("C:\\Users\\ana\\Temp"), AgeReference.MODIFIED), folderCleaner.target());
        assertEquals(new AgeLimit(30), folderCleaner.ageLimit());
        CleanerHelper binCleaner = (CleanerHelper) helpers.get(2);
        assertEquals(new RecycleBinTarget(), binCleaner.target());
        assertNull(binCleaner.ageLimit());
    }

    /**
     * Campo ageLimitDays ausente significa sem limite.
     */
    @Test
    void treatsMissingAgeLimitAsNoLimit() {
        String json = document(1, cleaner("{\"type\": \"RECYCLE_BIN\"}", ""));
        assertNull(((CleanerHelper) mapper.fromJson(json).get(0)).ageLimit());
    }

    /**
     * Texto vazio, em branco ou nulo devolve lista vazia.
     */
    @Test
    void returnsEmptyListForBlankDocument() {
        assertEquals(List.of(), mapper.fromJson(""));
        assertEquals(List.of(), mapper.fromJson("  \n "));
        assertEquals(List.of(), mapper.fromJson(null));
    }

    /**
     * JSON malformado ou que não é objeto lança a exceção do mapeador.
     */
    @Test
    void rejectsMalformedJson() {
        assertThrows(HelperJsonException.class, () -> mapper.fromJson("{ nao é json"));
        assertThrows(HelperJsonException.class, () -> mapper.fromJson("[]"));
    }

    /**
     * Tipo de ajudante desconhecido é recusado.
     */
    @Test
    void rejectsUnknownHelperType() {
        HelperJsonException e = assertThrows(HelperJsonException.class,
                () -> mapper.fromJson(document(1, organizer("MAGIC", VALID_SCHEDULE))));
        assertTrue(e.getMessage().contains("MAGIC"));
    }

    /**
     * Tipo de alvo desconhecido é recusado.
     */
    @Test
    void rejectsUnknownTargetType() {
        String json = document(1, cleaner("{\"type\": \"DISK\"}", ""));
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(json));
    }

    /**
     * Versão diferente da suportada é recusada com mensagem específica.
     */
    @Test
    void rejectsUnsupportedVersion() {
        HelperJsonException e = assertThrows(HelperJsonException.class, () -> mapper.fromJson(document(99, "")));
        assertTrue(e.getMessage().contains("99"));
    }

    /**
     * Versão ausente é recusada.
     */
    @Test
    void rejectsMissingVersion() {
        assertThrows(HelperJsonException.class, () -> mapper.fromJson("{\"helpers\": []}"));
    }

    /**
     * Campos obrigatórios ausentes são recusados.
     */
    @Test
    void rejectsMissingRequiredFields() {
        assertThrows(HelperJsonException.class, () -> mapper.fromJson("{\"version\": 1}"));
        String noFolder = "{\"type\": \"FOLDER_ORGANIZER\", \"id\": \"" + VALID_ID + "\","
                + " \"name\": \"A\", \"enabled\": true, \"schedule\": " + VALID_SCHEDULE + "}";
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(document(1, noFolder)));
        String noName = noFolder.replace("\"name\": \"A\",", "");
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(document(1, noName)));
        String noTarget = cleaner("{}", "").replace("\"target\": {}", "\"x\": 1");
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(document(1, noTarget)));
    }

    /**
     * Valores recusados pelo domínio ou mal formados viram a exceção do mapeador.
     */
    @Test
    void rejectsValuesRefusedByTheDomain() {
        String zeroInterval = "{\"intervalDays\": 0, \"times\": [\"09:00\"], \"startDate\": \"2026-10-14\"}";
        String repeatedTimes = "{\"intervalDays\": 1, \"times\": [\"09:00\", \"09:00\"], \"startDate\": \"2026-10-14\"}";
        String badTime = "{\"intervalDays\": 1, \"times\": [\"9h\"], \"startDate\": \"2026-10-14\"}";
        String badDate = "{\"intervalDays\": 1, \"times\": [\"09:00\"], \"startDate\": \"14/10/2026\"}";
        for (String invalid : List.of(zeroInterval, repeatedTimes, badTime, badDate)) {
            assertThrows(HelperJsonException.class,
                    () -> mapper.fromJson(document(1, organizer("FOLDER_ORGANIZER", invalid))));
        }
        String badId = organizer("FOLDER_ORGANIZER", VALID_SCHEDULE).replace(VALID_ID, "x");
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(document(1, badId)));
        String zeroAge = document(1, cleaner("{\"type\": \"RECYCLE_BIN\"}", ", \"ageLimitDays\": 0"));
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(zeroAge));
        String badReference = document(1, cleaner(
                "{\"type\": \"FOLDER\", \"folder\": \"C:\\\\T\", \"ageReference\": \"NEVER\"}", ""));
        assertThrows(HelperJsonException.class, () -> mapper.fromJson(badReference));
    }
}
