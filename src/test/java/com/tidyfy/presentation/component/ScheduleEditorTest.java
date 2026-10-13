package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.presentation.JavaFx;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;

/**
 * Testa o editor de frequência: intervalo e horários.
 */
class ScheduleEditorTest {

    @Test
    void intervalCannotGoBelowOneDay() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            Button decrease = (Button) editor.lookup("#decreaseInterval");

            assertTrue(decrease.isDisabled());
            editor.setIntervalDays(2);
            assertFalse(decrease.isDisabled());
        });
    }

    @Test
    void increaseAddsOneDay() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            ((Button) editor.lookup("#increaseInterval")).fire();

            assertEquals(2, editor.getIntervalDays());
        });
    }

    @Test
    void keepsTimesSortedWhenAdding() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.addTime(LocalTime.of(18, 0));
            editor.addTime(LocalTime.of(9, 0));

            assertEquals(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)), editor.getTimes());
        });
    }

    @Test
    void rejectsARepeatedTime() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            assertTrue(editor.addTime(LocalTime.of(9, 0)));

            assertFalse(editor.addTime(LocalTime.of(9, 0)));
            assertEquals(1, editor.getTimes().size());
        });
    }

    @Test
    void setTimesDropsRepeatedTimes() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setTimes(List.of(LocalTime.NOON, LocalTime.NOON, LocalTime.MIDNIGHT));

            assertEquals(List.of(LocalTime.MIDNIGHT, LocalTime.NOON), editor.getTimes());
        });
    }

    @Test
    void lastRemainingTimeCannotBeRemoved() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setTimes(List.of(LocalTime.of(9, 0)));

            assertTrue(removeButtons(editor).getFirst().isDisabled());
        });
    }

    @Test
    void removingATimeTakesItOutOfTheList() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setTimes(List.of(LocalTime.of(9, 0), LocalTime.of(18, 0)));

            removeButtons(editor).getFirst().fire();

            assertEquals(List.of(LocalTime.of(18, 0)), editor.getTimes());
        });
    }

    private static List<Button> removeButtons(ScheduleEditor editor) {
        return editor.lookupAll(".chip-remove").stream().map(Node.class::cast).map(Button.class::cast).toList();
    }
}
