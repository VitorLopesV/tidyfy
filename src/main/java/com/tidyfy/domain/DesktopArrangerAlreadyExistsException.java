package com.tidyfy.domain;

/**
 * Lançada ao tentar criar um segundo Organizador da área de trabalho. A área de trabalho é única,
 * então só pode existir um ajudante deste tipo.
 */
public class DesktopArrangerAlreadyExistsException extends DomainException {

    // Constantes

    /** Mensagem de erro: já existe um Organizador da área de trabalho. */
    private static final String MESSAGE =
            "Já existe um Organizador da área de trabalho. Só é possível ter um, porque a área de trabalho é única.";

    /**
     * Cria a exceção.
     */
    public DesktopArrangerAlreadyExistsException() {
        super(MESSAGE);
    }
}
