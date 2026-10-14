package com.tidyfy.application.port;

import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.time.Instant;
import java.util.List;

/**
 * Porta de persistência do histórico de execuções dos ajudantes.
 * Falhas de leitura ou gravação são lançadas como {@link java.io.UncheckedIOException}.
 */
public interface ExecutionHistoryRepository {

    /**
     * Registra uma execução no histórico.
     *
     * @param record execução a registrar
     */
    void add(ExecutionRecord record);

    /**
     * Lista as execuções de um ajudante, da mais recente para a mais antiga.
     *
     * @param id identificador do ajudante
     * @return execuções do ajudante; vazia se não houver nenhuma (ou se o id não existir)
     */
    List<ExecutionRecord> findByHelper(HelperId id);

    /**
     * Apaga os registros vencidos, isto é, os que {@link ExecutionRecord#isExpired(Instant)}
     * considera expirados em relação ao instante informado. Sem registros vencidos, nada acontece.
     *
     * @param now instante de referência
     */
    void deleteExpired(Instant now);
}
