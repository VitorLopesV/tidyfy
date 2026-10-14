package com.tidyfy.application.port;

import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;

/**
 * Porta do agendador de tarefas do sistema operacional.
 * Existe uma única tarefa por ajudante. Falhas do agendador são lançadas como
 * {@link java.io.UncheckedIOException}.
 */
public interface TaskScheduler {

    /**
     * Registra a tarefa do ajudante com a frequência dele. Se a tarefa já existir, é atualizada.
     *
     * @param helper ajudante a agendar
     */
    void register(Helper helper);

    /**
     * Remove a tarefa do ajudante. Id sem tarefa registrada não é erro: nada acontece.
     *
     * @param id identificador do ajudante
     */
    void unregister(HelperId id);
}
