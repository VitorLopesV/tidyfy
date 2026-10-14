package com.tidyfy.infrastructure.persistence;

/**
 * Erro ao converter ajudantes de ou para JSON.
 * <p>
 * Indica JSON inválido, campo obrigatório ausente, tipo ou versão desconhecidos
 * ou valor recusado pelo domínio.
 */
public class HelperJsonException extends RuntimeException {

    /**
     * Cria a exceção com a mensagem informada.
     *
     * @param message descrição do problema
     */
    public HelperJsonException(String message) {
        super(message);
    }

    /**
     * Cria a exceção com a mensagem e a causa informadas.
     *
     * @param message descrição do problema
     * @param cause   causa original
     */
    public HelperJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
