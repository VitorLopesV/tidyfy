package com.tidyfy.domain;

import java.util.UUID;

/**
 * Identificador único de um ajudante.
 *
 * @param value o UUID que identifica o ajudante
 */
public record HelperId(UUID value) {

    // Constantes

    /** Mensagem de erro quando o UUID não é informado. */
    private static final String VALUE_REQUIRED = "O identificador do ajudante é obrigatório.";

    /**
     * Cria um identificador a partir de um UUID existente.
     *
     * @throws IllegalArgumentException se o UUID for nulo
     */
    public HelperId {
        if (value == null) {
            throw new IllegalArgumentException(VALUE_REQUIRED);
        }
    }

    /**
     * Gera um novo identificador, diferente de todos os anteriores.
     *
     * @return um identificador novo
     */
    public static HelperId newId() {
        return new HelperId(UUID.randomUUID());
    }
}
