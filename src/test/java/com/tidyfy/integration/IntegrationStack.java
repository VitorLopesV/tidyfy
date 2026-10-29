package com.tidyfy.integration;

import com.tidyfy.application.fake.FakeDesktopIcons;
import com.tidyfy.application.fake.FakeRecycleBin;
import com.tidyfy.application.usecase.ArrangeDesktopUseCase;
import com.tidyfy.application.usecase.CleanupUseCase;
import com.tidyfy.application.usecase.ListExecutionHistoryUseCase;
import com.tidyfy.application.usecase.OrganizeFolderUseCase;
import com.tidyfy.application.usecase.RunHelperUseCase;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.CleanupTarget;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopLayout;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.RecycleBinTarget;
import com.tidyfy.domain.Schedule;
import com.tidyfy.infrastructure.filesystem.NioFileSystem;
import com.tidyfy.infrastructure.persistence.ExecutionHistoryJsonMapper;
import com.tidyfy.infrastructure.persistence.HelperJsonMapper;
import com.tidyfy.infrastructure.persistence.JsonExecutionHistoryRepository;
import com.tidyfy.infrastructure.persistence.JsonHelperRepository;
import com.tidyfy.infrastructure.persistence.StorageLocation;
import com.tidyfy.infrastructure.windows.CommandResult;
import com.tidyfy.infrastructure.windows.CommandRunner;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Pilha real dos ajudantes montada à mão para os testes de integração: sistema de arquivos e repositórios JSON
 * de verdade, dentro de uma pasta temporária. A lixeira é simulada por um {@link CommandRunner} dublê, que
 * apaga do disco o item que o comando do PowerShell mandaria para a Lixeira (nenhum processo real é executado),
 * e a lixeira do Windows é um {@link FakeRecycleBin}.
 */
final class IntegrationStack {

    /** Instante fixo "agora" de todos os testes. */
    static final Instant NOW = Instant.parse("2026-06-15T12:00:00Z");

    /** Relógio fixo usado pelos casos de uso. */
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    /** Padrão que extrai o caminho do comando de envio à lixeira. */
    private static final Pattern TRASH_PATH =
            Pattern.compile("::Delete(?:File|Directory)\\('(.*)', 'OnlyErrorDialogs'");

    /** Agenda qualquer; os testes não usam o agendador. */
    private static final Schedule SCHEDULE = new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    /** Caminhos enviados à lixeira dublê, em ordem. */
    private final List<Path> trashed = new ArrayList<>();

    /** Caminhos cujo envio à lixeira falha com código de saída 1. */
    private final Set<Path> failingPaths = new HashSet<>();

    /** Lixeira do Windows em memória. */
    private final FakeRecycleBin recycleBin = new FakeRecycleBin();

    /** Área de trabalho em memória. */
    private final FakeDesktopIcons desktopIcons = new FakeDesktopIcons();

    /** Repositório de ajudantes em JSON. */
    private final JsonHelperRepository helpers;

    /** Caso de uso de execução. */
    private final RunHelperUseCase runHelper;

    /** Caso de uso de consulta do histórico. */
    private final ListExecutionHistoryUseCase listHistory;

    /**
     * Monta a pilha guardando a configuração e o histórico na pasta informada.
     *
     * @param storageFolder pasta temporária de armazenamento
     */
    IntegrationStack(Path storageFolder) {
        StorageLocation storage = new StorageLocation(storageFolder);
        helpers = new JsonHelperRepository(storage, new HelperJsonMapper(), CLOCK);
        JsonExecutionHistoryRepository history =
                new JsonExecutionHistoryRepository(storage, new ExecutionHistoryJsonMapper(), CLOCK);
        NioFileSystem fileSystem = new NioFileSystem((CommandRunner) this::simulateTrash);
        runHelper = new RunHelperUseCase(helpers,
                new OrganizeFolderUseCase(fileSystem, new FolderOrganizationPlanner()),
                new CleanupUseCase(fileSystem, recycleBin, CLOCK),
                new ArrangeDesktopUseCase(desktopIcons, new DesktopLayoutPlanner()), history, CLOCK);
        listHistory = new ListExecutionHistoryUseCase(history);
    }

    /**
     * Imita o envio à lixeira: apaga o item do disco ou falha, conforme configurado.
     *
     * @param command comando do PowerShell montado por {@code TrashCommands}
     * @return resultado simulado do processo
     */
    private CommandResult simulateTrash(List<String> command) {
        Matcher matcher = TRASH_PATH.matcher(command.get(command.size() - 1));
        if (!matcher.find()) {
            return new CommandResult(2, "comando inesperado");
        }
        Path path = Path.of(matcher.group(1).replace("''", "'"));
        if (failingPaths.contains(path)) {
            return new CommandResult(1, "acesso negado");
        }
        trashed.add(path);
        deleteRecursively(path);
        return new CommandResult(0, "");
    }

    /**
     * Apaga um arquivo ou pasta com tudo que há dentro.
     *
     * @param path item a apagar
     */
    private static void deleteRecursively(Path path) {
        try (Stream<Path> walk = Files.walk(path)) {
            for (Path item : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(item);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Faz o envio do caminho à lixeira dublê falhar.
     *
     * @param path caminho que falhará
     */
    void failTrashOf(Path path) {
        failingPaths.add(path);
    }

    /**
     * Caminhos enviados à lixeira dublê.
     *
     * @return caminhos, em ordem
     */
    List<Path> trashed() {
        return List.copyOf(trashed);
    }

    /**
     * Lixeira do Windows em memória.
     *
     * @return o dublê
     */
    FakeRecycleBin recycleBin() {
        return recycleBin;
    }

    /**
     * Caso de uso de consulta do histórico.
     *
     * @return o caso de uso
     */
    ListExecutionHistoryUseCase listHistory() {
        return listHistory;
    }

    /**
     * Executa o ajudante pelo caso de uso de execução.
     *
     * @param helper ajudante a executar
     * @return o registro gravado, ou vazio se o ajudante não executou
     */
    Optional<ExecutionRecord> run(Helper helper) {
        return runHelper.execute(helper.id());
    }

    /**
     * Salva um ajudante organizador ligado.
     *
     * @param folder pasta a organizar
     * @return o ajudante salvo
     */
    FolderOrganizerHelper saveOrganizer(Path folder) {
        FolderOrganizerHelper helper = new FolderOrganizerHelper(HelperId.newId(), new HelperName("Organizar"),
                SCHEDULE, true, folder);
        helpers.save(helper);
        return helper;
    }

    /**
     * Salva um ajudante de limpeza.
     *
     * @param target   alvo da limpeza
     * @param ageLimit limite de idade, ou {@code null} para limpar tudo
     * @param enabled  se o ajudante está ligado
     * @return o ajudante salvo
     */
    CleanerHelper saveCleaner(CleanupTarget target, AgeLimit ageLimit, boolean enabled) {
        CleanerHelper helper = new CleanerHelper(HelperId.newId(), new HelperName("Limpar"), SCHEDULE, enabled,
                target, ageLimit);
        helpers.save(helper);
        return helper;
    }

    /**
     * Salva um ajudante ligado que limpa uma pasta.
     *
     * @param folder    pasta alvo
     * @param reference data usada para medir a idade
     * @param ageLimit  limite de idade, ou {@code null} para limpar tudo
     * @return o ajudante salvo
     */
    CleanerHelper saveFolderCleaner(Path folder, AgeReference reference, AgeLimit ageLimit) {
        return saveCleaner(new FolderTarget(folder, reference), ageLimit, true);
    }

    /**
     * Salva um ajudante ligado que limpa a lixeira.
     *
     * @param ageLimit limite de idade, ou {@code null} para esvaziar tudo
     * @return o ajudante salvo
     */
    CleanerHelper saveRecycleBinCleaner(AgeLimit ageLimit) {
        return saveCleaner(new RecycleBinTarget(), ageLimit, true);
    }

    /**
     * Salva um ajudante ligado que organiza a área de trabalho.
     *
     * @param layout layout dos ícones
     * @return o ajudante salvo
     */
    DesktopArrangerHelper saveDesktopArranger(DesktopLayout layout) {
        DesktopArrangerHelper helper = new DesktopArrangerHelper(HelperId.newId(), new HelperName("Área de trabalho"),
                SCHEDULE, true, layout);
        helpers.save(helper);
        return helper;
    }

    /**
     * Área de trabalho em memória.
     *
     * @return o dublê
     */
    FakeDesktopIcons desktopIcons() {
        return desktopIcons;
    }

    /**
     * Cria um arquivo com o conteúdo informado, criando as pastas que faltarem.
     *
     * @param file    caminho do arquivo
     * @param content conteúdo de texto
     * @return o caminho criado
     */
    static Path write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            return Files.writeString(file, content);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Define as datas de modificação e de criação de um arquivo.
     *
     * @param file     arquivo
     * @param modified nova data de modificação
     * @param created  nova data de criação
     */
    static void setTimes(Path file, Instant modified, Instant created) {
        try {
            Files.getFileAttributeView(file, BasicFileAttributeView.class)
                    .setTimes(FileTime.from(modified), null, FileTime.from(created));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Instante {@code days} dias antes de {@link #NOW}.
     *
     * @param days dias atrás
     * @return o instante
     */
    static Instant daysAgo(long days) {
        return NOW.minus(Duration.ofDays(days));
    }
}
