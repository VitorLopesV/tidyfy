package com.tidyfy.presentation.component;

import com.tidyfy.presentation.model.SchedulePreview;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Calendário dos próximos 14 dias com os dias em que o ajudante vai rodar destacados.
 */
public final class UpcomingRunsCalendar extends VBox {

    /** Rótulos dos dias, do primeiro (hoje) ao último. */
    private final List<Label> days = new ArrayList<>();

    /** Grade com os dias da semana e os dias do mês. */
    private final GridPane grid = new GridPane();

    /** Quantidade de execuções no período. */
    private final Label badge = new Label();

    /** Frase que resume a frequência e os horários. */
    private final Label summary = new Label();

    /** Atualiza o calendário quando o intervalo ou os horários mudam. */
    private final InvalidationListener refresh = observable -> refresh(true);

    /** Intervalo, em dias, entre as execuções. */
    private IntegerProperty intervalDays = new SimpleIntegerProperty(1);

    /** Horários do dia em que o ajudante executa. */
    private ObservableList<LocalTime> times = FXCollections.observableArrayList();

    // Constantes

    /** Quantidade de colunas da grade: uma por dia da semana. */
    private static final int WEEK_DAYS = 7;

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "upcoming-runs";

    /** Classe CSS do ícone. */
    private static final String ICON_STYLE_CLASS = "ink-icon";

    /** Modelo do título; recebe a quantidade de dias. */
    private static final String TITLE = "Próximos %d dias";

    /** Classe CSS do título. */
    private static final String TITLE_STYLE_CLASS = "h2";

    /** Classe CSS do contador de execuções. */
    private static final String BADGE_STYLE_CLASS = "runs-badge";

    /** Classe CSS da grade. */
    private static final String GRID_STYLE_CLASS = "calendar-grid";

    /** Classe CSS do nome do dia da semana. */
    private static final String WEEKDAY_STYLE_CLASS = "weekday";

    /** Classe CSS do dia do mês. */
    private static final String DAY_STYLE_CLASS = "day";

    /** Classe CSS do dia de hoje. */
    private static final String TODAY_STYLE_CLASS = "today";

    /** Classe CSS dos dias em que o ajudante executa. */
    private static final String RUN_STYLE_CLASS = "run";

    /** Classe CSS da frase de resumo. */
    private static final String SUMMARY_STYLE_CLASS = "summary";

    /** Modelo do texto de acessibilidade; recebe o texto das execuções e a quantidade de dias. */
    private static final String ACCESSIBLE_TEXT = "%s nos próximos %d dias";

    /**
     * Cria o calendário a partir de hoje.
     */
    public UpcomingRunsCalendar() {
        this(LocalDate.now());
    }

    /**
     * Cria o calendário a partir de uma data (usado nos testes para fixar o dia).
     *
     * @param today primeiro dia exibido
     */
    UpcomingRunsCalendar(LocalDate today) {
        getStyleClass().add(STYLE_CLASS);
        setSpacing(12);

        Icon icon = new Icon(Glyph.CALENDAR, 18);
        icon.getStyleClass().add(ICON_STYLE_CLASS);
        Label title = new Label(String.format(TITLE, SchedulePreview.DAYS));
        title.getStyleClass().add(TITLE_STYLE_CLASS);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        badge.getStyleClass().add(BADGE_STYLE_CLASS);
        HBox header = new HBox(8, icon, title, spacer, badge);
        header.setAlignment(Pos.CENTER_LEFT);

        grid.getStyleClass().add(GRID_STYLE_CLASS);
        grid.setHgap(6);
        grid.setVgap(6);
        for (int column = 0; column < WEEK_DAYS; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(100.0 / WEEK_DAYS);
            constraints.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(constraints);
            Label weekday = new Label(SchedulePreview.weekday(today.plusDays(column).getDayOfWeek()));
            weekday.getStyleClass().add(WEEKDAY_STYLE_CLASS);
            grid.add(weekday, column, 0);
        }
        for (int i = 0; i < SchedulePreview.DAYS; i++) {
            Label day = new Label(String.valueOf(today.plusDays(i).getDayOfMonth()));
            day.getStyleClass().add(DAY_STYLE_CLASS);
            if (i == 0) {
                day.getStyleClass().add(TODAY_STYLE_CLASS);
            }
            day.setMaxWidth(Double.MAX_VALUE);
            grid.add(day, i % WEEK_DAYS, 1 + i / WEEK_DAYS);
            days.add(day);
            Animations.riseIn(day, Duration.millis(100 + i * 30L));
        }

        summary.getStyleClass().add(SUMMARY_STYLE_CLASS);
        summary.setWrapText(true);
        getChildren().addAll(header, grid, summary);
        refresh(false);
    }

    /**
     * Liga o calendário ao intervalo e aos horários do formulário; ele se atualiza quando eles mudam.
     *
     * @param intervalDays intervalo, em dias, entre as execuções
     * @param times        horários do dia
     */
    public void bind(IntegerProperty intervalDays, ObservableList<LocalTime> times) {
        this.intervalDays.removeListener(refresh);
        this.times.removeListener(refresh);
        this.intervalDays = intervalDays;
        this.times = times;
        intervalDays.addListener(refresh);
        times.addListener(refresh);
        refresh(false);
    }

    /**
     * Destaca os dias de execução e atualiza o contador e o resumo.
     *
     * @param animate {@code true} para animar os dias que passam a ser destacados
     */
    private void refresh(boolean animate) {
        int interval = intervalDays.get();
        for (int i = 0; i < days.size(); i++) {
            Label day = days.get(i);
            boolean runs = SchedulePreview.runsOn(i, interval);
            boolean marked = day.getStyleClass().contains(RUN_STYLE_CLASS);
            if (runs && !marked) {
                day.getStyleClass().add(RUN_STYLE_CLASS);
                if (animate) {
                    Animations.pop(day);
                }
            } else if (!runs && marked) {
                day.getStyleClass().remove(RUN_STYLE_CLASS);
            }
        }
        String runs = SchedulePreview.runsLabel(SchedulePreview.runCount(interval, times.size()));
        badge.setText(runs);
        grid.setAccessibleText(String.format(ACCESSIBLE_TEXT, runs, SchedulePreview.DAYS));
        summary.setText(SchedulePreview.summary(interval, times));
    }
}
