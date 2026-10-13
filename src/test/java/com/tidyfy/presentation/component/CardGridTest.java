package com.tidyfy.presentation.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.tidyfy.presentation.JavaFx;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

/**
 * Testa o cálculo de colunas e o posicionamento dos cartões na grade.
 */
class CardGridTest {

    @Test
    void alwaysHasAtLeastOneColumn() {
        assertEquals(1, CardGrid.columnsFor(100));
        assertEquals(1, CardGrid.columnsFor(320));
    }

    @Test
    void addsAColumnOnlyWhenTheWholeCardAndGapFit() {
        assertEquals(1, CardGrid.columnsFor(657));
        assertEquals(2, CardGrid.columnsFor(658));
        assertEquals(3, CardGrid.columnsFor(1160));
    }

    @Test
    void laysOutCardsInRowsOfEqualWidth() {
        JavaFx.run(() -> {
            CardGrid grid = new CardGrid();
            for (int i = 0; i < 4; i++) {
                Region card = new Region();
                card.setPrefHeight(100);
                grid.getChildren().add(card);
            }
            grid.resize(1012, grid.prefHeight(1012));
            grid.layout();

            Region first = (Region) grid.getChildren().get(0);
            Region fourth = (Region) grid.getChildren().get(3);
            assertEquals(325.33, first.getWidth(), 0.01);
            assertEquals(0, fourth.getLayoutX(), 0.01);
            assertEquals(118, fourth.getLayoutY(), 0.01);
            assertEquals(218, grid.prefHeight(1012), 0.01);
        });
    }
}
