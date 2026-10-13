package com.tidyfy.domain;

/**
 * Nome de um ajudante, definido pelo usuário. É obrigatório e guardado sem espaços nas pontas.
 *
 * @param value o nome já aparado
 */
public record HelperName(String value) {

    // Constantes

    /** Mensagem de erro quando o nome é nulo, vazio ou só com espaços. */
    private static final String NAME_REQUIRED = "O nome do ajudante é obrigatório.";

    /**
     * Cria o nome aparando os espaços das pontas.
     *
     * @throws IllegalArgumentException se o nome for nulo, vazio ou só com espaços
     */
    public HelperName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        value = value.strip();
    }
}
