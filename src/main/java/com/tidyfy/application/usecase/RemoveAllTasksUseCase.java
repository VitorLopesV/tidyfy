package com.tidyfy.application.usecase;

import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.HelperId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Remove do agendador todas as tarefas do Tidyfy, sem olhar a configuração dos ajudantes. É usado na
 * desinstalação, para não deixar tarefas apontando para um executável que deixou de existir.
 * Uma falha ao remover uma tarefa é acumulada no relatório e não impede as demais; se a listagem das
 * tarefas falhar, a falha também vai para o relatório (com id nulo) e nada é removido.
 */
public final class RemoveAllTasksUseCase {

    /** Agendador de onde as tarefas são removidas. */
    private final TaskScheduler scheduler;

    // Constantes

    /** Nome do parâmetro do agendador, usado na mensagem de valor nulo. */
    private static final String SCHEDULER_NAME = "scheduler";

    /**
     * Cria o caso de uso.
     *
     * @param scheduler agendador de tarefas
     * @throws NullPointerException se o agendador for nulo
     */
    public RemoveAllTasksUseCase(TaskScheduler scheduler) {
        this.scheduler = Objects.requireNonNull(scheduler, SCHEDULER_NAME);
    }

    /**
     * Remove todas as tarefas do Tidyfy registradas no agendador. Nunca lança exceção do agendador: as
     * falhas vão para o relatório.
     *
     * @return relatório com a quantidade removida e as falhas
     */
    public RemoveTasksResult execute() {
        List<RemoveTasksResult.Failure> failures = new ArrayList<>();
        List<HelperId> ids;
        try {
            ids = scheduler.registeredIds();
        } catch (RuntimeException e) {
            failures.add(new RemoveTasksResult.Failure(null, e));
            return new RemoveTasksResult(0, failures);
        }
        int removed = 0;
        for (HelperId id : ids) {
            try {
                scheduler.unregister(id);
                removed++;
            } catch (RuntimeException e) {
                failures.add(new RemoveTasksResult.Failure(id, e));
            }
        }
        return new RemoveTasksResult(removed, failures);
    }
}
