package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Movimentação de um arquivo: de onde ele sai e para onde vai.
 * Origem e destino iguais são aceitos; decidir se isso vale a pena é papel do planejador.
 *
 * @param source caminho atual do arquivo
 * @param target caminho de destino do arquivo
 */
public record FileMove(Path source, Path target) {

    // Constantes

    /** Nome do parâmetro da origem, usado na mensagem de valor nulo. */
    private static final String SOURCE_NAME = "source";

    /** Nome do parâmetro do destino, usado na mensagem de valor nulo. */
    private static final String TARGET_NAME = "target";

    /**
     * Valida os dados da movimentação.
     *
     * @throws NullPointerException se {@code source} ou {@code target} for nulo
     */
    public FileMove {
        Objects.requireNonNull(source, SOURCE_NAME);
        Objects.requireNonNull(target, TARGET_NAME);
    }
}
