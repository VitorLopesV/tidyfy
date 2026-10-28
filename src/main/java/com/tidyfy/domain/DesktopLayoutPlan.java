package com.tidyfy.domain;

import java.util.List;
import java.util.Objects;

/**
 * Resultado do {@link DesktopLayoutPlanner}: onde cada ícone deve ficar e quais não couberam na área visível.
 *
 * @param placements ícones com a nova posição, na ordem em que o planejador os posicionou
 * @param leftOut    ícones que não couberam; continuam com a posição que tinham e não entram em
 *                   {@code placements}
 */
public record DesktopLayoutPlan(List<DesktopIcon> placements, List<DesktopIcon> leftOut) {

    // Constantes

    /** Nome do parâmetro das posições, usado na mensagem de valor nulo. */
    private static final String PLACEMENTS_NAME = "placements";

    /** Nome do parâmetro dos ícones que ficaram de fora, usado na mensagem de valor nulo. */
    private static final String LEFT_OUT_NAME = "leftOut";

    /**
     * Valida os dados e guarda cópias imutáveis das listas.
     *
     * @throws NullPointerException se {@code placements} ou {@code leftOut} for nulo
     */
    public DesktopLayoutPlan {
        Objects.requireNonNull(placements, PLACEMENTS_NAME);
        Objects.requireNonNull(leftOut, LEFT_OUT_NAME);
        placements = List.copyOf(placements);
        leftOut = List.copyOf(leftOut);
    }
}
