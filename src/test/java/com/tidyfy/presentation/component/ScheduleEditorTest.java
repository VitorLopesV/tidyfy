package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.domain.ScheduleUnit;
import com.tidyfy.presentation.JavaFx;
import java.time.LocalTime;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
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
            editor.setInterval(2);
            assertFalse(decrease.isDisabled());
        });
    }

    @Test
    void increaseAddsOneDay() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            ((Button) editor.lookup("#increaseInterval")).fire();

            assertEquals(2, editor.getInterval());
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

    @Test
    void startsInDaysAndOffersTheFourUnits() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();

            assertEquals(ScheduleUnit.DAY, editor.getUnit());
            for (ScheduleUnit unit : ScheduleUnit.values()) {
                assertTrue(editor.lookup("#unit" + unit.name()) instanceof ToggleButton);
            }
            assertTrue(((ToggleButton) editor.lookup("#unitDAY")).isSelected());
        });
    }

    @Test
    void choosingAUnitOptionChangesTheUnit() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();

            ((ToggleButton) editor.lookup("#unitWEEK")).fire();

            assertEquals(ScheduleUnit.WEEK, editor.getUnit());
            assertTrue(((ToggleButton) editor.lookup("#unitWEEK")).isSelected());
            assertFalse(((ToggleButton) editor.lookup("#unitDAY")).isSelected());
        });
    }

    @Test
    void settingTheUnitSelectsItsOption() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();

            editor.setUnit(ScheduleUnit.YEAR);

            assertTrue(((ToggleButton) editor.lookup("#unitYEAR")).isSelected());
        });
    }

    @Test
    void theSelectedUnitCannotBeUnselected() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();

            ((ToggleButton) editor.lookup("#unitDAY")).fire();

            assertEquals(ScheduleUnit.DAY, editor.getUnit());
            assertTrue(((ToggleButton) editor.lookup("#unitDAY")).isSelected());
        });
    }

    @Test
    void switchingToMonthsAdjustsTheIntervalToWhatTheUnitAccepts() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setInterval(5);

            editor.setUnit(ScheduleUnit.MONTH);

            assertEquals(4, editor.getInterval());
        });
    }

    @Test
    void switchingToYearsForcesTheIntervalToOneAndDisablesTheSteppers() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setInterval(9);

            editor.setUnit(ScheduleUnit.YEAR);

            assertEquals(1, editor.getInterval());
            assertTrue(((Button) editor.lookup("#increaseInterval")).isDisabled());
            assertTrue(((Button) editor.lookup("#decreaseInterval")).isDisabled());
        });
    }

    @Test
    void increaseInMonthsSkipsToTheNextAcceptedInterval() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setUnit(ScheduleUnit.MONTH);
            editor.setInterval(4);

            ((Button) editor.lookup("#increaseInterval")).fire();
            assertEquals(6, editor.getInterval());
            ((Button) editor.lookup("#increaseInterval")).fire();
            ((Button) editor.lookup("#increaseInterval")).fire();
            assertEquals(12, editor.getInterval());
            assertTrue(((Button) editor.lookup("#increaseInterval")).isDisabled());
        });
    }

    @Test
    void weeksAcceptAnyInterval() {
        JavaFx.run(() -> {
            ScheduleEditor editor = new ScheduleEditor();
            editor.setUnit(ScheduleUnit.WEEK);
            editor.setInterval(5);

            ((Button) editor.lookup("#increaseInterval")).fire();

            assertEquals(6, editor.getInterval());
        });
    }
}
