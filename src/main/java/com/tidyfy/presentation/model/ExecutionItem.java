package com.tidyfy.presentation.model;

import java.util.List;
import java.util.Objects;

/**
 * Uma execução de ajudante como a tela a exibe.
 *
 * @param executedAt data e hora já formatadas
 * @param summary    resumo da execução (ex.: "3 ações" ou "Nada a fazer")
 * @param actions    ações realizadas, na ordem; a lista é copiada e fica imutável
 */
public record ExecutionItem(String executedAt, String summary, List<ExecutionAction> actions) {

    // Constantes

    /** Nome do parâmetro da data, usado na mensagem de valor nulo. */
    private static final String EXECUTED_AT_NAME = "executedAt";

    /** Nome do parâmetro do resumo, usado na mensagem de valor nulo. */
    private static final String SUMMARY_NAME = "summary";

    /** Nome do parâmetro das ações, usado na mensagem de valor nulo. */
    private static final String ACTIONS_NAME = "actions";

    /**
     * Valida os dados e faz uma cópia imutável da lista de ações.
     *
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public ExecutionItem {
        Objects.requireNonNull(executedAt, EXECUTED_AT_NAME);
        Objects.requireNonNull(summary, SUMMARY_NAME);
        Objects.requireNonNull(actions, ACTIONS_NAME);
        actions = List.copyOf(actions);
    }

    /**
     * Diz se alguma ação da execução falhou.
     *
     * @return {@code true} se há ao menos uma falha
     */
    public boolean hasFailure() {
        return actions.stream().anyMatch(ExecutionAction::failure);
    }
}
