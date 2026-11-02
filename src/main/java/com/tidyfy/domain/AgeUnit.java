package com.tidyfy.domain;

/**
 * Unidade de tempo do limite de idade da limpeza. Mês e ano são aproximações de tamanho fixo (30 e 365 dias),
 * para que o limite não dependa do calendário.
 */
public enum AgeUnit {

    /** Dia. */
    DAY(1),

    /** Semana (7 dias). */
    WEEK(7),

    /** Mês (30 dias). */
    MONTH(30),

    /** Ano (365 dias). */
    YEAR(365);

    /** Quantidade de dias que a unidade vale. */
    private final int days;

    /**
     * Cria a unidade.
     *
     * @param days quantidade de dias que ela vale
     */
    AgeUnit(int days) {
        this.days = days;
    }

    /**
     * Quantidade de dias de uma unidade.
     *
     * @return os dias que a unidade vale
     */
    public int days() {
        return days;
    }
}
