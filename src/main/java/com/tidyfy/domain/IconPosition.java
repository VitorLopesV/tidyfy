package com.tidyfy.domain;

/**
 * Posição de um ícone da área de trabalho, em pixels, a partir do canto superior esquerdo da área de trabalho.
 *
 * @param x distância da borda esquerda
 * @param y distância da borda superior
 */
public record IconPosition(int x, int y) {
}
