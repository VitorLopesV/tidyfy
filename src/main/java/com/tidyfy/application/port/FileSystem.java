package com.tidyfy.application.port;

import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import java.nio.file.Path;
import java.util.List;

/**
 * Porta de acesso ao sistema de arquivos.
 * Falhas de leitura, escrita ou permissão são lançadas como {@link java.io.UncheckedIOException}.
 */
public interface FileSystem {

    /**
     * Lista o que está diretamente na pasta (arquivos e pastas), sem entrar nas subpastas.
     *
     * @param folder pasta a listar
     * @return entradas da pasta; vazia se ela estiver vazia
     */
    List<FolderEntry> listEntries(Path folder);

    /**
     * Informa se o caminho existe (arquivo ou pasta).
     *
     * @param path caminho a verificar
     * @return {@code true} se existir
     */
    boolean exists(Path path);

    /**
     * Compara o conteúdo de dois arquivos.
     *
     * @param first primeiro arquivo
     * @param second segundo arquivo
     * @return {@code true} se os conteúdos forem idênticos
     */
    boolean sameContent(Path first, Path second);

    /**
     * Move o arquivo ou a pasta da origem para o destino, criando a pasta de destino se preciso.
     * Não sobrescreve: o chamador garante que o destino está livre.
     *
     * @param move origem e destino
     */
    void move(FileMove move);

    /**
     * Envia o arquivo ou a pasta para a lixeira do sistema.
     *
     * @param path caminho a enviar
     */
    void moveToTrash(Path path);
}
