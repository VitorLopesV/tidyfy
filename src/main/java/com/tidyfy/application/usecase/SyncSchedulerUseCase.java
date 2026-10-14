package com.tidyfy.application.usecase;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Sincroniza o Agendador de Tarefas com os ajudantes salvos (a configuração é a fonte da verdade).
 * É idempotente: cada ajudante ligado é registrado de novo (o agendador substitui a tarefa, o que refaz as
 * ausentes e aponta as demais para o executável atual) e cada desligado é removido (tarefa inexistente é
 * ignorada). Tarefas órfãs, de ajudantes que não existem mais, também são removidas.
 * Uma falha em um item é acumulada no relatório e não impede os demais.
 */
public final class SyncSchedulerUseCase {

    /** Repositório com os ajudantes salvos. */
    private final HelperRepository repository;

    /** Agendador a sincronizar. */
    private final TaskScheduler scheduler;

    // Constantes

    /** Nome do parâmetro do repositório, usado na mensagem de valor nulo. */
    private static final String REPOSITORY_NAME = "repository";

    /** Nome do parâmetro do agendador, usado na mensagem de valor nulo. */
    private static final String SCHEDULER_NAME = "scheduler";

    /**
     * Cria o caso de uso.
     *
     * @param repository repositório dos ajudantes
     * @param scheduler  agendador de tarefas
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public SyncSchedulerUseCase(HelperRepository repository, TaskScheduler scheduler) {
        this.repository = Objects.requireNonNull(repository, REPOSITORY_NAME);
        this.scheduler = Objects.requireNonNull(scheduler, SCHEDULER_NAME);
    }

    /**
     * Sincroniza o agendador. Só lança exceção se os ajudantes não puderem ser lidos; falhas do agendador
     * vão para o relatório.
     *
     * @return relatório com as contagens e as falhas
     * @throws java.io.UncheckedIOException se a leitura do repositório falhar
     */
    public SyncResult execute() {
        List<Helper> helpers = repository.findAll();
        List<SyncResult.Failure> failures = new ArrayList<>();
        int registered = 0;
        int removed = 0;
        Set<HelperId> known = new HashSet<>();
        for (Helper helper : helpers) {
            known.add(helper.id());
            try {
                if (helper.enabled()) {
                    scheduler.register(helper);
                    registered++;
                } else {
                    scheduler.unregister(helper.id());
                    removed++;
                }
            } catch (RuntimeException e) {
                failures.add(new SyncResult.Failure(helper.id(), e));
            }
        }
        int orphansRemoved = removeOrphans(known, failures);
        return new SyncResult(registered, removed, orphansRemoved, failures);
    }

    /**
     * Remove as tarefas cujo id não pertence a nenhum ajudante salvo.
     *
     * @return quantidade de órfãs removidas
     */
    private int removeOrphans(Set<HelperId> known, List<SyncResult.Failure> failures) {
        List<HelperId> taskIds;
        try {
            taskIds = scheduler.registeredIds();
        } catch (RuntimeException e) {
            failures.add(new SyncResult.Failure(null, e));
            return 0;
        }
        int removed = 0;
        for (HelperId id : taskIds) {
            if (known.contains(id)) {
                continue;
            }
            try {
                scheduler.unregister(id);
                removed++;
            } catch (RuntimeException e) {
                failures.add(new SyncResult.Failure(id, e));
            }
        }
        return removed;
    }
}
