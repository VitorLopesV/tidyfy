package com.tidyfy.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Registro de uma execução de ajudante, guardado no histórico por {@link #RETENTION_DAYS} dias.
 * Guarda o nome que o ajudante tinha na hora, pois ele pode ser renomeado ou removido depois.
 *
 * @param helperId   ajudante que executou
 * @param helperName nome do ajudante no momento da execução
 * @param executedAt instante da execução
 * @param actions    ações realizadas, na ordem; a lista é copiada e fica imutável
 */
public record ExecutionRecord(HelperId helperId, String helperName, Instant executedAt, List<HelperAction> actions) {

    /** Por quantos dias o registro é guardado no histórico. */
    public static final int RETENTION_DAYS = 30;

    // Constantes

    /** Nome do parâmetro do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_ID_NAME = "helperId";

    /** Nome do parâmetro do nome do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_NAME_NAME = "helperName";

    /** Nome do parâmetro do instante da execução, usado na mensagem de valor nulo. */
    private static final String EXECUTED_AT_NAME = "executedAt";

    /** Nome do parâmetro das ações, usado na mensagem de valor nulo. */
    private static final String ACTIONS_NAME = "actions";

    /** Nome do parâmetro do instante atual, usado na mensagem de valor nulo. */
    private static final String NOW_NAME = "now";

    /**
     * Valida os dados e faz uma cópia imutável da lista de ações.
     *
     * @throws NullPointerException se algum parâmetro, ou alguma ação da lista, for nulo
     */
    public ExecutionRecord {
        Objects.requireNonNull(helperId, HELPER_ID_NAME);
        Objects.requireNonNull(helperName, HELPER_NAME_NAME);
        Objects.requireNonNull(executedAt, EXECUTED_AT_NAME);
        Objects.requireNonNull(actions, ACTIONS_NAME);
        actions = List.copyOf(actions);
    }

    /**
     * Diz se o registro já passou do prazo de guarda. Só é verdadeiro quando se passaram mais de
     * {@link #RETENTION_DAYS} dias (de 24 horas) desde a execução, o mesmo critério de
     * {@link AgeLimit#isExceededBy(Instant, Instant)}.
     *
     * @param now instante atual, informado por quem chama para não depender do relógio
     * @return {@code true} se o registro deve ser descartado
     * @throws NullPointerException se {@code now} for nulo
     */
    public boolean isExpired(Instant now) {
        Objects.requireNonNull(now, NOW_NAME);
        return new AgeLimit(RETENTION_DAYS).isExceededBy(executedAt, now);
    }
}
