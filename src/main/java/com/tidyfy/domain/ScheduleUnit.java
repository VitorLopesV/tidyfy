package com.tidyfy.domain;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Unidade de tempo da frequência de um ajudante (dia, semana, mês ou ano). Diferente de {@link AgeUnit},
 * mês e ano seguem o calendário. Como o Agendador do Windows só sabe repetir a cada N meses dentro do ano,
 * o mês aceita apenas divisores de 12 e o ano aceita apenas 1.
 */
public enum ScheduleUnit {

    /** Dia: qualquer intervalo. */
    DAY(ChronoUnit.DAYS, List.of()),

    /** Semana: qualquer intervalo. */
    WEEK(ChronoUnit.WEEKS, List.of()),

    /** Mês: intervalos 1, 2, 3, 4, 6 ou 12. */
    MONTH(ChronoUnit.MONTHS, List.of(1, 2, 3, 4, 6, 12)),

    /** Ano: somente a cada 1 ano. */
    YEAR(ChronoUnit.YEARS, List.of(1));

    /** Unidade do {@code java.time} equivalente. */
    private final ChronoUnit chronoUnit;

    /** Intervalos aceitos, em ordem crescente; vazio quando qualquer intervalo é aceito. */
    private final List<Integer> allowed;

    /**
     * Cria a unidade.
     *
     * @param chronoUnit unidade do {@code java.time} equivalente
     * @param allowed    intervalos aceitos, em ordem crescente; vazio para qualquer um
     */
    ScheduleUnit(ChronoUnit chronoUnit, List<Integer> allowed) {
        this.chronoUnit = chronoUnit;
        this.allowed = allowed;
    }

    /**
     * Unidade do {@code java.time} equivalente.
     *
     * @return a unidade usada nas contas de datas
     */
    public ChronoUnit chronoUnit() {
        return chronoUnit;
    }

    /**
     * Diz se o intervalo é aceito por esta unidade.
     *
     * @param interval intervalo, em unidades
     * @return {@code true} se for um inteiro de pelo menos 1 e permitido pela unidade
     */
    public boolean accepts(int interval) {
        if (interval < 1) {
            return false;
        }
        return allowed.isEmpty() || allowed.contains(interval);
    }

    /**
     * Devolve o próximo intervalo aceito, maior que o informado.
     *
     * @param interval intervalo atual
     * @return o próximo intervalo aceito, ou o próprio intervalo se não houver maior
     */
    public int increase(int interval) {
        if (allowed.isEmpty()) {
            return interval + 1;
        }
        return allowed.stream().filter(value -> value > interval).findFirst().orElse(interval);
    }

    /**
     * Devolve o intervalo aceito anterior, menor que o informado (nunca menos que 1).
     *
     * @param interval intervalo atual
     * @return o intervalo aceito anterior, ou o próprio intervalo se não houver menor
     */
    public int decrease(int interval) {
        if (allowed.isEmpty()) {
            return Math.max(1, interval - 1);
        }
        return allowed.reversed().stream().filter(value -> value < interval).findFirst().orElse(interval);
    }

    /**
     * Ajusta um intervalo ao que a unidade aceita: o maior aceito que não passe do informado, ou o menor aceito.
     *
     * @param interval intervalo desejado
     * @return um intervalo aceito por esta unidade
     */
    public int adjust(int interval) {
        if (accepts(interval)) {
            return interval;
        }
        if (allowed.isEmpty()) {
            return 1;
        }
        return allowed.reversed().stream().filter(value -> value <= interval).findFirst().orElse(allowed.getFirst());
    }
}
