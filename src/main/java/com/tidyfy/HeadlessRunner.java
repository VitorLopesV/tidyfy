package com.tidyfy;

import com.tidyfy.application.usecase.RunHelperUseCase;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import com.tidyfy.infrastructure.logging.ExecutionLog;

import java.io.PrintStream;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Modo sem interface do Tidyfy ({@code --run <id>}), chamado pelo Agendador de Tarefas do Windows.
 * Executa o ajudante, registra no log e devolve o código de saída; quem chama decide encerrar o processo.
 * Uma execução que terminou com ação de falha no histórico também devolve código de falha.
 * Não referencia JavaFX: o processo agendado nunca carrega a interface.
 */
public final class HeadlessRunner {

    /** Código de saída de sucesso (inclui ajudante inexistente ou desligado, que não é erro). */
    public static final int EXIT_OK = 0;

    /** Código de saída quando a execução do ajudante falhou. */
    public static final int EXIT_FAILURE = 1;

    /** Código de saída quando os argumentos são inválidos. */
    public static final int EXIT_INVALID_ARGUMENTS = 2;

    /** Fornece o caso de uso de execução; só é chamado quando há um ajudante a executar. */
    private final Supplier<RunHelperUseCase> runHelper;

    /** Log das execuções. */
    private final ExecutionLog log;

    /** Saída de erro onde as mensagens para o usuário são escritas. */
    private final PrintStream err;

    // Constantes

    /** Descrição registrada no log quando os argumentos são inválidos. */
    private static final String INVALID_ARGUMENTS_LOG = "Argumentos inválidos";

    /** Modelo da mensagem de erro de argumentos para o usuário. */
    private static final String INVALID_ARGUMENTS_MESSAGE = "Tidyfy: %s";

    /** Modelo da mensagem de falha para o usuário. */
    private static final String FAILURE_MESSAGE = "Tidyfy: a execução do ajudante %s falhou: %s";

    /**
     * Cria o executor.
     *
     * @param runHelper fornecedor do caso de uso (adiado para não montar nada com argumentos inválidos)
     * @param log       log das execuções
     * @param err       saída de erro para as mensagens ao usuário
     */
    public HeadlessRunner(Supplier<RunHelperUseCase> runHelper, ExecutionLog log, PrintStream err) {
        this.runHelper = runHelper;
        this.log = log;
        this.err = err;
    }

    /**
     * Executa o que os argumentos pedem. Só o modo {@link LaunchArguments.Mode#RUN} executa algo; qualquer
     * outro (argumentos inválidos) é registrado no log e escrito no erro.
     *
     * @param arguments argumentos já interpretados
     * @return {@link #EXIT_OK}, {@link #EXIT_FAILURE} ou {@link #EXIT_INVALID_ARGUMENTS}
     */
    public int run(LaunchArguments arguments) {
        if (arguments.mode() != LaunchArguments.Mode.RUN) {
            String message = arguments.error();
            log.error(INVALID_ARGUMENTS_LOG, new IllegalArgumentException(message));
            err.println(String.format(INVALID_ARGUMENTS_MESSAGE, message));
            return EXIT_INVALID_ARGUMENTS;
        }
        return execute(arguments.helperId());
    }

    /**
     * Executa o ajudante e registra o início e o fim (ou a falha) no log.
     */
    private int execute(HelperId id) {
        log.started(id);
        try {
            Optional<ExecutionRecord> record = runHelper.get().execute(id);
            Optional<HelperAction> failure = record.flatMap(HeadlessRunner::firstFailure);
            if (failure.isPresent()) {
                return fail(id, new IllegalStateException(failure.get().message()));
            }
            log.finished(id, record.map(r -> r.actions().size()).orElse(0));
            return EXIT_OK;
        } catch (Exception | LinkageError e) {
            return fail(id, e);
        }
    }

    /**
     * Procura, no registro, a primeira ação de falha (o caso de uso converte as falhas em ações).
     */
    private static Optional<HelperAction> firstFailure(ExecutionRecord record) {
        return record.actions().stream().filter(action -> action.type() == ActionType.FAILED).findFirst();
    }

    /**
     * Registra a falha no log e no erro e devolve o código de saída de falha.
     */
    private int fail(HelperId id, Throwable cause) {
        log.failed(id, cause);
        err.println(String.format(FAILURE_MESSAGE, id.value(), cause));
        return EXIT_FAILURE;
    }
}
