package com.tidyfy.infrastructure.persistence;

/**
 * Erro ao converter o histórico de execuções de ou para JSON.
 * <p>
 * Indica JSON inválido, campo obrigatório ausente, tipo de ação ou versão desconhecidos
 * ou valor recusado pelo domínio.
 */
public class HistoryJsonException extends RuntimeException {

    /**
     * Cria a exceção com a mensagem informada.
     *
     * @param message descrição do problema
     */
    public HistoryJsonException(String message) {
        super(message);
    }

    /**
     * Cria a exceção com a mensagem e a causa informadas.
     *
     * @param message descrição do problema
     * @param cause   causa original
     */
    public HistoryJsonException(String message, Throwable cause) {
        super(message, cause);
    }
}
