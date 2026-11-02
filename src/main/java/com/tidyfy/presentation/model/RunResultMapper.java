package com.tidyfy.presentation.model;

import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Converte o registro de uma execução manual no resultado exibido ao usuário. Há falha quando o registro
 * tem alguma ação {@link ActionType#FAILED}, a mesma regra do modo sem interface.
 */
public final class RunResultMapper {

    // Constantes

    /** Título do sucesso. */
    private static final String SUCCESS_TITLE = "Execução concluída";

    /** Modelo da mensagem de sucesso sem nenhuma ação; recebe o nome do ajudante. */
    private static final String SUCCESS_NOTHING = "%s rodou, mas não havia nada a fazer.";

    /** Modelo da mensagem de sucesso com uma ação; recebe o nome do ajudante. */
    private static final String SUCCESS_ONE = "%s rodou e fez 1 ação. Os detalhes estão no histórico.";

    /** Modelo da mensagem de sucesso com várias ações; recebe o nome do ajudante e a quantidade. */
    private static final String SUCCESS_MANY = "%s rodou e fez %d ações. Os detalhes estão no histórico.";

    /** Título da falha. */
    private static final String FAILURE_TITLE = "A execução falhou";

    /** Modelo da mensagem de falha; recebe o nome do ajudante. */
    private static final String FAILURE_MESSAGE =
            "%s não conseguiu terminar o que devia. O que deu errado está nos detalhes e no histórico.";

    /** Modelo da linha de detalhe de uma falha com origem; recebe a origem e a mensagem. */
    private static final String FAILURE_WITH_SOURCE = "%s: %s";

    /** Separador das linhas de detalhe. */
    private static final String LINE_SEPARATOR = "\n";

    /** Título de quando o ajudante não existe mais. */
    private static final String MISSING_TITLE = "Ajudante não encontrado";

    /** Modelo da mensagem de quando o ajudante não existe mais; recebe o nome do ajudante. */
    private static final String MISSING_MESSAGE =
            "%s não existe mais, talvez tenha sido removido por outro processo. A lista foi atualizada.";

    /** Detalhe de quando o ajudante não existe mais. */
    private static final String MISSING_DETAILS = "Ajudante inexistente no repositório.";

    /** Nome do parâmetro do nome do ajudante, usado na mensagem de valor nulo. */
    private static final String NAME_NAME = "helperName";

    /** Nome do parâmetro do registro, usado na mensagem de valor nulo. */
    private static final String RECORD_NAME = "record";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private RunResultMapper() {
    }

    /**
     * Converte o registro da execução no resultado exibido.
     *
     * @param helperName nome do ajudante, como aparece na lista
     * @param record     registro gravado no histórico
     * @return sucesso com a quantidade de ações, ou falha com a causa de cada ação que falhou
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public static RunResult toResult(String helperName, ExecutionRecord record) {
        Objects.requireNonNull(helperName, NAME_NAME);
        Objects.requireNonNull(record, RECORD_NAME);
        List<HelperAction> failures = record.actions().stream()
                .filter(action -> action.type() == ActionType.FAILED).toList();
        if (!failures.isEmpty()) {
            String details = failures.stream().map(RunResultMapper::describe)
                    .collect(Collectors.joining(LINE_SEPARATOR));
            return new RunResult(false, FAILURE_TITLE, String.format(FAILURE_MESSAGE, helperName), details);
        }
        int done = record.actions().size();
        String message = switch (done) {
            case 0 -> String.format(SUCCESS_NOTHING, helperName);
            case 1 -> String.format(SUCCESS_ONE, helperName);
            default -> String.format(SUCCESS_MANY, helperName, done);
        };
        return new RunResult(true, SUCCESS_TITLE, message, null);
    }

    /**
     * Resultado de quando o ajudante não existe mais no momento da execução.
     *
     * @param helperName nome do ajudante, como aparece na lista
     * @return uma falha explicando que o ajudante não foi encontrado
     * @throws NullPointerException se {@code helperName} for nulo
     */
    public static RunResult missing(String helperName) {
        Objects.requireNonNull(helperName, NAME_NAME);
        return new RunResult(false, MISSING_TITLE, String.format(MISSING_MESSAGE, helperName), MISSING_DETAILS);
    }

    /**
     * Descreve uma ação que falhou.
     *
     * @param action ação com falha
     * @return a origem e a mensagem, ou só a mensagem quando não há origem
     */
    private static String describe(HelperAction action) {
        return action.source() == null ? action.message()
                : String.format(FAILURE_WITH_SOURCE, action.source(), action.message());
    }
}
