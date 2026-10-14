package com.tidyfy.infrastructure.persistence;

import java.nio.file.Path;

/**
 * Erro de arquivo de configurações corrompido ou de versão desconhecida.
 * <p>
 * O arquivo original não é alterado; uma cópia de segurança é guardada ao lado dele.
 * A interface pode mostrar a mensagem ao usuário.
 */
public class CorruptedStorageException extends RuntimeException {

    // Constantes

    /** Mensagem com o caminho do arquivo e o motivo. */
    private static final String MESSAGE =
            "O arquivo de configurações %s está corrompido ou tem versão desconhecida e não foi alterado. Motivo: %s";

    /**
     * Cria a exceção.
     *
     * @param file  arquivo problemático
     * @param cause erro de conversão original
     */
    public CorruptedStorageException(Path file, HelperJsonException cause) {
        super(String.format(MESSAGE, file, cause.getMessage()), cause);
    }
}
