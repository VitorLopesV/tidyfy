package com.tidyfy.application.usecase;

import com.tidyfy.domain.HelperId;
import java.util.List;

/**
 * Relatório da remoção de todas as tarefas do Tidyfy do agendador.
 *
 * @param removed  quantidade de tarefas removidas
 * @param failures falhas ocorridas; uma falha não impede as demais remoções
 */
public record RemoveTasksResult(int removed, List<Failure> failures) {

    /**
     * Copia a lista de falhas, tornando-a imutável.
     */
    public RemoveTasksResult {
        failures = List.copyOf(failures);
    }

    /**
     * Informa se a remoção terminou sem nenhuma falha.
     *
     * @return {@code true} se não houve falhas
     */
    public boolean succeeded() {
        return failures.isEmpty();
    }

    /**
     * Falha em uma remoção.
     *
     * @param id    ajudante da tarefa que não foi removida; {@code null} quando a falha foi ao listar as tarefas
     * @param error causa da falha
     */
    public record Failure(HelperId id, RuntimeException error) {
    }
}
