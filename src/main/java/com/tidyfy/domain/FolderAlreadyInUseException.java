package com.tidyfy.domain;

import java.nio.file.Path;

/**
 * Lançada ao tentar salvar um ajudante que atua numa pasta (ou na lixeira) já usada
 * por outro ajudante do mesmo tipo.
 */
public class FolderAlreadyInUseException extends DomainException {

    // Constantes

    /** Modelo da mensagem de erro: pasta igual, contida ou que contém a de outro ajudante. */
    private static final String FOLDER_MESSAGE =
            "A pasta %s já está em uso pelo ajudante \"%s\" (mesma pasta ou pasta aninhada).";

    /** Modelo da mensagem de erro: a lixeira já tem um ajudante de limpeza. */
    private static final String RECYCLE_BIN_MESSAGE = "A lixeira já está em uso pelo ajudante \"%s\".";

    /**
     * Cria a exceção com a mensagem pronta.
     *
     * @param message a mensagem de erro
     */
    private FolderAlreadyInUseException(String message) {
        super(message);
    }

    /**
     * Cria a exceção para uma pasta que se sobrepõe à de outro ajudante.
     *
     * @param folder a pasta que o ajudante quer usar
     * @param owner  o nome do ajudante que já usa a pasta (ou uma pasta aninhada)
     * @return a exceção
     */
    public static FolderAlreadyInUseException forFolder(Path folder, HelperName owner) {
        return new FolderAlreadyInUseException(String.format(FOLDER_MESSAGE, folder, owner.value()));
    }

    /**
     * Cria a exceção para a lixeira, que já tem um ajudante de limpeza.
     *
     * @param owner o nome do ajudante que já limpa a lixeira
     * @return a exceção
     */
    public static FolderAlreadyInUseException forRecycleBin(HelperName owner) {
        return new FolderAlreadyInUseException(String.format(RECYCLE_BIN_MESSAGE, owner.value()));
    }
}
