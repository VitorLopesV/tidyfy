package com.tidyfy.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.CleanupTarget;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converte ajudantes de e para o JSON do {@code helpers.txt}.
 * <p>
 * Trabalha sobre a árvore JSON ({@link JsonNode}), sem anotações no domínio, e não toca no disco.
 * O documento tem a {@code version} do formato e a lista {@code helpers}, onde o campo {@code type}
 * discrimina o tipo de ajudante. Qualquer problema vira {@link HelperJsonException}.
 */
public final class HelperJsonMapper {

    /** Versão atual do formato do arquivo. */
    public static final int CURRENT_VERSION = 1;

    /** Conversor JSON usado para ler e escrever a árvore. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Constantes

    /** Campo da versão do formato. */
    private static final String VERSION_FIELD = "version";

    /** Campo da lista de ajudantes. */
    private static final String HELPERS_FIELD = "helpers";

    /** Campo discriminador do tipo. */
    private static final String TYPE_FIELD = "type";

    /** Campo do identificador. */
    private static final String ID_FIELD = "id";

    /** Campo do nome. */
    private static final String NAME_FIELD = "name";

    /** Campo do estado ligado/desligado. */
    private static final String ENABLED_FIELD = "enabled";

    /** Campo da frequência. */
    private static final String SCHEDULE_FIELD = "schedule";

    /** Campo do intervalo em dias. */
    private static final String INTERVAL_DAYS_FIELD = "intervalDays";

    /** Campo dos horários. */
    private static final String TIMES_FIELD = "times";

    /** Campo da data de início. */
    private static final String START_DATE_FIELD = "startDate";

    /** Campo da pasta. */
    private static final String FOLDER_FIELD = "folder";

    /** Campo do alvo da limpeza. */
    private static final String TARGET_FIELD = "target";

    /** Campo da referência de idade. */
    private static final String AGE_REFERENCE_FIELD = "ageReference";

    /** Campo do limite de idade em dias. */
    private static final String AGE_LIMIT_DAYS_FIELD = "ageLimitDays";

    /** Tipo de ajudante que organiza uma pasta. */
    private static final String FOLDER_ORGANIZER_TYPE = "FOLDER_ORGANIZER";

    /** Tipo de ajudante que faz limpeza. */
    private static final String CLEANER_TYPE = "CLEANER";

    /** Tipo de alvo: pasta. */
    private static final String FOLDER_TARGET_TYPE = "FOLDER";

    /** Tipo de alvo: lixeira. */
    private static final String RECYCLE_BIN_TARGET_TYPE = "RECYCLE_BIN";

    /** Padrão dos horários. */
    private static final String TIME_PATTERN = "HH:mm";

    /** Formato dos horários. */
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /** Mensagem de JSON inválido. */
    private static final String INVALID_JSON = "O JSON dos ajudantes é inválido: %s";

    /** Mensagem de falha ao gerar o JSON. */
    private static final String WRITE_ERROR = "Não foi possível gerar o JSON dos ajudantes: %s";

    /** Mensagem de documento que não é um objeto. */
    private static final String NOT_AN_OBJECT = "O documento dos ajudantes deve ser um objeto JSON.";

    /** Mensagem de versão não suportada. */
    private static final String UNSUPPORTED_VERSION =
            "Versão %d do formato dos ajudantes não é suportada (esperada: %d).";

    /** Mensagem de campo obrigatório ausente ou com tipo errado. */
    private static final String INVALID_FIELD = "Campo obrigatório ausente ou inválido: %s";

    /** Mensagem de tipo desconhecido. */
    private static final String UNKNOWN_TYPE = "Tipo desconhecido no campo %s: %s";

    /** Mensagem de valor recusado. */
    private static final String INVALID_VALUE = "Valor inválido no ajudante: %s";

    /**
     * Converte os ajudantes no documento JSON indentado, com a versão atual.
     *
     * @param helpers ajudantes a converter
     * @return o JSON do documento
     * @throws HelperJsonException se o documento não puder ser gerado
     */
    public String toJson(List<Helper> helpers) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put(VERSION_FIELD, CURRENT_VERSION);
        ArrayNode array = root.putArray(HELPERS_FIELD);
        for (Helper helper : helpers) {
            array.add(toNode(helper));
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new HelperJsonException(String.format(WRITE_ERROR, e.getOriginalMessage()), e);
        }
    }

    /**
     * Converte o documento JSON nos ajudantes que ele descreve.
     * Texto nulo, vazio ou em branco devolve lista vazia.
     *
     * @param json conteúdo do documento
     * @return os ajudantes lidos
     * @throws HelperJsonException se o JSON for inválido, a versão não for suportada,
     *                             faltar campo, o tipo for desconhecido ou o domínio recusar um valor
     */
    public List<Helper> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new HelperJsonException(NOT_AN_OBJECT);
            }
            checkVersion(root.get(VERSION_FIELD));
            List<Helper> helpers = new ArrayList<>();
            for (JsonNode node : array(root, HELPERS_FIELD)) {
                helpers.add(toHelper(node));
            }
            return List.copyOf(helpers);
        } catch (JsonProcessingException e) {
            throw new HelperJsonException(String.format(INVALID_JSON, e.getOriginalMessage()), e);
        } catch (DateTimeException | IllegalArgumentException e) {
            throw new HelperJsonException(String.format(INVALID_VALUE, e.getMessage()), e);
        }
    }

    /**
     * Confere a versão do documento.
     *
     * @param version nó da versão, possivelmente nulo
     */
    private void checkVersion(JsonNode version) {
        if (version == null || !version.isInt()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, VERSION_FIELD));
        }
        if (version.intValue() != CURRENT_VERSION) {
            throw new HelperJsonException(String.format(UNSUPPORTED_VERSION, version.intValue(), CURRENT_VERSION));
        }
    }

    /**
     * Converte um ajudante em nó JSON.
     *
     * @param helper ajudante
     * @return o nó
     */
    private ObjectNode toNode(Helper helper) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (helper) {
            case FolderOrganizerHelper organizer -> {
                node.put(TYPE_FIELD, FOLDER_ORGANIZER_TYPE);
                writeCommon(node, helper);
                node.put(FOLDER_FIELD, organizer.folder().toString());
            }
            case CleanerHelper cleaner -> {
                node.put(TYPE_FIELD, CLEANER_TYPE);
                writeCommon(node, helper);
                node.set(TARGET_FIELD, targetToNode(cleaner.target()));
                if (cleaner.ageLimit() == null) {
                    node.putNull(AGE_LIMIT_DAYS_FIELD);
                } else {
                    node.put(AGE_LIMIT_DAYS_FIELD, cleaner.ageLimit().days());
                }
            }
        }
        return node;
    }

    /**
     * Escreve os campos comuns a todo ajudante.
     *
     * @param node   nó de destino
     * @param helper ajudante
     */
    private void writeCommon(ObjectNode node, Helper helper) {
        node.put(ID_FIELD, helper.id().value().toString());
        node.put(NAME_FIELD, helper.name().value());
        node.put(ENABLED_FIELD, helper.enabled());
        ObjectNode schedule = node.putObject(SCHEDULE_FIELD);
        schedule.put(INTERVAL_DAYS_FIELD, helper.schedule().intervalDays());
        ArrayNode times = schedule.putArray(TIMES_FIELD);
        helper.schedule().times().forEach(time -> times.add(TIME_FORMAT.format(time)));
        schedule.put(START_DATE_FIELD, helper.schedule().startDate().toString());
    }

    /**
     * Converte um alvo de limpeza em nó JSON.
     *
     * @param target alvo
     * @return o nó
     */
    private ObjectNode targetToNode(CleanupTarget target) {
        ObjectNode node = objectMapper.createObjectNode();
        switch (target) {
            case FolderTarget folder -> {
                node.put(TYPE_FIELD, FOLDER_TARGET_TYPE);
                node.put(FOLDER_FIELD, folder.folder().toString());
                node.put(AGE_REFERENCE_FIELD, folder.ageReference().name());
            }
            case RecycleBinTarget ignored -> node.put(TYPE_FIELD, RECYCLE_BIN_TARGET_TYPE);
        }
        return node;
    }

    /**
     * Converte um nó JSON em ajudante.
     *
     * @param node nó do ajudante
     * @return o ajudante
     */
    private Helper toHelper(JsonNode node) {
        if (!node.isObject()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, HELPERS_FIELD));
        }
        String type = text(node, TYPE_FIELD);
        HelperId id = new HelperId(UUID.fromString(text(node, ID_FIELD)));
        HelperName name = new HelperName(text(node, NAME_FIELD));
        boolean enabled = bool(node, ENABLED_FIELD);
        Schedule schedule = toSchedule(object(node, SCHEDULE_FIELD));
        return switch (type) {
            case FOLDER_ORGANIZER_TYPE ->
                    new FolderOrganizerHelper(id, name, schedule, enabled, Path.of(text(node, FOLDER_FIELD)));
            case CLEANER_TYPE -> new CleanerHelper(id, name, schedule, enabled,
                    toTarget(object(node, TARGET_FIELD)), toAgeLimit(node.get(AGE_LIMIT_DAYS_FIELD)));
            default -> throw new HelperJsonException(String.format(UNKNOWN_TYPE, TYPE_FIELD, type));
        };
    }

    /**
     * Converte o nó da frequência.
     *
     * @param node nó da frequência
     * @return a frequência
     */
    private Schedule toSchedule(JsonNode node) {
        List<LocalTime> times = new ArrayList<>();
        for (JsonNode time : array(node, TIMES_FIELD)) {
            if (!time.isTextual()) {
                throw new HelperJsonException(String.format(INVALID_FIELD, TIMES_FIELD));
            }
            times.add(LocalTime.parse(time.textValue(), TIME_FORMAT));
        }
        return new Schedule(integer(node, INTERVAL_DAYS_FIELD), times, LocalDate.parse(text(node, START_DATE_FIELD)));
    }

    /**
     * Converte o nó do alvo de limpeza.
     *
     * @param node nó do alvo
     * @return o alvo
     */
    private CleanupTarget toTarget(JsonNode node) {
        String type = text(node, TYPE_FIELD);
        return switch (type) {
            case FOLDER_TARGET_TYPE -> new FolderTarget(Path.of(text(node, FOLDER_FIELD)),
                    AgeReference.valueOf(text(node, AGE_REFERENCE_FIELD)));
            case RECYCLE_BIN_TARGET_TYPE -> new RecycleBinTarget();
            default -> throw new HelperJsonException(String.format(UNKNOWN_TYPE, TARGET_FIELD, type));
        };
    }

    /**
     * Converte o nó do limite de idade; ausente ou nulo significa sem limite.
     *
     * @param node nó, possivelmente nulo
     * @return o limite, ou {@code null} se não houver
     */
    private AgeLimit toAgeLimit(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isInt()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, AGE_LIMIT_DAYS_FIELD));
        }
        return new AgeLimit(node.intValue());
    }

    /**
     * Lê um campo de texto obrigatório.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return o texto
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, field));
        }
        return value.textValue();
    }

    /**
     * Lê um campo booleano obrigatório.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return o valor
     */
    private boolean bool(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isBoolean()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, field));
        }
        return value.booleanValue();
    }

    /**
     * Lê um campo inteiro obrigatório.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return o valor
     */
    private int integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isInt()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, field));
        }
        return value.intValue();
    }

    /**
     * Lê um campo objeto obrigatório.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return o objeto
     */
    private JsonNode object(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, field));
        }
        return value;
    }

    /**
     * Lê um campo lista obrigatório.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return a lista
     */
    private JsonNode array(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isArray()) {
            throw new HelperJsonException(String.format(INVALID_FIELD, field));
        }
        return value;
    }
}
