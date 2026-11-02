package com.tidyfy.presentation.model;

import com.tidyfy.domain.AgeUnit;
import com.tidyfy.domain.Schedule;
import com.tidyfy.domain.ScheduleUnit;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Textos e contas da prévia de frequência mostrada nos formulários e nos cartões.
 */
public final class SchedulePreview {

    // Constantes

    /** Quantidade de dias que a prévia cobre. */
    public static final int DAYS = 14;

    /** Abreviações dos dias da semana, de segunda a domingo. */
    private static final String[] WEEKDAYS = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};

    /** Padrão de exibição dos horários. */
    private static final String TIME_PATTERN = "HH:mm";

    /** Formato de exibição dos horários. */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern(TIME_PATTERN);

    /** Texto da frequência diária. */
    private static final String EVERY_DAY = "Todo dia";

    /** Texto da frequência semanal. */
    private static final String EVERY_WEEK = "Toda semana";

    /** Texto da frequência mensal. */
    private static final String EVERY_MONTH = "Todo mês";

    /** Texto da frequência anual. */
    private static final String EVERY_YEAR = "Todo ano";

    /** Modelo da frequência a cada N unidades; recebe o intervalo e a unidade no plural. */
    private static final String EVERY_N = "A cada %d %s";

    /** Unidade no singular. */
    private static final String UNIT_ONE = "dia";

    /** Unidade no plural. */
    private static final String UNIT_MANY = "dias";

    /** Semana no singular. */
    private static final String WEEK_ONE = "semana";

    /** Semana no plural. */
    private static final String WEEK_MANY = "semanas";

    /** Mês no singular. */
    private static final String MONTH_ONE = "mês";

    /** Mês no plural. */
    private static final String MONTH_MANY = "meses";

    /** Ano no singular. */
    private static final String YEAR_ONE = "ano";

    /** Ano no plural. */
    private static final String YEAR_MANY = "anos";

    /** Modelo do resumo sem horários; recebe a frequência. */
    private static final String SUMMARY_WITHOUT_TIMES = "%s.";

    /** Modelo do resumo com horários; recebe a frequência e os horários. */
    private static final String SUMMARY_WITH_TIMES = "%s às %s.";

    /** Separador entre horários, exceto antes do último. */
    private static final String TIMES_SEPARATOR = ", ";

    /** Modelo que liga o último horário aos anteriores; recebe os anteriores e o último. */
    private static final String TIMES_LAST = "%s e %s";

    /** Texto para uma execução. */
    private static final String RUNS_ONE = "1 execução";

    /** Modelo do texto para várias execuções; recebe a quantidade. */
    private static final String RUNS_MANY = "%d execuções";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private SchedulePreview() {
    }

    /**
     * Descreve a frequência.
     *
     * @param interval intervalo entre as execuções
     * @param unit     unidade do intervalo
     * @return "Todo dia", "Toda semana", "Todo mês", "Todo ano" ou "A cada N" seguido da unidade no plural
     */
    public static String every(int interval, ScheduleUnit unit) {
        if (interval == 1) {
            return switch (unit) {
                case DAY -> EVERY_DAY;
                case WEEK -> EVERY_WEEK;
                case MONTH -> EVERY_MONTH;
                case YEAR -> EVERY_YEAR;
            };
        }
        return String.format(EVERY_N, interval, unit(interval, unit));
    }

    /**
     * Devolve o nome da unidade da frequência, no singular ou no plural.
     *
     * @param interval intervalo entre as execuções
     * @param unit     unidade do intervalo
     * @return ex.: "semana" para 1 e "semanas" para 2
     */
    public static String unit(int interval, ScheduleUnit unit) {
        AgeUnit equivalent = switch (unit) {
            case DAY -> AgeUnit.DAY;
            case WEEK -> AgeUnit.WEEK;
            case MONTH -> AgeUnit.MONTH;
            case YEAR -> AgeUnit.YEAR;
        };
        return ageUnit(equivalent, interval);
    }

    /**
     * Devolve o nome da unidade do limite de idade, no singular ou no plural.
     *
     * @param unit   unidade do limite
     * @param amount quantidade de unidades
     * @return ex.: "semana" para 1 e "semanas" para 2
     */
    public static String ageUnit(AgeUnit unit, int amount) {
        boolean one = amount == 1;
        return switch (unit) {
            case DAY -> one ? UNIT_ONE : UNIT_MANY;
            case WEEK -> one ? WEEK_ONE : WEEK_MANY;
            case MONTH -> one ? MONTH_ONE : MONTH_MANY;
            case YEAR -> one ? YEAR_ONE : YEAR_MANY;
        };
    }

    /**
     * Formata um horário como HH:mm.
     *
     * @param time horário a formatar
     * @return o horário formatado
     */
    public static String time(LocalTime time) {
        return TIME.format(time);
    }

    /**
     * Resume a frequência e os horários em uma frase.
     *
     * @param interval intervalo entre as execuções
     * @param unit     unidade do intervalo
     * @param times    horários do dia
     * @return a frase, ex.: "Todo dia às 08:00 e 18:00."
     */
    public static String summary(int interval, ScheduleUnit unit, List<LocalTime> times) {
        if (times.isEmpty()) {
            return String.format(SUMMARY_WITHOUT_TIMES, every(interval, unit));
        }
        return String.format(SUMMARY_WITH_TIMES, every(interval, unit), joinTimes(times));
    }

    /**
     * Junta os horários em ordem crescente, separados por vírgula e com "e" antes do último.
     *
     * @param times horários a juntar
     * @return o texto, ex.: "08:00, 12:00 e 18:00"
     */
    static String joinTimes(List<LocalTime> times) {
        List<String> formatted = times.stream().sorted().map(SchedulePreview::time).toList();
        if (formatted.size() == 1) {
            return formatted.getFirst();
        }
        String leading = String.join(TIMES_SEPARATOR, formatted.subList(0, formatted.size() - 1));
        return String.format(TIMES_LAST, leading, formatted.getLast());
    }

    /**
     * Informa se o ajudante executa no dia indicado. O dia 0 é hoje, o dia de início da frequência.
     *
     * @param today     dia de hoje
     * @param dayOffset dias a partir de hoje
     * @param interval  intervalo entre as execuções
     * @param unit      unidade do intervalo
     * @return {@code true} quando há execução nesse dia
     */
    public static boolean runsOn(LocalDate today, int dayOffset, int interval, ScheduleUnit unit) {
        return previewSchedule(today, interval, unit).occursOn(today.plusDays(dayOffset));
    }

    /**
     * Conta as execuções dentro da prévia.
     *
     * @param today       dia de hoje
     * @param interval    intervalo entre as execuções
     * @param unit        unidade do intervalo
     * @param timesPerDay horários por dia de execução
     * @return total de execuções em {@link #DAYS} dias
     */
    public static int runCount(LocalDate today, int interval, ScheduleUnit unit, int timesPerDay) {
        int days = 0;
        for (int offset = 0; offset < DAYS; offset++) {
            if (runsOn(today, offset, interval, unit)) {
                days++;
            }
        }
        return days * timesPerDay;
    }

    /**
     * Monta a frequência da prévia, que começa hoje.
     *
     * @param today    dia de hoje
     * @param interval intervalo entre as execuções
     * @param unit     unidade do intervalo
     * @return a frequência, com um único horário (só os dias importam)
     */
    private static Schedule previewSchedule(LocalDate today, int interval, ScheduleUnit unit) {
        return new Schedule(interval, unit, List.of(LocalTime.MIDNIGHT), today);
    }

    /**
     * Descreve uma quantidade de execuções.
     *
     * @param runs quantidade de execuções
     * @return "1 execução" ou "N execuções"
     */
    public static String runsLabel(int runs) {
        return runs == 1 ? RUNS_ONE : String.format(RUNS_MANY, runs);
    }

    /**
     * Devolve a abreviação do dia da semana.
     *
     * @param day dia da semana
     * @return abreviação em português, ex.: "seg"
     */
    public static String weekday(DayOfWeek day) {
        return WEEKDAYS[day.getValue() - 1];
    }
}
