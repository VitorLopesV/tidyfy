package com.tidyfy.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;

/**
 * Calcula, a partir de um {@link Schedule} e do relógio, quando é a próxima execução do ajudante e
 * quanto do intervalo até ela já passou.
 * <p>
 * O ajudante roda no dia de início ({@code startDate}) e depois a cada {@code intervalDays} dias, em cada um
 * dos horários do {@code Schedule}. Um horário igual ao instante atual conta como já executado: a próxima
 * execução é sempre estritamente depois do instante atual. Os cálculos usam o fuso do relógio injetado.
 */
public final class ScheduleCalculator {

    /** Relógio que define o instante atual. */
    private final Clock clock;

    // Constantes

    /** Nome do parâmetro do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /** Nome do parâmetro da frequência, usado na mensagem de valor nulo. */
    private static final String SCHEDULE_NAME = "schedule";

    /** Progresso de quem ainda não tem execução anterior. */
    private static final double NO_PROGRESS = 0.0;

    /**
     * Cria o calculador.
     *
     * @param clock relógio que informa o instante atual
     * @throws NullPointerException se {@code clock} for nulo
     */
    public ScheduleCalculator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, CLOCK_NAME);
    }

    /**
     * Devolve a data e hora da próxima execução: o primeiro horário estritamente depois do instante atual,
     * num dia válido. Antes do dia de início, é o primeiro horário do dia de início.
     *
     * @param schedule frequência do ajudante
     * @return a próxima execução
     * @throws NullPointerException se {@code schedule} for nulo
     */
    public LocalDateTime nextExecution(Schedule schedule) {
        Objects.requireNonNull(schedule, SCHEDULE_NAME);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        long elapsed = ChronoUnit.DAYS.between(schedule.startDate(), today);
        if (elapsed < 0) {
            return schedule.startDate().atTime(firstTime(schedule));
        }
        long sinceLastValidDay = elapsed % schedule.intervalDays();
        if (sinceLastValidDay == 0) {
            for (LocalTime time : schedule.times()) {
                if (time.isAfter(now.toLocalTime())) {
                    return today.atTime(time);
                }
            }
            return today.plusDays(schedule.intervalDays()).atTime(firstTime(schedule));
        }
        long untilNextValidDay = schedule.intervalDays() - sinceLastValidDay;
        return today.plusDays(untilNextValidDay).atTime(firstTime(schedule));
    }

    /**
     * Devolve a data e hora da execução anterior: o último horário até o instante atual (inclusive),
     * num dia válido. Não há execução anterior antes da primeira execução.
     *
     * @param schedule frequência do ajudante
     * @return a execução anterior, ou vazio se ainda não houve nenhuma
     * @throws NullPointerException se {@code schedule} for nulo
     */
    public Optional<LocalDateTime> previousExecution(Schedule schedule) {
        Objects.requireNonNull(schedule, SCHEDULE_NAME);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        long elapsed = ChronoUnit.DAYS.between(schedule.startDate(), today);
        if (elapsed < 0) {
            return Optional.empty();
        }
        long sinceLastValidDay = elapsed % schedule.intervalDays();
        if (sinceLastValidDay == 0) {
            for (int i = schedule.times().size() - 1; i >= 0; i--) {
                LocalTime time = schedule.times().get(i);
                if (!time.isAfter(now.toLocalTime())) {
                    return Optional.of(today.atTime(time));
                }
            }
            if (elapsed == 0) {
                return Optional.empty();
            }
            return Optional.of(today.minusDays(schedule.intervalDays()).atTime(lastTime(schedule)));
        }
        return Optional.of(today.minusDays(sinceLastValidDay).atTime(lastTime(schedule)));
    }

    /**
     * Devolve quanto do intervalo entre a execução anterior e a próxima já passou, de 0 a 1.
     * Antes da primeira execução o progresso é 0.
     *
     * @param schedule frequência do ajudante
     * @return o progresso, de 0 (acabou de executar) a 1 (prestes a executar)
     * @throws NullPointerException se {@code schedule} for nulo
     */
    public double progress(Schedule schedule) {
        Optional<LocalDateTime> previous = previousExecution(schedule);
        if (previous.isEmpty()) {
            return NO_PROGRESS;
        }
        LocalDateTime next = nextExecution(schedule);
        Duration total = Duration.between(previous.get(), next);
        Duration passed = Duration.between(previous.get(), LocalDateTime.now(clock));
        return (double) passed.toNanos() / total.toNanos();
    }

    /**
     * Devolve o primeiro horário do dia.
     *
     * @param schedule frequência do ajudante
     * @return o menor horário
     */
    private LocalTime firstTime(Schedule schedule) {
        return schedule.times().get(0);
    }

    /**
     * Devolve o último horário do dia.
     *
     * @param schedule frequência do ajudante
     * @return o maior horário
     */
    private LocalTime lastTime(Schedule schedule) {
        return schedule.times().get(schedule.times().size() - 1);
    }
}
