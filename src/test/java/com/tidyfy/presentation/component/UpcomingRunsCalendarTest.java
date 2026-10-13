package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tidyfy.presentation.JavaFx;
import java.time.LocalDate;
import java.time.LocalTime;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.Test;

/**
 * Testa o calendário dos próximos dias de execução.
 */
class UpcomingRunsCalendarTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);

    @Test
    void highlightsTheDaysTheHelperRuns() {
        JavaFx.run(() -> {
            UpcomingRunsCalendar calendar = calendar(3, LocalTime.of(8, 0));

            assertEquals(5, runDays(calendar));
            assertEquals("5 execuções", ((Label) calendar.lookup(".runs-badge")).getText());
            assertEquals("A cada 3 dias às 08:00.", ((Label) calendar.lookup(".summary")).getText());
        });
    }

    @Test
    void updatesWhenTheIntervalChanges() {
        JavaFx.run(() -> {
            IntegerProperty interval = new SimpleIntegerProperty(1);
            UpcomingRunsCalendar calendar = new UpcomingRunsCalendar(MONDAY);
            calendar.bind(interval, FXCollections.observableArrayList(LocalTime.of(20, 0)));

            interval.set(7);

            assertEquals(2, runDays(calendar));
            assertEquals("2 execuções", ((Label) calendar.lookup(".runs-badge")).getText());
        });
    }

    @Test
    void countsEveryTimeOfTheDay() {
        JavaFx.run(() -> {
            UpcomingRunsCalendar calendar = calendar(1, LocalTime.of(9, 0), LocalTime.of(18, 0));

            assertEquals("28 execuções", ((Label) calendar.lookup(".runs-badge")).getText());
        });
    }

    @Test
    void weekStartsOnTodaysWeekdayAndShowsTodayFirst() {
        JavaFx.run(() -> {
            UpcomingRunsCalendar calendar = calendar(1, LocalTime.NOON);
            GridPane grid = (GridPane) calendar.lookup(".calendar-grid");

            assertEquals("seg", ((Label) grid.getChildren().get(0)).getText());
            Label today = (Label) calendar.lookup(".today");
            assertEquals("12", today.getText());
        });
    }

    private static UpcomingRunsCalendar calendar(int intervalDays, LocalTime... times) {
        UpcomingRunsCalendar calendar = new UpcomingRunsCalendar(MONDAY);
        ObservableList<LocalTime> list = FXCollections.observableArrayList(times);
        calendar.bind(new SimpleIntegerProperty(intervalDays), list);
        return calendar;
    }

    private static long runDays(UpcomingRunsCalendar calendar) {
        return calendar.lookupAll(".day").stream().filter(day -> day.getStyleClass().contains("run")).count();
    }
}
