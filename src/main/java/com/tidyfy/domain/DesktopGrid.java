package com.tidyfy.domain;

/**
 * Área visível da área de trabalho no monitor principal e a grade de células dos ícones, em pixels.
 * Os valores de {@code left} e {@code top} são relativos ao canto superior esquerdo da área de trabalho
 * (num cenário com vários monitores o monitor principal pode não começar em zero).
 *
 * @param left       borda esquerda da área visível
 * @param top        borda superior da área visível
 * @param width      largura da área visível (descontada a barra de tarefas)
 * @param height     altura da área visível (descontada a barra de tarefas)
 * @param cellWidth  largura da célula de um ícone
 * @param cellHeight altura da célula de um ícone
 */
public record DesktopGrid(int left, int top, int width, int height, int cellWidth, int cellHeight) {

    // Constantes

    /** Mensagem quando alguma medida não é positiva. */
    private static final String NON_POSITIVE_MESSAGE = "width, height, cellWidth and cellHeight must be positive";

    /**
     * Valida as medidas.
     *
     * @throws IllegalArgumentException se largura, altura ou o tamanho da célula não for positivo
     */
    public DesktopGrid {
        if (width <= 0 || height <= 0 || cellWidth <= 0 || cellHeight <= 0) {
            throw new IllegalArgumentException(NON_POSITIVE_MESSAGE);
        }
    }
}
