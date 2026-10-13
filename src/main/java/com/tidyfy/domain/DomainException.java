package com.tidyfy.domain;

/**
 * Base de todas as exceções que representam a violação de uma regra de negócio.
 */
public class DomainException extends RuntimeException {

    /**
     * Cria a exceção com a mensagem informada.
     *
     * @param message a descrição da regra violada
     */
    public DomainException(String message) {
        super(message);
    }
}
