package com.tidyfy.presentation.model;

import java.time.DayOfWeek;
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

    /** Modelo da frequência a cada N dias; recebe o intervalo. */
    private static final String EVERY_N_DAYS = "A cada %d dias";

    /** Unidade no singular. */
    private static final String UNIT_ONE = "dia";

    /** Unidade no plural. */
    private static final String UNIT_MANY = "dias";

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
     * Descreve a frequência em dias.
     *
     * @param intervalDays intervalo, em dias, entre as execuções
     * @return "Todo dia" ou "A cada N dias"
     */
    public static String every(int intervalDays) {
        return intervalDays == 1 ? EVERY_DAY : String.format(EVERY_N_DAYS, intervalDays);
    }

    /**
     * Devolve a unidade do intervalo, no singular ou no plural.
     *
     * @param intervalDays intervalo, em dias
     * @return "dia" ou "dias"
     */
    public static String unit(int intervalDays) {
        return intervalDays == 1 ? UNIT_ONE : UNIT_MANY;
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
     * @param intervalDays intervalo, em dias, entre as execuções
     * @param times        horários do dia
     * @return a frase, ex.: "Todo dia às 08:00 e 18:00."
     */
    public static String summary(int intervalDays, List<LocalTime> times) {
        if (times.isEmpty()) {
            return String.format(SUMMARY_WITHOUT_TIMES, every(intervalDays));
        }
        return String.format(SUMMARY_WITH_TIMES, every(intervalDays), joinTimes(times));
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
     * Informa se o ajudante executa no dia indicado. O dia 0 é hoje; o ajudante roda hoje e depois a cada
     * {@code intervalDays} dias.
     *
     * @param dayOffset    dias a partir de hoje
     * @param intervalDays intervalo, em dias, entre as execuções
     * @return {@code true} quando há execução nesse dia
     */
    public static boolean runsOn(int dayOffset, int intervalDays) {
        return dayOffset % intervalDays == 0;
    }

    /**
     * Conta as execuções dentro da prévia.
     *
     * @param intervalDays intervalo, em dias, entre as execuções
     * @param timesPerDay  horários por dia de execução
     * @return total de execuções em {@link #DAYS} dias
     */
    public static int runCount(int intervalDays, int timesPerDay) {
        int days = (DAYS - 1) / intervalDays + 1;
        return days * timesPerDay;
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
