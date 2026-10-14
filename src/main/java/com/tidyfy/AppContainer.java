package com.tidyfy;

import com.tidyfy.application.usecase.CleanupUseCase;
import com.tidyfy.application.usecase.ListExecutionHistoryUseCase;
import com.tidyfy.application.usecase.ListHelpersUseCase;
import com.tidyfy.application.usecase.OrganizeFolderUseCase;
import com.tidyfy.application.usecase.RemoveHelperUseCase;
import com.tidyfy.application.usecase.RunHelperUseCase;
import com.tidyfy.application.usecase.SaveHelperUseCase;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.HelperRules;
import com.tidyfy.domain.ProtectedFolders;
import com.tidyfy.infrastructure.filesystem.NioFileSystem;
import com.tidyfy.infrastructure.persistence.ExecutionHistoryJsonMapper;
import com.tidyfy.infrastructure.persistence.HelperJsonMapper;
import com.tidyfy.infrastructure.persistence.JsonExecutionHistoryRepository;
import com.tidyfy.infrastructure.persistence.JsonHelperRepository;
import com.tidyfy.infrastructure.persistence.StorageLocation;
import com.tidyfy.infrastructure.windows.CommandRunner;
import com.tidyfy.infrastructure.windows.PowerShellRecycleBin;
import com.tidyfy.infrastructure.windows.ProcessCommandRunner;
import com.tidyfy.infrastructure.windows.SchtasksTaskScheduler;
import com.tidyfy.infrastructure.windows.WindowsProtectedFolders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

/**
 * Composition root do Tidyfy: instancia a infraestrutura e os casos de uso, ligando tudo por construtor,
 * sem framework, e entrega os casos de uso à interface e ao modo sem interface.
 * É a única classe que conhece todas as camadas. Cada caso de uso é uma instância única, devolvida sempre
 * a mesma a cada chamada.
 */
public final class AppContainer {

    /** Caso de uso de salvar ajudante. */
    private final SaveHelperUseCase saveHelper;

    /** Caso de uso de remover ajudantes. */
    private final RemoveHelperUseCase removeHelper;

    /** Caso de uso de listar ajudantes. */
    private final ListHelpersUseCase listHelpers;

    /** Caso de uso de consultar o histórico de execuções. */
    private final ListExecutionHistoryUseCase listExecutionHistory;

    /** Caso de uso de executar um ajudante. */
    private final RunHelperUseCase runHelper;

    // Constantes

    /** Nome da subpasta da pasta do Tidyfy onde o agendador grava os XML temporários. */
    private static final String TASKS_FOLDER_NAME = "tasks";

    /** Mensagem de erro ao criar a pasta dos XML temporários. */
    private static final String TASKS_FOLDER_ERROR = "Não foi possível criar a pasta %s";

    /** Nome da pasta do executável do Java, usada quando o executável do processo não é conhecido. */
    private static final String JAVA_BIN_FOLDER = "bin";

    /** Executável do Java sem janela de console, usado quando o executável do processo não é conhecido. */
    private static final String JAVA_FALLBACK_EXECUTABLE = "javaw.exe";

    /** Propriedade do sistema com a pasta de instalação do Java. */
    private static final String JAVA_HOME_PROPERTY = "java.home";

    /**
     * Monta o container com as dependências externas informadas.
     * A pasta {@code tasks} (dos XML temporários do agendador) é criada dentro da pasta do Tidyfy,
     * pois o agendador exige que ela exista.
     *
     * @param storage          local de armazenamento (configuração, histórico e pasta temporária)
     * @param launcher         executável que o Agendador do Windows chama com {@code --run {id}}
     * @param clock            relógio dos casos de uso
     * @param protectedFolders pastas sensíveis recusadas pelo domínio
     * @param commandRunner    executor de processos externos ({@code schtasks}, PowerShell)
     * @throws UncheckedIOException se a pasta {@code tasks} não puder ser criada
     */
    public AppContainer(StorageLocation storage, Path launcher, Clock clock, ProtectedFolders protectedFolders,
                        CommandRunner commandRunner) {
        JsonHelperRepository helpers = new JsonHelperRepository(storage, new HelperJsonMapper(), clock);
        JsonExecutionHistoryRepository history =
                new JsonExecutionHistoryRepository(storage, new ExecutionHistoryJsonMapper(), clock);
        SchtasksTaskScheduler scheduler =
                new SchtasksTaskScheduler(commandRunner, launcher, createTasksDirectory(storage));
        NioFileSystem fileSystem = new NioFileSystem(commandRunner);
        PowerShellRecycleBin recycleBin = new PowerShellRecycleBin(commandRunner);

        OrganizeFolderUseCase organizeFolder = new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner());
        CleanupUseCase cleanup = new CleanupUseCase(fileSystem, recycleBin, clock);

        this.saveHelper = new SaveHelperUseCase(helpers, scheduler, new HelperRules(protectedFolders));
        this.removeHelper = new RemoveHelperUseCase(helpers, scheduler);
        this.listHelpers = new ListHelpersUseCase(helpers);
        this.listExecutionHistory = new ListExecutionHistoryUseCase(history);
        this.runHelper = new RunHelperUseCase(helpers, organizeFolder, cleanup, history, clock);
    }

    /**
     * Cria o container de produção: {@code %APPDATA%\Tidyfy}, pastas sensíveis lidas do Windows,
     * relógio do sistema, processos reais e o executável do processo atual como launcher.
     *
     * @return o container de produção
     */
    public static AppContainer create() {
        return new AppContainer(StorageLocation.defaultLocation(), currentLauncher(), Clock.systemDefaultZone(),
                WindowsProtectedFolders.fromSystem(), new ProcessCommandRunner());
    }

    /**
     * Devolve o executável do processo atual, que a tarefa agendada chama com {@code --run {id}}.
     * Se o sistema não informar, usa o {@code javaw.exe} do Java em execução.
     * Provisório: a issue P2 (empacotamento) substitui isso pelo executável instalado do Tidyfy.
     *
     * @return o caminho do executável
     */
    static Path currentLauncher() {
        return ProcessHandle.current().info().command().map(Path::of).orElseGet(() ->
                Path.of(System.getProperty(JAVA_HOME_PROPERTY), JAVA_BIN_FOLDER, JAVA_FALLBACK_EXECUTABLE));
    }

    /**
     * Cria (se preciso) a pasta dos XML temporários do agendador.
     */
    private static Path createTasksDirectory(StorageLocation storage) {
        Path tasks = storage.directory().resolve(TASKS_FOLDER_NAME);
        try {
            return Files.createDirectories(tasks);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(TASKS_FOLDER_ERROR, tasks), e);
        }
    }

    /**
     * Caso de uso de salvar (criar ou editar) um ajudante.
     *
     * @return o caso de uso
     */
    public SaveHelperUseCase saveHelper() {
        return saveHelper;
    }

    /**
     * Caso de uso de remover ajudantes.
     *
     * @return o caso de uso
     */
    public RemoveHelperUseCase removeHelper() {
        return removeHelper;
    }

    /**
     * Caso de uso de listar os ajudantes.
     *
     * @return o caso de uso
     */
    public ListHelpersUseCase listHelpers() {
        return listHelpers;
    }

    /**
     * Caso de uso de consultar o histórico de execuções de um ajudante.
     *
     * @return o caso de uso
     */
    public ListExecutionHistoryUseCase listExecutionHistory() {
        return listExecutionHistory;
    }

    /**
     * Caso de uso de executar um ajudante (usado pelo modo sem interface).
     *
     * @return o caso de uso
     */
    public RunHelperUseCase runHelper() {
        return runHelper;
    }
}
