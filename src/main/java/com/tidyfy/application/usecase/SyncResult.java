package com.tidyfy.application.usecase;

import com.tidyfy.domain.HelperId;
import java.util.List;

/**
 * Relatório da sincronização do agendador.
 *
 * @param registered      quantidade de tarefas de ajudantes ligados (re)registradas
 * @param removed         quantidade de tarefas de ajudantes desligados removidas
 * @param orphansRemoved  quantidade de tarefas órfãs (de ajudantes que não existem mais) removidas
 * @param failures        falhas ocorridas; uma falha não impede os demais itens
 */
public record SyncResult(int registered, int removed, int orphansRemoved, List<Failure> failures) {

    /**
     * Copia a lista de falhas, tornando-a imutável.
     */
    public SyncResult {
        failures = List.copyOf(failures);
    }

    /**
     * Informa se a sincronização terminou sem nenhuma falha.
     *
     * @return {@code true} se não houve falhas
     */
    public boolean succeeded() {
        return failures.isEmpty();
    }

    /**
     * Falha em um item da sincronização.
     *
     * @param id    ajudante (ou tarefa órfã) afetado; {@code null} quando a falha foi ao listar as tarefas
     * @param error causa da falha
     */
    public record Failure(HelperId id, RuntimeException error) {
    }
}
