package com.tidyfy.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Idade máxima, em dias, que um arquivo pode ter para ser mantido na limpeza de pasta.
 *
 * @param days limite em dias (no mínimo 1)
 */
public record AgeLimit(int days) {

    // Constantes

    /** Menor limite permitido, em dias. */
    private static final int MIN_DAYS = 1;

    /** Mensagem para limite menor que o mínimo. */
    private static final String DAYS_TOO_SMALL = "O limite de idade deve ser de pelo menos %d dia(s).";

    /** Nome do parâmetro do momento, usado na mensagem de valor nulo. */
    private static final String MOMENT_NAME = "moment";

    /** Nome do parâmetro do instante atual, usado na mensagem de valor nulo. */
    private static final String NOW_NAME = "now";

    /**
     * Valida o limite.
     *
     * @throws IllegalArgumentException se {@code days} for menor que 1
     */
    public AgeLimit {
        if (days < MIN_DAYS) {
            throw new IllegalArgumentException(String.format(DAYS_TOO_SMALL, MIN_DAYS));
        }
    }

    /**
     * Diz se o momento informado já passou do limite de idade. Só é verdadeiro quando se passaram
     * mais de {@code days} dias (de 24 horas) entre {@code moment} e {@code now}; exatamente
     * {@code days} dias ainda não excede o limite.
     *
     * @param moment data de referência do arquivo (criação ou modificação)
     * @param now    instante atual, informado por quem chama para não depender do relógio
     * @return {@code true} se a idade for maior que o limite
     * @throws NullPointerException se {@code moment} ou {@code now} for nulo
     */
    public boolean isExceededBy(Instant moment, Instant now) {
        Objects.requireNonNull(moment, MOMENT_NAME);
        Objects.requireNonNull(now, NOW_NAME);
        return Duration.between(moment, now).compareTo(Duration.ofDays(days)) > 0;
    }
}
