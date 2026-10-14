package com.tidyfy.presentation.model;

import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Converte os registros do histórico nos itens exibidos pela tela, com os textos em português.
 * Na linha da ação aparece só o nome do arquivo (e a subpasta de destino, nas movimentações);
 * o caminho completo vai em {@link ExecutionAction#detail()}, mostrado como dica.
 */
public final class ExecutionItemMapper {

    /** Fuso usado para mostrar a data e a hora. */
    private final ZoneId zone;

    // Constantes

    /** Formato da data e hora de cada execução. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Resumo quando a execução não fez nada. */
    private static final String SUMMARY_NOTHING = "Nada a fazer";

    /** Resumo quando a execução fez uma única ação. */
    private static final String SUMMARY_ONE = "1 ação";

    /** Modelo do resumo com várias ações; recebe a quantidade. */
    private static final String SUMMARY_MANY = "%d ações";

    /** Modelo do resumo quando há falhas; recebe o resumo das ações e a quantidade de falhas. */
    private static final String SUMMARY_WITH_FAILURES = "%s, %d com falha";

    /** Modelo do texto de arquivo movido; recebe o nome de origem e o destino. */
    private static final String MOVED_TEXT = "%s → %s";

    /** Modelo do caminho completo de arquivo movido; recebe a origem e o destino. */
    private static final String MOVED_DETAIL = "%s → %s";

    /** Modelo do destino de uma movimentação; recebe a subpasta e o nome do arquivo. */
    private static final String DESTINATION = "%s\\%s";

    /** Modelo do texto de duplicado enviado à lixeira; recebe o nome do arquivo. */
    private static final String DUPLICATE_TRASHED_TEXT = "Duplicado enviado à lixeira: %s";

    /** Modelo do texto de arquivo enviado à lixeira; recebe o nome do arquivo. */
    private static final String TRASHED_TEXT = "Enviado à lixeira: %s";

    /** Texto da lixeira esvaziada. */
    private static final String RECYCLE_BIN_EMPTIED_TEXT = "Lixeira esvaziada";

    /** Modelo da lixeira esvaziada com a quantidade informada; recebe a mensagem da ação. */
    private static final String RECYCLE_BIN_EMPTIED_WITH_COUNT = "Lixeira esvaziada: %s";

    /** Modelo da falha em um arquivo; recebe o nome do arquivo e a mensagem. */
    private static final String FAILED_FILE_TEXT = "Falha em %s: %s";

    /** Modelo da falha sem arquivo; recebe a mensagem. */
    private static final String FAILED_TEXT = "Falha: %s";

    /** Mensagem usada quando a falha não trouxe texto. */
    private static final String UNKNOWN_FAILURE = "motivo desconhecido";

    /** Texto usado quando a ação não tem arquivo. */
    private static final String NO_FILE = "arquivo";

    /** Nome do parâmetro do fuso, usado na mensagem de valor nulo. */
    private static final String ZONE_NAME = "zone";

    /** Nome do parâmetro do registro, usado na mensagem de valor nulo. */
    private static final String RECORD_NAME = "record";

    /** Nome do parâmetro dos registros, usado na mensagem de valor nulo. */
    private static final String RECORDS_NAME = "records";

    /**
     * Cria o conversor.
     *
     * @param zone fuso em que a data e a hora são mostradas (o do sistema, no aplicativo)
     * @throws NullPointerException se {@code zone} for nulo
     */
    public ExecutionItemMapper(ZoneId zone) {
        this.zone = Objects.requireNonNull(zone, ZONE_NAME);
    }

    /**
     * Converte vários registros, do mais recente para o mais antigo.
     *
     * @param records registros do histórico, em qualquer ordem
     * @return os itens da tela, do mais recente para o mais antigo
     * @throws NullPointerException se {@code records} for nulo
     */
    public List<ExecutionItem> mapAll(List<ExecutionRecord> records) {
        Objects.requireNonNull(records, RECORDS_NAME);
        return records.stream()
                .sorted(Comparator.comparing(ExecutionRecord::executedAt).reversed())
                .map(this::map)
                .toList();
    }

    /**
     * Converte um registro.
     *
     * @param record registro do histórico
     * @return o item da tela, com as ações na mesma ordem do registro
     * @throws NullPointerException se {@code record} for nulo
     */
    public ExecutionItem map(ExecutionRecord record) {
        Objects.requireNonNull(record, RECORD_NAME);
        List<ExecutionAction> actions = record.actions().stream().map(ExecutionItemMapper::mapAction).toList();
        long failures = actions.stream().filter(ExecutionAction::failure).count();
        return new ExecutionItem(DATE_FORMAT.format(record.executedAt().atZone(zone)),
                summaryOf(actions.size(), failures), actions);
    }

    /**
     * Monta o resumo da execução.
     *
     * @param total    quantidade de ações
     * @param failures quantidade de ações que falharam
     * @return o resumo em português
     */
    private static String summaryOf(int total, long failures) {
        String summary = switch (total) {
            case 0 -> SUMMARY_NOTHING;
            case 1 -> SUMMARY_ONE;
            default -> String.format(SUMMARY_MANY, total);
        };
        return failures > 0 ? String.format(SUMMARY_WITH_FAILURES, summary, failures) : summary;
    }

    /**
     * Converte uma ação em texto.
     *
     * @param action ação do registro
     * @return a ação pronta para a tela
     */
    private static ExecutionAction mapAction(HelperAction action) {
        String source = nameOf(action.source());
        String detail = action.source() == null ? null : action.source().toString();
        return switch (action.type()) {
            case MOVED -> new ExecutionAction(String.format(MOVED_TEXT, source, destinationOf(action.target())),
                    movedDetail(action), false);
            case DUPLICATE_TRASHED ->
                    new ExecutionAction(String.format(DUPLICATE_TRASHED_TEXT, source), detail, false);
            case TRASHED -> new ExecutionAction(String.format(TRASHED_TEXT, source), detail, false);
            case RECYCLE_BIN_EMPTIED -> new ExecutionAction(action.message() == null ? RECYCLE_BIN_EMPTIED_TEXT
                    : String.format(RECYCLE_BIN_EMPTIED_WITH_COUNT, action.message()), null, false);
            case FAILED -> new ExecutionAction(failureText(action, source), detail, true);
        };
    }

    /**
     * Monta o texto de uma falha.
     *
     * @param action ação que falhou
     * @param source nome do arquivo de origem, se houver
     * @return o texto da falha com a mensagem
     */
    private static String failureText(HelperAction action, String source) {
        String message = action.message() == null || action.message().isBlank() ? UNKNOWN_FAILURE : action.message();
        return action.source() == null ? String.format(FAILED_TEXT, message)
                : String.format(FAILED_FILE_TEXT, source, message);
    }

    /**
     * Monta o caminho completo de uma movimentação.
     *
     * @param action ação de mover
     * @return "origem → destino" com os caminhos completos, ou só a origem (ou nada) se faltar algum
     */
    private static String movedDetail(HelperAction action) {
        if (action.source() == null || action.target() == null) {
            return action.source() == null ? null : action.source().toString();
        }
        return String.format(MOVED_DETAIL, action.source(), action.target());
    }

    /**
     * Devolve o nome do arquivo de um caminho.
     *
     * @param path caminho, possivelmente nulo
     * @return o nome do arquivo ou um texto genérico quando não há caminho
     */
    private static String nameOf(Path path) {
        return path == null || path.getFileName() == null ? NO_FILE : path.getFileName().toString();
    }

    /**
     * Devolve o destino como "subpasta\arquivo", sem o caminho inteiro.
     *
     * @param target destino da movimentação, possivelmente nulo
     * @return a subpasta e o nome do arquivo de destino
     */
    private static String destinationOf(Path target) {
        Path parent = target == null ? null : target.getParent();
        if (parent == null || parent.getFileName() == null) {
            return nameOf(target);
        }
        return String.format(DESTINATION, parent.getFileName(), nameOf(target));
    }
}
