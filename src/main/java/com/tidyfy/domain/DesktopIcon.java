package com.tidyfy.domain;

import java.util.Objects;

/**
 * Ícone da área de trabalho: o nome que o Windows exibe e a posição dele.
 *
 * @param name     nome exibido do ícone (não vazio)
 * @param position posição do ícone
 */
public record DesktopIcon(String name, IconPosition position) {

    // Constantes

    /** Nome do parâmetro do nome, usado na mensagem de valor nulo. */
    private static final String NAME_NAME = "name";

    /** Nome do parâmetro da posição, usado na mensagem de valor nulo. */
    private static final String POSITION_NAME = "position";

    /** Mensagem quando o nome é vazio. */
    private static final String BLANK_NAME_MESSAGE = "name must not be blank";

    /**
     * Valida os dados do ícone.
     *
     * @throws NullPointerException     se {@code name} ou {@code position} for nulo
     * @throws IllegalArgumentException se {@code name} for vazio
     */
    public DesktopIcon {
        Objects.requireNonNull(name, NAME_NAME);
        Objects.requireNonNull(position, POSITION_NAME);
        if (name.isBlank()) {
            throw new IllegalArgumentException(BLANK_NAME_MESSAGE);
        }
    }
}
