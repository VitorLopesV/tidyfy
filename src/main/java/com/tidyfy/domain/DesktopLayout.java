package com.tidyfy.domain;

/**
 * Layout em que o Organizador da área de trabalho posiciona os ícones.
 */
public enum DesktopLayout {

    /** Colunas a partir da borda esquerda. */
    LEFT,

    /** Colunas a partir da borda direita. */
    RIGHT,

    /** Linhas a partir da borda superior. */
    TOP,

    /** Linhas a partir da borda inferior. */
    BOTTOM,

    /** Ícones distribuídos pelo perímetro da tela, com o centro livre. */
    BORDERS,

    /** Uma coluna por categoria de arquivo (mais atalhos), a partir da borda esquerda. */
    BY_TYPE_LEFT
}
