package com.tidyfy.application.port;

import java.time.Instant;

/**
 * Porta da lixeira do sistema. Esvaziar remove os arquivos de forma permanente.
 * Falhas são lançadas como {@link java.io.UncheckedIOException}.
 */
public interface RecycleBin {

    /**
     * Esvazia a lixeira por completo.
     *
     * @return quantidade de itens removidos (zero se já estava vazia)
     */
    int emptyAll();

    /**
     * Remove da lixeira só o que foi para lá antes do instante de corte.
     *
     * @param cutoff instante de corte: itens enviados antes dele são removidos
     * @return quantidade de itens removidos (zero se nenhum for mais antigo)
     */
    int emptyOlderThan(Instant cutoff);
}
