package com.tidyfy.application.usecase;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.HelperId;
import java.util.Collection;
import java.util.Objects;

/**
 * Remove um ou vários ajudantes: tira a tarefa de cada um do agendador e apaga a configuração do
 * repositório. O histórico de execuções não é tocado: fica guardado até expirar.
 */
public final class RemoveHelperUseCase {

    /** Repositório de onde a configuração dos ajudantes é apagada. */
    private final HelperRepository repository;

    /** Agendador de onde as tarefas dos ajudantes são removidas. */
    private final TaskScheduler scheduler;

    // Constantes

    /** Nome do parâmetro do repositório, usado na mensagem de valor nulo. */
    private static final String REPOSITORY_NAME = "repository";

    /** Nome do parâmetro do agendador, usado na mensagem de valor nulo. */
    private static final String SCHEDULER_NAME = "scheduler";

    /** Nome do parâmetro dos ids, usado na mensagem de valor nulo. */
    private static final String IDS_NAME = "ids";

    /**
     * Cria o caso de uso.
     *
     * @param repository repositório dos ajudantes
     * @param scheduler agendador de tarefas
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public RemoveHelperUseCase(HelperRepository repository, TaskScheduler scheduler) {
        this.repository = Objects.requireNonNull(repository, REPOSITORY_NAME);
        this.scheduler = Objects.requireNonNull(scheduler, SCHEDULER_NAME);
    }

    /**
     * Remove os ajudantes informados, um de cada vez, na ordem recebida. Para cada um, remove
     * primeiro a tarefa do agendador e só então apaga do repositório; assim, se o agendador falhar,
     * o ajudante continua salvo. A falha é propagada e os ajudantes seguintes não são processados
     * (os anteriores já foram removidos). Ids inexistentes não são erro.
     *
     * @param ids identificadores dos ajudantes a remover
     * @throws NullPointerException se {@code ids} for nulo
     * @throws java.io.UncheckedIOException se o agendador ou o repositório falhar
     */
    public void execute(Collection<HelperId> ids) {
        Objects.requireNonNull(ids, IDS_NAME);
        for (HelperId id : ids) {
            scheduler.unregister(id);
            repository.delete(id);
        }
    }
}
