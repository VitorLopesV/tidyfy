package com.tidyfy.domain;

/**
 * Lançada ao tentar criar um ajudante de um tipo que já atingiu o limite máximo.
 */
public class HelperLimitReachedException extends DomainException {

    // Constantes

    /** Modelo da mensagem de erro: limite por tipo e tipo do ajudante. */
    private static final String MESSAGE = "O limite de %d ajudantes do tipo %s foi atingido.";

    /**
     * Cria a exceção para o tipo que atingiu o limite.
     *
     * @param type o tipo de ajudante que atingiu o limite
     */
    public HelperLimitReachedException(HelperType type) {
        super(String.format(MESSAGE, HelperType.MAX_PER_TYPE, type));
    }
}
