package com.tidyfy.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Idade máxima que um arquivo pode ter para ser mantido na limpeza, informada como uma quantidade
 * de uma unidade de tempo (dias, semanas, meses ou anos).
 *
 * @param amount quantidade de unidades (no mínimo 1)
 * @param unit   unidade de tempo
 */
public record AgeLimit(int amount, AgeUnit unit) {

    // Constantes

    /** Menor limite permitido, em dias. */
    private static final int MIN_DAYS = 1;

    /** Mensagem para limite menor que o mínimo. */
    private static final String AMOUNT_TOO_SMALL = "O limite de idade deve ser de pelo menos %d.";

    /** Mensagem para limite grande demais para ser contado em dias. */
    private static final String AMOUNT_TOO_LARGE = "O limite de idade é grande demais.";

    /** Nome do parâmetro da unidade, usado na mensagem de valor nulo. */
    private static final String UNIT_NAME = "unit";

    /** Nome do parâmetro do momento, usado na mensagem de valor nulo. */
    private static final String MOMENT_NAME = "moment";

    /** Nome do parâmetro do instante atual, usado na mensagem de valor nulo. */
    private static final String NOW_NAME = "now";

    /**
     * Valida o limite.
     *
     * @throws IllegalArgumentException se {@code amount} for menor que 1 ou se o limite em dias não couber em um inteiro
     * @throws NullPointerException     se {@code unit} for nulo
     */
    public AgeLimit {
        Objects.requireNonNull(unit, UNIT_NAME);
        if (amount < MIN_DAYS) {
            throw new IllegalArgumentException(String.format(AMOUNT_TOO_SMALL, MIN_DAYS));
        }
        if ((long) amount * unit.days() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException(AMOUNT_TOO_LARGE);
        }
    }

    /**
     * Cria um limite em dias.
     *
     * @param days limite em dias (no mínimo 1)
     */
    public AgeLimit(int days) {
        this(days, AgeUnit.DAY);
    }

    /**
     * Limite convertido em dias.
     *
     * @return a quantidade de dias equivalente
     */
    public int days() {
        return amount * unit.days();
    }

    /**
     * Diz se o momento informado já passou do limite de idade. Só é verdadeiro quando se passaram
     * mais de o limite em dias (de 24 horas) entre {@code moment} e {@code now}; exatamente
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
        return Duration.between(moment, now).compareTo(Duration.ofDays(days())) > 0;
    }
}
