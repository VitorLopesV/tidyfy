package com.tidyfy.presentation.model;

import com.tidyfy.domain.DesktopLayout;
import java.util.Objects;

/**
 * Textos em português dos layouts da área de trabalho, usados no formulário e no cartão.
 */
public final class DesktopLayoutText {

    // Constantes

    /** Nome do layout à esquerda. */
    private static final String LEFT_LABEL = "Esquerda";

    /** Nome do layout à direita. */
    private static final String RIGHT_LABEL = "Direita";

    /** Nome do layout no topo. */
    private static final String TOP_LABEL = "Topo";

    /** Nome do layout na base. */
    private static final String BOTTOM_LABEL = "Base";

    /** Nome do layout pelas bordas. */
    private static final String BORDERS_LABEL = "Bordas";

    /** Nome do layout por tipo. */
    private static final String BY_TYPE_LABEL = "Por tipo";

    /** Descrição do layout à esquerda. */
    private static final String LEFT_DESCRIPTION = "Colunas a partir da borda esquerda, em ordem de nome.";

    /** Descrição do layout à direita. */
    private static final String RIGHT_DESCRIPTION = "Colunas a partir da borda direita, em ordem de nome.";

    /** Descrição do layout no topo. */
    private static final String TOP_DESCRIPTION = "Linhas a partir da borda superior, em ordem de nome.";

    /** Descrição do layout na base. */
    private static final String BOTTOM_DESCRIPTION = "Linhas a partir da borda inferior, em ordem de nome.";

    /** Descrição do layout pelas bordas. */
    private static final String BORDERS_DESCRIPTION = "Pelo contorno da tela, com o centro livre.";

    /** Descrição do layout por tipo. */
    private static final String BY_TYPE_DESCRIPTION =
            "Uma coluna para cada tipo de arquivo (atalhos, documentos, imagens...), a partir da esquerda.";

    /** Nome do parâmetro do layout, usado na mensagem de valor nulo. */
    private static final String LAYOUT_NAME = "layout";

    /**
     * Impede a criação de instâncias: a classe só tem métodos estáticos.
     */
    private DesktopLayoutText() {
    }

    /**
     * Devolve o nome curto do layout.
     *
     * @param layout o layout
     * @return o nome em português
     * @throws NullPointerException se {@code layout} for nulo
     */
    public static String label(DesktopLayout layout) {
        return switch (Objects.requireNonNull(layout, LAYOUT_NAME)) {
            case LEFT -> LEFT_LABEL;
            case RIGHT -> RIGHT_LABEL;
            case TOP -> TOP_LABEL;
            case BOTTOM -> BOTTOM_LABEL;
            case BORDERS -> BORDERS_LABEL;
            case BY_TYPE_LEFT -> BY_TYPE_LABEL;
        };
    }

    /**
     * Devolve a descrição do layout, numa frase.
     *
     * @param layout o layout
     * @return a descrição em português
     * @throws NullPointerException se {@code layout} for nulo
     */
    public static String description(DesktopLayout layout) {
        return switch (Objects.requireNonNull(layout, LAYOUT_NAME)) {
            case LEFT -> LEFT_DESCRIPTION;
            case RIGHT -> RIGHT_DESCRIPTION;
            case TOP -> TOP_DESCRIPTION;
            case BOTTOM -> BOTTOM_DESCRIPTION;
            case BORDERS -> BORDERS_DESCRIPTION;
            case BY_TYPE_LEFT -> BY_TYPE_DESCRIPTION;
        };
    }
}
