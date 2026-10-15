package com.tidyfy;

import com.tidyfy.application.usecase.RemoveAllTasksUseCase;
import com.tidyfy.application.usecase.RemoveTasksResult;
import com.tidyfy.infrastructure.logging.ExecutionLog;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Modo {@code --remove-tasks} do Tidyfy, chamado pelo desinstalador: remove todas as tarefas do Tidyfy do
 * Agendador de Tarefas do Windows, registra no log e devolve o código de saída; quem chama decide encerrar
 * o processo. Não referencia JavaFX: o desinstalador nunca abre janela.
 */
public final class RemoveTasksRunner {

    /** Código de saída de sucesso (inclui o caso de não haver nenhuma tarefa). */
    public static final int EXIT_OK = 0;

    /** Código de saída quando alguma remoção falhou. */
    public static final int EXIT_FAILURE = 1;

    /** Fornece o caso de uso; só é chamado quando a remoção é executada. */
    private final Supplier<RemoveAllTasksUseCase> removeAllTasks;

    /** Log das execuções. */
    private final ExecutionLog log;

    /** Saída de erro onde as mensagens para o usuário são escritas. */
    private final PrintStream err;

    // Constantes

    /** Mensagem de início no log. */
    private static final String STARTED_LOG = "Remoção das tarefas do agendador iniciada";

    /** Modelo da mensagem de conclusão no log: quantidade removida e quantidade de falhas. */
    private static final String FINISHED_LOG = "Remoção das tarefas concluída: %d tarefa(s) removida(s), %d falha(s)";

    /** Modelo da descrição de uma falha ao remover uma tarefa. */
    private static final String TASK_FAILURE_LOG = "Falha ao remover a tarefa %s";

    /** Descrição usada quando a falha foi ao listar as tarefas. */
    private static final String LISTING_FAILURE_LOG = "Falha ao listar as tarefas";

    /** Descrição de uma falha inesperada. */
    private static final String UNEXPECTED_FAILURE_LOG = "Falha inesperada ao remover as tarefas";

    /** Modelo da mensagem de falha para o usuário. */
    private static final String FAILURE_MESSAGE = "Tidyfy: %d falha(s) ao remover as tarefas do agendador";

    /**
     * Cria o executor.
     *
     * @param removeAllTasks fornecedor do caso de uso
     * @param log            log das execuções
     * @param err            saída de erro para as mensagens ao usuário
     */
    public RemoveTasksRunner(Supplier<RemoveAllTasksUseCase> removeAllTasks, ExecutionLog log, PrintStream err) {
        this.removeAllTasks = removeAllTasks;
        this.log = log;
        this.err = err;
    }

    /**
     * Remove todas as tarefas e registra o resultado no log.
     *
     * @return {@link #EXIT_OK} se não houve falha, ou {@link #EXIT_FAILURE}
     */
    public int run() {
        log.info(STARTED_LOG);
        try {
            RemoveTasksResult result = removeAllTasks.get().execute();
            log.info(String.format(FINISHED_LOG, result.removed(), result.failures().size()));
            for (RemoveTasksResult.Failure failure : result.failures()) {
                String description = failure.id() == null
                        ? LISTING_FAILURE_LOG : String.format(TASK_FAILURE_LOG, failure.id().value());
                log.error(description, failure.error());
            }
            if (result.succeeded()) {
                return EXIT_OK;
            }
            err.println(String.format(FAILURE_MESSAGE, result.failures().size()));
            return EXIT_FAILURE;
        } catch (Exception | LinkageError e) {
            log.error(UNEXPECTED_FAILURE_LOG, e);
            err.println(String.format(FAILURE_MESSAGE, 1));
            return EXIT_FAILURE;
        }
    }
}
