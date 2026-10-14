package com.tidyfy.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Converte registros de execução de e para o JSON do {@code history.txt}.
 * <p>
 * Trabalha sobre a árvore JSON ({@link JsonNode}), sem anotações no domínio, e não toca no disco.
 * O documento tem a {@code version} do formato e a lista {@code records}. Origem, destino e mensagem
 * das ações são gravados como {@code null} quando não existem e aceitos ausentes ou {@code null} na leitura.
 * Qualquer problema vira {@link HistoryJsonException}.
 */
public final class ExecutionHistoryJsonMapper {

    /** Versão atual do formato do arquivo. */
    public static final int CURRENT_VERSION = 1;

    /** Conversor JSON usado para ler e escrever a árvore. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Constantes

    /** Campo da versão do formato. */
    private static final String VERSION_FIELD = "version";

    /** Campo da lista de registros. */
    private static final String RECORDS_FIELD = "records";

    /** Campo do identificador do ajudante. */
    private static final String HELPER_ID_FIELD = "helperId";

    /** Campo do nome do ajudante. */
    private static final String HELPER_NAME_FIELD = "helperName";

    /** Campo do instante da execução. */
    private static final String EXECUTED_AT_FIELD = "executedAt";

    /** Campo da lista de ações. */
    private static final String ACTIONS_FIELD = "actions";

    /** Campo do tipo da ação. */
    private static final String TYPE_FIELD = "type";

    /** Campo da origem da ação. */
    private static final String SOURCE_FIELD = "source";

    /** Campo do destino da ação. */
    private static final String TARGET_FIELD = "target";

    /** Campo da mensagem da ação. */
    private static final String MESSAGE_FIELD = "message";

    /** Mensagem de JSON inválido. */
    private static final String INVALID_JSON = "O JSON do histórico é inválido: %s";

    /** Mensagem de falha ao gerar o JSON. */
    private static final String WRITE_ERROR = "Não foi possível gerar o JSON do histórico: %s";

    /** Mensagem de documento que não é um objeto. */
    private static final String NOT_AN_OBJECT = "O documento do histórico deve ser um objeto JSON.";

    /** Mensagem de versão não suportada. */
    private static final String UNSUPPORTED_VERSION =
            "Versão %d do formato do histórico não é suportada (esperada: %d).";

    /** Mensagem de campo obrigatório ausente ou com tipo errado. */
    private static final String INVALID_FIELD = "Campo obrigatório ausente ou inválido: %s";

    /** Mensagem de valor recusado. */
    private static final String INVALID_VALUE = "Valor inválido no histórico: %s";

    /**
     * Converte os registros no documento JSON indentado, com a versão atual.
     *
     * @param records registros a converter
     * @return o JSON do documento
     * @throws HistoryJsonException se o documento não puder ser gerado
     */
    public String toJson(List<ExecutionRecord> records) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put(VERSION_FIELD, CURRENT_VERSION);
        ArrayNode array = root.putArray(RECORDS_FIELD);
        for (ExecutionRecord record : records) {
            array.add(toNode(record));
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new HistoryJsonException(String.format(WRITE_ERROR, e.getOriginalMessage()), e);
        }
    }

    /**
     * Converte o documento JSON nos registros que ele descreve.
     * Texto nulo, vazio ou em branco devolve lista vazia.
     *
     * @param json conteúdo do documento
     * @return os registros lidos, na ordem do arquivo
     * @throws HistoryJsonException se o JSON for inválido, a versão não for suportada,
     *                              faltar campo ou algum valor for recusado
     */
    public List<ExecutionRecord> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root == null || !root.isObject()) {
                throw new HistoryJsonException(NOT_AN_OBJECT);
            }
            checkVersion(root.get(VERSION_FIELD));
            List<ExecutionRecord> records = new ArrayList<>();
            for (JsonNode node : array(root, RECORDS_FIELD)) {
                records.add(toRecord(node));
            }
            return List.copyOf(records);
        } catch (JsonProcessingException e) {
            throw new HistoryJsonException(String.format(INVALID_JSON, e.getOriginalMessage()), e);
        } catch (DateTimeException | IllegalArgumentException e) {
            throw new HistoryJsonException(String.format(INVALID_VALUE, e.getMessage()), e);
        }
    }

    /**
     * Confere a versão do documento.
     *
     * @param version nó da versão, possivelmente nulo
     */
    private void checkVersion(JsonNode version) {
        if (version == null || !version.isInt()) {
            throw new HistoryJsonException(String.format(INVALID_FIELD, VERSION_FIELD));
        }
        if (version.intValue() != CURRENT_VERSION) {
            throw new HistoryJsonException(String.format(UNSUPPORTED_VERSION, version.intValue(), CURRENT_VERSION));
        }
    }

    /**
     * Converte um registro em nó JSON.
     *
     * @param record registro
     * @return o nó
     */
    private ObjectNode toNode(ExecutionRecord record) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(HELPER_ID_FIELD, record.helperId().value().toString());
        node.put(HELPER_NAME_FIELD, record.helperName());
        node.put(EXECUTED_AT_FIELD, record.executedAt().toString());
        ArrayNode actions = node.putArray(ACTIONS_FIELD);
        for (HelperAction action : record.actions()) {
            ObjectNode actionNode = actions.addObject();
            actionNode.put(TYPE_FIELD, action.type().name());
            putNullable(actionNode, SOURCE_FIELD, action.source() == null ? null : action.source().toString());
            putNullable(actionNode, TARGET_FIELD, action.target() == null ? null : action.target().toString());
            putNullable(actionNode, MESSAGE_FIELD, action.message());
        }
        return node;
    }

    /**
     * Escreve um texto, ou {@code null} JSON quando o valor não existe.
     *
     * @param node  nó de destino
     * @param field nome do campo
     * @param value valor, possivelmente nulo
     */
    private void putNullable(ObjectNode node, String field, String value) {
        if (value == null) {
            node.putNull(field);
        } else {
            node.put(field, value);
        }
    }

    /**
     * Converte um nó JSON em registro.
     *
     * @param node nó do registro
     * @return o registro
     */
    private ExecutionRecord toRecord(JsonNode node) {
        if (!node.isObject()) {
            throw new HistoryJsonException(String.format(INVALID_FIELD, RECORDS_FIELD));
        }
        HelperId id = new HelperId(UUID.fromString(text(node, HELPER_ID_FIELD)));
        String name = text(node, HELPER_NAME_FIELD);
        Instant executedAt = Instant.parse(text(node, EXECUTED_AT_FIELD));
        List<HelperAction> actions = new ArrayList<>();
        for (JsonNode actionNode : array(node, ACTIONS_FIELD)) {
            actions.add(toAction(actionNode));
        }
        return new ExecutionRecord(id, name, executedAt, actions);
    }

    /**
     * Converte um nó JSON em ação.
     *
     * @param node nó da ação
     * @return a ação
     */
    private HelperAction toAction(JsonNode node) {
        if (!node.isObject()) {
            throw new HistoryJsonException(String.format(INVALID_FIELD, ACTIONS_FIELD));
        }
        ActionType type = ActionType.valueOf(text(node, TYPE_FIELD));
        String source = optionalText(node, SOURCE_FIELD);
        String target = optionalText(node, TARGET_FIELD);
        return new HelperAction(type, source == null ? null : Path.of(source),
                target == null ? null : Path.of(target), optionalText(node, MESSAGE_FIELD));
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
            throw new HistoryJsonException(String.format(INVALID_FIELD, field));
        }
        return value.textValue();
    }

    /**
     * Lê um campo de texto opcional; ausente ou {@code null} JSON devolve {@code null}.
     *
     * @param node  nó pai
     * @param field nome do campo
     * @return o texto, ou {@code null}
     */
    private String optionalText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new HistoryJsonException(String.format(INVALID_FIELD, field));
        }
        return value.textValue();
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
            throw new HistoryJsonException(String.format(INVALID_FIELD, field));
        }
        return value;
    }
}
