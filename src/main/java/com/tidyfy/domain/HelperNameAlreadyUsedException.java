package com.tidyfy.domain;

/**
 * Lançada ao tentar salvar um ajudante com o nome de outro ajudante.
 */
public class HelperNameAlreadyUsedException extends DomainException {

    // Constantes

    /** Modelo da mensagem de erro: nome repetido. */
    private static final String MESSAGE = "Já existe um ajudante chamado \"%s\".";

    /**
     * Cria a exceção para o nome repetido.
     *
     * @param name o nome que já está em uso
     */
    public HelperNameAlreadyUsedException(HelperName name) {
        super(String.format(MESSAGE, name.value()));
    }
}
