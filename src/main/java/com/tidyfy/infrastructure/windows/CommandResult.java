package com.tidyfy.infrastructure.windows;

import java.util.Objects;

/**
 * Resultado da execução de um processo externo.
 *
 * @param exitCode código de saída do processo (0 indica sucesso)
 * @param output   saída de texto do processo, padrão e erro juntos; nunca nula
 */
public record CommandResult(int exitCode, String output) {

    // Constantes

    /** Código de saída que indica sucesso. */
    private static final int SUCCESS_EXIT_CODE = 0;

    /** Mensagem quando a saída é nula. */
    private static final String NULL_OUTPUT_MESSAGE = "output must not be null";

    /**
     * Valida que a saída não é nula.
     */
    public CommandResult {
        Objects.requireNonNull(output, NULL_OUTPUT_MESSAGE);
    }

    /**
     * Indica se o processo terminou com sucesso.
     *
     * @return {@code true} se o código de saída é 0
     */
    public boolean succeeded() {
        return exitCode == SUCCESS_EXIT_CODE;
    }
}
