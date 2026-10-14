package com.tidyfy;

import com.tidyfy.application.usecase.SyncResult;
import com.tidyfy.application.usecase.SyncSchedulerUseCase;
import com.tidyfy.infrastructure.logging.ExecutionLog;

import java.util.function.Supplier;

/**
 * Dispara a sincronização do agendador em segundo plano ao abrir a interface.
 * Roda em uma thread daemon: nunca atrasa nem impede a abertura da interface, e qualquer falha (inclusive ao
 * montar o caso de uso) é só registrada no log.
 */
final class SchedulerSyncStarter {

    /** Fornece o caso de uso; chamado já na thread de segundo plano. */
    private final Supplier<SyncSchedulerUseCase> syncScheduler;

    /** Log onde as falhas são registradas. */
    private final ExecutionLog log;

    // Constantes

    /** Nome da thread de segundo plano. */
    private static final String THREAD_NAME = "tidyfy-scheduler-sync";

    /** Descrição registrada quando a sincronização inteira falha. */
    private static final String SYNC_FAILED_LOG = "Falha ao sincronizar o agendador";

    /** Descrição registrada quando um item da sincronização falha. */
    private static final String ITEM_FAILED_LOG = "Falha ao sincronizar a tarefa %s do agendador";

    /** Texto usado no lugar do id quando a falha não é de uma tarefa específica. */
    private static final String NO_TASK = "(listagem)";

    /**
     * Cria o disparador.
     *
     * @param syncScheduler fornecedor do caso de uso (adiado para a thread de segundo plano)
     * @param log           log das falhas
     */
    SchedulerSyncStarter(Supplier<SyncSchedulerUseCase> syncScheduler, ExecutionLog log) {
        this.syncScheduler = syncScheduler;
        this.log = log;
    }

    /**
     * Inicia a sincronização em uma thread daemon e retorna na hora.
     *
     * @return a thread iniciada (útil para esperar o término em testes)
     */
    Thread start() {
        Thread thread = new Thread(this::run, THREAD_NAME);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Executa a sincronização e registra as falhas, sem deixar nenhuma exceção escapar.
     */
    void run() {
        try {
            SyncResult result = syncScheduler.get().execute();
            for (SyncResult.Failure failure : result.failures()) {
                String task = failure.id() == null ? NO_TASK : failure.id().value().toString();
                log.error(String.format(ITEM_FAILED_LOG, task), failure.error());
            }
        } catch (Exception | LinkageError e) {
            try {
                log.error(SYNC_FAILED_LOG, e);
            } catch (RuntimeException ignored) {
                // Nem o log funciona: a interface não pode ser afetada.
            }
        }
    }
}
