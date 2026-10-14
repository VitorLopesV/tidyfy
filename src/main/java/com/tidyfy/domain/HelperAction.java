package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Uma ação registrada em uma execução de ajudante.
 *
 * @param type    tipo da ação (obrigatório)
 * @param source  arquivo de origem; pode ser {@code null} (ex.: lixeira esvaziada)
 * @param target  destino do arquivo; pode ser {@code null} (ex.: arquivo enviado para a lixeira)
 * @param message texto complementar, usado principalmente em falhas; pode ser {@code null}
 */
public record HelperAction(ActionType type, Path source, Path target, String message) {

    // Constantes

    /** Nome do parâmetro do tipo, usado na mensagem de valor nulo. */
    private static final String TYPE_NAME = "type";

    /**
     * Valida os dados da ação.
     *
     * @throws NullPointerException se {@code type} for nulo
     */
    public HelperAction {
        Objects.requireNonNull(type, TYPE_NAME);
    }
}
