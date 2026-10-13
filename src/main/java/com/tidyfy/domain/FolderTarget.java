package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Alvo de limpeza que é uma pasta.
 *
 * @param folder       pasta a limpar
 * @param ageReference data do arquivo usada para medir a idade dele
 */
public record FolderTarget(Path folder, AgeReference ageReference) implements CleanupTarget {

    // Constantes

    /** Nome do parâmetro da pasta, usado na mensagem de valor nulo. */
    private static final String FOLDER_NAME = "folder";

    /** Nome do parâmetro da data de referência, usado na mensagem de valor nulo. */
    private static final String AGE_REFERENCE_NAME = "ageReference";

    /**
     * Valida os dados do alvo.
     *
     * @throws NullPointerException se {@code folder} ou {@code ageReference} for nulo
     */
    public FolderTarget {
        Objects.requireNonNull(folder, FOLDER_NAME);
        Objects.requireNonNull(ageReference, AGE_REFERENCE_NAME);
    }
}
