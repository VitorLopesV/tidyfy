package com.tidyfy.presentation.model;

import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.domain.ScheduleCalculator;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Converte os ajudantes do domínio nos {@link HelperItem} que a interface exibe, com o texto da
 * próxima execução e o progresso até ela. Ajudante desligado não tem próxima execução.
 */
public final class HelperItemMapper {

    /** Calculador da próxima execução e do progresso. */
    private final ScheduleCalculator calculator;

    /** Relógio que define o dia de hoje. */
    private final Clock clock;

    // Constantes

    /** Nome do parâmetro do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /** Padrão de exibição da data da próxima execução, para dias além de amanhã. */
    private static final String DATE_PATTERN = "dd/MM";

    /** Formato de exibição da data da próxima execução. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern(DATE_PATTERN);

    /** Modelo do texto da próxima execução hoje; recebe o horário. */
    private static final String TODAY = "Hoje, %s";

    /** Modelo do texto da próxima execução amanhã; recebe o horário. */
    private static final String TOMORROW = "Amanhã, %s";

    /** Modelo do texto da próxima execução em outro dia; recebe a data e o horário. */
    private static final String OTHER_DAY = "%s, %s";

    /** Progresso de quem não tem próxima execução. */
    private static final double NO_PROGRESS = 0.0;

    /**
     * Cria o mapeador.
     *
     * @param clock relógio que informa o instante atual
     * @throws NullPointerException se {@code clock} for nulo
     */
    public HelperItemMapper(Clock clock) {
        this.clock = Objects.requireNonNull(clock, CLOCK_NAME);
        this.calculator = new ScheduleCalculator(clock);
    }

    /**
     * Converte uma lista de ajudantes, mantendo a ordem.
     *
     * @param helpers ajudantes do domínio
     * @return os itens de tela correspondentes
     */
    public List<HelperItem> toItems(List<Helper> helpers) {
        return helpers.stream().map(this::toItem).toList();
    }

    /**
     * Converte um ajudante do domínio no item de tela.
     *
     * @param helper ajudante do domínio
     * @return o item de tela
     */
    public HelperItem toItem(Helper helper) {
        Schedule schedule = helper.schedule();
        String nextRun = helper.enabled() ? describe(calculator.nextExecution(schedule)) : null;
        double progress = helper.enabled() ? calculator.progress(schedule) : NO_PROGRESS;
        String id = helper.id().value().toString();
        String name = helper.name().value();
        return switch (helper) {
            case FolderOrganizerHelper organizer -> new HelperItem(id, name, HelperKind.ORGANIZER,
                    organizer.folder(), schedule.intervalDays(), schedule.times(), organizer.enabled(),
                    nextRun, progress, null, null);
            case CleanerHelper cleaner -> toCleanerItem(cleaner, id, name, nextRun, progress);
        };
    }

    /**
     * Converte um ajudante de limpeza.
     *
     * @param cleaner  ajudante de limpeza
     * @param id       identificador em texto
     * @param name     nome do ajudante
     * @param nextRun  texto da próxima execução
     * @param progress progresso até a próxima execução
     * @return o item de tela
     */
    private HelperItem toCleanerItem(CleanerHelper cleaner, String id, String name, String nextRun,
                                     double progress) {
        Schedule schedule = cleaner.schedule();
        Integer ageLimitDays = cleaner.findAgeLimit().map(AgeLimit::days).orElse(null);
        return switch (cleaner.target()) {
            case FolderTarget folder -> new HelperItem(id, name, HelperKind.CLEANER, folder.folder(),
                    schedule.intervalDays(), schedule.times(), cleaner.enabled(), nextRun, progress,
                    ageLimitDays, toReference(folder.ageReference()));
            case RecycleBinTarget recycleBin -> new HelperItem(id, name, HelperKind.CLEANER, null,
                    schedule.intervalDays(), schedule.times(), cleaner.enabled(), nextRun, progress,
                    ageLimitDays, null);
        };
    }

    /**
     * Converte a data de referência do domínio na da tela.
     *
     * @param reference data de referência do domínio
     * @return a data de referência da tela
     */
    private static HelperAgeReference toReference(AgeReference reference) {
        return switch (reference) {
            case CREATED -> HelperAgeReference.CREATED;
            case MODIFIED -> HelperAgeReference.MODIFIED;
        };
    }

    /**
     * Escreve a próxima execução como "Hoje, HH:mm", "Amanhã, HH:mm" ou "dd/MM, HH:mm".
     *
     * @param next data e hora da próxima execução
     * @return o texto
     */
    private String describe(LocalDateTime next) {
        LocalDate today = LocalDate.now(clock);
        String time = SchedulePreview.time(next.toLocalTime());
        if (next.toLocalDate().equals(today)) {
            return String.format(TODAY, time);
        }
        if (next.toLocalDate().equals(today.plusDays(1))) {
            return String.format(TOMORROW, time);
        }
        return String.format(OTHER_DAY, DATE.format(next), time);
    }
}
