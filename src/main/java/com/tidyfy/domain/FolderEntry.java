package com.tidyfy.domain;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Item encontrado dentro de uma pasta: um arquivo ou uma subpasta, com as datas de criação e de modificação.
 *
 * @param path       caminho do item
 * @param directory  {@code true} se o item é uma pasta, {@code false} se é um arquivo
 * @param createdAt  momento em que o item foi criado
 * @param modifiedAt momento da última modificação do item
 */
public record FolderEntry(Path path, boolean directory, Instant createdAt, Instant modifiedAt) {

    // Constantes

    /** Nome do parâmetro do caminho, usado na mensagem de valor nulo. */
    private static final String PATH_NAME = "path";

    /** Nome do parâmetro da data de criação, usado na mensagem de valor nulo. */
    private static final String CREATED_AT_NAME = "createdAt";

    /** Nome do parâmetro da data de modificação, usado na mensagem de valor nulo. */
    private static final String MODIFIED_AT_NAME = "modifiedAt";

    /**
     * Valida os dados do item.
     *
     * @throws NullPointerException se {@code path}, {@code createdAt} ou {@code modifiedAt} for nulo
     */
    public FolderEntry {
        Objects.requireNonNull(path, PATH_NAME);
        Objects.requireNonNull(createdAt, CREATED_AT_NAME);
        Objects.requireNonNull(modifiedAt, MODIFIED_AT_NAME);
    }
}
