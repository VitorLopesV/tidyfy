package com.tidyfy.domain;

import java.nio.file.Path;

/**
 * Lançada ao tentar usar uma pasta sensível do sistema como alvo de um ajudante.
 */
public class ProtectedFolderException extends DomainException {

    // Constantes

    /** Modelo da mensagem de erro: pasta recusada. */
    private static final String MESSAGE = "A pasta %s é protegida e não pode ser usada por um ajudante.";

    /**
     * Cria a exceção para a pasta recusada.
     *
     * @param folder a pasta sensível que foi informada
     */
    public ProtectedFolderException(Path folder) {
        super(String.format(MESSAGE, folder));
    }
}
