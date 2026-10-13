package com.tidyfy.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Frequência de um ajudante: a cada {@code intervalDays} dias, em cada um dos horários de {@code times}.
 * A contagem do intervalo parte de {@code startDate}, o dia em que o ajudante foi salvo ou teve a frequência
 * alterada. Não há limite máximo para o intervalo: quem define é o usuário.
 *
 * @param intervalDays intervalo em dias entre as execuções (no mínimo 1)
 * @param times        horários do dia, sem repetição, guardados em ordem crescente (ao menos um)
 * @param startDate    dia de onde se conta o intervalo
 */
public record Schedule(int intervalDays, List<LocalTime> times, LocalDate startDate) {

    // Constantes

    /** Menor intervalo permitido, em dias. */
    private static final int MIN_INTERVAL_DAYS = 1;

    /** Mensagem para intervalo menor que o mínimo. */
    private static final String INTERVAL_TOO_SMALL = "O intervalo deve ser de pelo menos %d dia(s).";

    /** Mensagem para lista de horários vazia. */
    private static final String TIMES_EMPTY = "Informe pelo menos um horário.";

    /** Mensagem para horários repetidos. */
    private static final String TIMES_REPEATED = "Os horários não podem se repetir.";

    /** Nome do parâmetro de horários, usado na mensagem de valor nulo. */
    private static final String TIMES_NAME = "times";

    /** Nome do parâmetro de data de início, usado na mensagem de valor nulo. */
    private static final String START_DATE_NAME = "startDate";

    /**
     * Valida os dados e guarda os horários como lista imutável em ordem crescente.
     *
     * @throws NullPointerException     se {@code times} ou {@code startDate} for nulo
     * @throws IllegalArgumentException se o intervalo for menor que 1, ou se os horários forem vazios ou repetidos
     */
    public Schedule {
        Objects.requireNonNull(times, TIMES_NAME);
        Objects.requireNonNull(startDate, START_DATE_NAME);
        if (intervalDays < MIN_INTERVAL_DAYS) {
            throw new IllegalArgumentException(String.format(INTERVAL_TOO_SMALL, MIN_INTERVAL_DAYS));
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
}
