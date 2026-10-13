package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tidyfy.presentation.JavaFx;
import org.junit.jupiter.api.Test;

/**
 * Testa o medidor de uso do limite por tipo.
 */
class UsageMeterTest {

    @Test
    void hasOneMarkPerSlotOfTheType() {
        int marks = JavaFx.call(() -> new UsageMeter().getChildren().size());

        assertEquals(20, marks);
    }

    @Test
    void lightsUpOneMarkPerCreatedHelper() {
        long lit = JavaFx.call(() -> {
            UsageMeter meter = new UsageMeter();
            meter.setUsed(4);
            return meter.getChildren().stream().filter(tick -> tick.getStyleClass().contains("on")).count();
        });

        assertEquals(4, lit);
    }

    @Test
    void describesTheUsageForScreenReaders() {
        String text = JavaFx.call(() -> {
            UsageMeter meter = new UsageMeter();
            meter.setUsed(2);
            return meter.getAccessibleText();
        });

        assertEquals("2 de 20 ajudantes criados", text);
    }
}
