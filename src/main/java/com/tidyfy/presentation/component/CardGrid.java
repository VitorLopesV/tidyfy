package com.tidyfy.presentation.component;

import java.util.List;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * Grade responsiva: quantas colunas de pelo menos 320 px couberem, com linhas da altura do maior cartão.
 */
public final class CardGrid extends Pane {

    // Constantes

    /** Largura mínima de cada coluna, em pixels. */
    static final double MIN_CELL_WIDTH = 320;

    /** Espaço entre as células, em pixels. */
    static final double GAP = 18;

    /** Classe CSS do componente. */
    private static final String STYLE_CLASS = "card-grid";

    /**
     * Cria a grade vazia.
     */
    public CardGrid() {
        getStyleClass().add(STYLE_CLASS);
    }

    /**
     * Calcula quantas colunas cabem na largura informada (no mínimo uma).
     *
     * @param width largura disponível, em pixels
     * @return quantidade de colunas
     */
    public static int columnsFor(double width) {
        return Math.max(1, (int) Math.floor((width + GAP) / (MIN_CELL_WIDTH + GAP)));
    }

    /**
     * Informa que a altura da grade depende da largura.
     *
     * @return {@link Orientation#HORIZONTAL}
     */
    @Override
    public Orientation getContentBias() {
        return Orientation.HORIZONTAL;
    }

    /**
     * Calcula a largura mínima: uma coluna mais as margens internas.
     *
     * @param height altura de referência (não usada)
     * @return largura mínima, em pixels
     */
    @Override
    protected double computeMinWidth(double height) {
        return snappedLeftInset() + MIN_CELL_WIDTH + snappedRightInset();
    }

    /**
     * Calcula a largura preferida, igual à mínima.
     *
     * @param height altura de referência (não usada)
     * @return largura preferida, em pixels
     */
    @Override
    protected double computePrefWidth(double height) {
        return computeMinWidth(height);
    }

    /**
     * Calcula a altura preferida para a largura informada.
     *
     * @param width largura disponível, ou negativa quando não definida
     * @return altura preferida, em pixels
     */
    @Override
    protected double computePrefHeight(double width) {
        double outer = width < 0 ? computePrefWidth(-1) : width;
        double inner = outer - snappedLeftInset() - snappedRightInset();
        return snappedTopInset() + arrange(inner, false) + snappedBottomInset();
    }

    /**
     * Posiciona os cartões nas células.
     */
    @Override
    protected void layoutChildren() {
        arrange(getWidth() - snappedLeftInset() - snappedRightInset(), true);
    }

    /**
     * Distribui os cartões em linhas e colunas.
     *
     * @param innerWidth largura útil, sem as margens internas
     * @param apply      {@code true} para posicionar os cartões; {@code false} para só medir
     * @return altura total ocupada pelas linhas
     */
    private double arrange(double innerWidth, boolean apply) {
        List<Node> cards = getManagedChildren();
        if (cards.isEmpty()) {
            return 0;
        }
        int columns = columnsFor(innerWidth);
        double cellWidth = (innerWidth - GAP * (columns - 1)) / columns;
        double height = 0;
        for (int start = 0; start < cards.size(); start += columns) {
            int end = Math.min(start + columns, cards.size());
            double rowHeight = 0;
            for (int i = start; i < end; i++) {
                rowHeight = Math.max(rowHeight, cards.get(i).prefHeight(cellWidth));
            }
            if (apply) {
                for (int i = start; i < end; i++) {
                    double x = snappedLeftInset() + (i - start) * (cellWidth + GAP);
                    cards.get(i).resizeRelocate(x, snappedTopInset() + height, cellWidth, rowHeight);
                }
            }
            height += rowHeight + (end < cards.size() ? GAP : 0);
        }
        return height;
    }
}
