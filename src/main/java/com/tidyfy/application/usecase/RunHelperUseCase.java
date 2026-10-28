package com.tidyfy.application.usecase;

import com.tidyfy.application.port.ExecutionHistoryRepository;
import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.HelperId;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Ponto de entrada da execução agendada de um ajudante. Carrega o ajudante, escolhe o caso de uso
 * conforme o tipo, grava o {@link ExecutionRecord} no histórico e apaga os registros expirados.
 * Ajudante inexistente ou desligado não faz nada e não grava histórico. Se o caso de uso lançar uma
 * {@link RuntimeException}, ela não é propagada: vira uma única ação {@link ActionType#FAILED} no
 * registro. O modo sem interface (HeadlessRunner) lê as ações FAILED do registro devolvido e grava o log em arquivo.
 * Falha ao gravar o histórico é propagada.
 */
public final class RunHelperUseCase {

    /** Repositório de onde o ajudante é carregado. */
    private final HelperRepository repository;

    /** Caso de uso da organização de pasta. */
    private final OrganizeFolderUseCase organizeFolder;

    /** Caso de uso da limpeza. */
    private final CleanupUseCase cleanup;

    /** Histórico onde a execução é registrada. */
    private final ExecutionHistoryRepository history;

    /** Relógio que informa o instante da execução. */
    private final Clock clock;

    // Constantes

    /** Mensagem de erro: o tipo ainda não é tratado nesta camada (será ligado nas próximas etapas). */
    private static final String UNSUPPORTED_TYPE = "O Organizador da área de trabalho ainda não é suportado aqui.";

    /** Nome do parâmetro do repositório, usado na mensagem de valor nulo. */
    private static final String REPOSITORY_NAME = "repository";

    /** Nome do parâmetro da organização de pasta, usado na mensagem de valor nulo. */
    private static final String ORGANIZE_FOLDER_NAME = "organizeFolder";

    /** Nome do parâmetro da limpeza, usado na mensagem de valor nulo. */
    private static final String CLEANUP_NAME = "cleanup";

    /** Nome do parâmetro do histórico, usado na mensagem de valor nulo. */
    private static final String HISTORY_NAME = "history";

    /** Nome do parâmetro do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /** Nome do parâmetro do identificador, usado na mensagem de valor nulo. */
    private static final String ID_NAME = "id";

    /**
     * Cria o caso de uso.
     *
     * @param repository     repositório de ajudantes
     * @param organizeFolder caso de uso da organização de pasta
     * @param cleanup        caso de uso da limpeza
     * @param history        histórico de execuções
     * @param clock          relógio que informa o instante atual
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public RunHelperUseCase(HelperRepository repository, OrganizeFolderUseCase organizeFolder,
            CleanupUseCase cleanup, ExecutionHistoryRepository history, Clock clock) {
        this.repository = Objects.requireNonNull(repository, REPOSITORY_NAME);
        this.organizeFolder = Objects.requireNonNull(organizeFolder, ORGANIZE_FOLDER_NAME);
        this.cleanup = Objects.requireNonNull(cleanup, CLEANUP_NAME);
        this.history = Objects.requireNonNull(history, HISTORY_NAME);
        this.clock = Objects.requireNonNull(clock, CLOCK_NAME);
    }

    /**
     * Executa o ajudante, registra a execução e apaga os registros com mais de 30 dias.
     * Não faz nada se o ajudante não existir ou estiver desligado.
     *
     * @param id identificador do ajudante
     * @return o registro gravado no histórico, ou vazio se o ajudante não existe ou está desligado
     * @throws NullPointerException se {@code id} for nulo
     * @throws java.io.UncheckedIOException se o histórico não puder ser gravado
     */
    public Optional<ExecutionRecord> execute(HelperId id) {
        Objects.requireNonNull(id, ID_NAME);
        Optional<Helper> found = repository.findById(id);
        if (found.isEmpty() || !found.get().enabled()) {
            return Optional.empty();
        }
        Helper helper = found.get();
        List<HelperAction> actions = run(helper);
        Instant now = clock.instant();
        ExecutionRecord record = new ExecutionRecord(helper.id(), helper.name().value(), now, actions);
        history.add(record);
        history.deleteExpired(now);
        return Optional.of(record);
    }

    /**
     * Executa o caso de uso do tipo do ajudante, convertendo qualquer falha em uma ação de falha.
     *
     * @param helper ajudante a executar
     * @return as ações realizadas, ou uma única ação {@link ActionType#FAILED}
     */
    private List<HelperAction> run(Helper helper) {
        try {
            return switch (helper) {
                case FolderOrganizerHelper organizer -> organizeFolder.execute(organizer);
                case CleanerHelper cleaner -> cleanup.execute(cleaner);
                case DesktopArrangerHelper desktop -> throw new UnsupportedOperationException(UNSUPPORTED_TYPE);
            };
        } catch (RuntimeException failure) {
            String message = failure.getMessage() != null ? failure.getMessage()
                    : failure.getClass().getSimpleName();
            return List.of(new HelperAction(ActionType.FAILED, null, null, message));
        }
    }
}
