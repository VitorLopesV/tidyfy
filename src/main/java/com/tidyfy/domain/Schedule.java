package com.tidyfy.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Frequência de um ajudante: a cada {@code interval} {@code unit}s (dias, semanas, meses ou anos), em cada um
 * dos horários de {@code times}. A contagem parte de {@code startDate}, o dia em que o ajudante foi salvo ou
 * teve a frequência alterada, e cada execução cai no mesmo dia da semana (semana) ou no mesmo dia do mês (mês e
 * ano) da data de início. Quando o dia de início é 29, 30 ou 31, a execução mensal cai no último dia do mês; um
 * ano a partir de 29 de fevereiro cai em 28 de fevereiro nos anos não bissextos.
 * Não há limite máximo para o intervalo, exceto o que a unidade aceita ({@link ScheduleUnit#accepts(int)}).
 *
 * @param interval  intervalo entre as execuções, na unidade informada (no mínimo 1)
 * @param unit      unidade do intervalo
 * @param times     horários do dia, sem repetição, guardados em ordem crescente (ao menos um)
 * @param startDate dia de onde se conta o intervalo
 */
public record Schedule(int interval, ScheduleUnit unit, List<LocalTime> times, LocalDate startDate) {

    // Constantes

    /** Menor intervalo permitido. */
    private static final int MIN_INTERVAL = 1;

    /** Primeiro dia do mês em que a execução mensal cai no último dia do mês. */
    private static final int FIRST_LAST_DAY_OF_MONTH = 29;

    /** Mensagem para intervalo menor que o mínimo. */
    private static final String INTERVAL_TOO_SMALL = "O intervalo deve ser de pelo menos %d.";

    /** Mensagem para intervalo que a unidade não aceita. */
    private static final String INTERVAL_NOT_ACCEPTED =
            "Esse intervalo não é aceito: em meses use 1, 2, 3, 4, 6 ou 12; em anos, apenas 1.";

    /** Mensagem para lista de horários vazia. */
    private static final String TIMES_EMPTY = "Informe pelo menos um horário.";

    /** Mensagem para horários repetidos. */
    private static final String TIMES_REPEATED = "Os horários não podem se repetir.";

    /** Nome do parâmetro da unidade, usado na mensagem de valor nulo. */
    private static final String UNIT_NAME = "unit";

    /** Nome do parâmetro de horários, usado na mensagem de valor nulo. */
    private static final String TIMES_NAME = "times";

    /** Nome do parâmetro de data de início, usado na mensagem de valor nulo. */
    private static final String START_DATE_NAME = "startDate";

    /** Nome do parâmetro da data consultada, usado na mensagem de valor nulo. */
    private static final String DATE_NAME = "date";

    /**
     * Valida os dados e guarda os horários como lista imutável em ordem crescente.
     *
     * @throws NullPointerException     se {@code unit}, {@code times} ou {@code startDate} for nulo
     * @throws IllegalArgumentException se o intervalo for menor que 1 ou não aceito pela unidade, ou se os
     *                                  horários forem vazios ou repetidos
     */
    public Schedule {
        Objects.requireNonNull(unit, UNIT_NAME);
        Objects.requireNonNull(times, TIMES_NAME);
        Objects.requireNonNull(startDate, START_DATE_NAME);
        if (interval < MIN_INTERVAL) {
            throw new IllegalArgumentException(String.format(INTERVAL_TOO_SMALL, MIN_INTERVAL));
        }
        if (!unit.accepts(interval)) {
            throw new IllegalArgumentException(INTERVAL_NOT_ACCEPTED);
        }
        if (times.isEmpty()) {
            throw new IllegalArgumentException(TIMES_EMPTY);
        }
        TreeSet<LocalTime> sorted = new TreeSet<>(times);
        if (sorted.size() != times.size()) {
            throw new IllegalArgumentException(TIMES_REPEATED);
        }
        times = List.copyOf(sorted);
    }

    /**
     * Cria uma frequência em dias.
     *
     * @param intervalDays intervalo em dias entre as execuções (no mínimo 1)
     * @param times        horários do dia, sem repetição (ao menos um)
     * @param startDate    dia de onde se conta o intervalo
     */
    public Schedule(int intervalDays, List<LocalTime> times, LocalDate startDate) {
        this(intervalDays, ScheduleUnit.DAY, times, startDate);
    }

    /**
     * Devolve o dia da execução de número {@code index}; a de número 0 é o dia de início.
     *
     * @param index posição da execução, a partir de 0
     * @return o dia dessa execução
     */
    public LocalDate occurrence(long index) {
        long steps = index * interval;
        return switch (unit) {
            case DAY -> startDate.plusDays(steps);
            case WEEK -> startDate.plusWeeks(steps);
            case MONTH -> monthlyOccurrence(steps);
            case YEAR -> startDate.plusYears(steps);
        };
    }

    /**
     * Devolve a posição da primeira execução que cai em {@code date} ou depois dela. Para datas até o dia de
     * início, é a execução 0.
     *
     * @param date dia de referência
     * @return a posição da primeira execução não anterior a {@code date}
     * @throws NullPointerException se {@code date} for nulo
     */
    public long indexOnOrAfter(LocalDate date) {
        Objects.requireNonNull(date, DATE_NAME);
        if (!date.isAfter(startDate)) {
            return 0;
        }
        long index = Math.max(0, unit.chronoUnit().between(startDate, date) / interval - 1);
        while (occurrence(index).isBefore(date)) {
            index++;
        }
        return index;
    }

    /**
     * Diz se o ajudante executa no dia informado.
     *
     * @param date dia consultado
     * @return {@code true} se o dia é um dia de execução
     * @throws NullPointerException se {@code date} for nulo
     */
    public boolean occursOn(LocalDate date) {
        return occurrence(indexOnOrAfter(date)).equals(date);
    }

    /**
     * Calcula o dia de uma execução mensal: o mesmo dia do mês do início, ou o último dia do mês quando o
     * início foi no dia 29 ou depois.
     *
     * @param months meses a somar à data de início
     * @return o dia da execução
     */
    private LocalDate monthlyOccurrence(long months) {
        LocalDate date = startDate.plusMonths(months);
        if (startDate.getDayOfMonth() >= FIRST_LAST_DAY_OF_MONTH) {
            return date.withDayOfMonth(date.lengthOfMonth());
        }
        return date;
    }
}
