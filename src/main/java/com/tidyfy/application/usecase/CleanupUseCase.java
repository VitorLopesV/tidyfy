package com.tidyfy.application.usecase;

import com.tidyfy.application.port.FileSystem;
import com.tidyfy.application.port.RecycleBin;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.AgeLimit;
import com.tidyfy.domain.AgeReference;
import com.tidyfy.domain.CleanerHelper;
import com.tidyfy.domain.FolderEntry;
import com.tidyfy.domain.FolderTarget;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.RecycleBinTarget;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Executa a limpeza de um ajudante. Para uma pasta, manda para a lixeira os arquivos soltos (subpastas
 * não são tocadas), todos ou só os mais antigos que o {@link AgeLimit}, medindo a idade pela data
 * escolhida no alvo ({@link AgeReference}). Falha de E/S em um arquivo é registrada como
 * {@link ActionType#FAILED} e os demais continuam. Falha ao listar a pasta é propagada.
 */
public final class CleanupUseCase {

    /** Porta de acesso ao sistema de arquivos. */
    private final FileSystem fileSystem;

    /** Porta da lixeira do sistema. */
    private final RecycleBin recycleBin;

    /** Relógio usado para medir a idade dos arquivos. */
    private final Clock clock;

    // Constantes

    /** Nome do parâmetro do sistema de arquivos, usado na mensagem de valor nulo. */
    private static final String FILE_SYSTEM_NAME = "fileSystem";

    /** Nome do parâmetro da lixeira, usado na mensagem de valor nulo. */
    private static final String RECYCLE_BIN_NAME = "recycleBin";

    /** Nome do parâmetro do relógio, usado na mensagem de valor nulo. */
    private static final String CLOCK_NAME = "clock";

    /** Nome do parâmetro do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_NAME = "helper";

    /** Mensagem do ramo da lixeira, ainda não implementado. */
    private static final String RECYCLE_BIN_NOT_IMPLEMENTED = "Limpeza da lixeira ainda não implementada.";

    /**
     * Cria o caso de uso.
     *
     * @param fileSystem porta do sistema de arquivos
     * @param recycleBin porta da lixeira
     * @param clock      relógio que informa o instante atual
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public CleanupUseCase(FileSystem fileSystem, RecycleBin recycleBin, Clock clock) {
        this.fileSystem = Objects.requireNonNull(fileSystem, FILE_SYSTEM_NAME);
        this.recycleBin = Objects.requireNonNull(recycleBin, RECYCLE_BIN_NAME);
        this.clock = Objects.requireNonNull(clock, CLOCK_NAME);
    }

    /**
     * Executa a limpeza do ajudante conforme o alvo dele (pasta ou lixeira).
     *
     * @param helper ajudante de limpeza
     * @return as ações realizadas; vazia se não havia nada a fazer
     * @throws NullPointerException se {@code helper} for nulo
     * @throws UncheckedIOException se a pasta não puder ser listada
     */
    public List<HelperAction> execute(CleanerHelper helper) {
        Objects.requireNonNull(helper, HELPER_NAME);
        return switch (helper.target()) {
            case FolderTarget target -> cleanFolder(helper, target);
            case RecycleBinTarget target -> cleanRecycleBin(helper);
        };
    }

    // Região: limpeza de pasta

    /**
     * Manda para a lixeira os arquivos soltos da pasta que passaram do limite de idade (todos, se não
     * houver limite). Subpastas ficam intactas.
     *
     * @param helper ajudante de limpeza
     * @param target pasta a limpar
     * @return uma ação {@link ActionType#TRASHED} ou {@link ActionType#FAILED} por arquivo
     */
    private List<HelperAction> cleanFolder(CleanerHelper helper, FolderTarget target) {
        Optional<AgeLimit> limit = helper.findAgeLimit();
        Instant now = clock.instant();
        List<HelperAction> actions = new ArrayList<>();
        for (FolderEntry entry : fileSystem.listEntries(target.folder())) {
            if (!entry.directory() && shouldClean(entry, target.ageReference(), limit, now)) {
                actions.add(trash(entry));
            }
        }
        return actions;
    }

    /**
     * Diz se o arquivo deve ser limpo.
     *
     * @param entry     arquivo
     * @param reference data usada para medir a idade
     * @param limit     limite de idade, se houver
     * @param now       instante atual
     * @return {@code true} se não há limite ou se o arquivo passou dele
     */
    private boolean shouldClean(FolderEntry entry, AgeReference reference, Optional<AgeLimit> limit, Instant now) {
        Instant moment = reference == AgeReference.CREATED ? entry.createdAt() : entry.modifiedAt();
        return limit.map(l -> l.isExceededBy(moment, now)).orElse(true);
    }

    /**
     * Envia o arquivo para a lixeira.
     *
     * @param entry arquivo a enviar
     * @return a ação {@link ActionType#TRASHED}, ou {@link ActionType#FAILED} se houve falha de E/S
     */
    private HelperAction trash(FolderEntry entry) {
        try {
            fileSystem.moveToTrash(entry.path());
            return new HelperAction(ActionType.TRASHED, entry.path(), null, null);
        } catch (UncheckedIOException e) {
            return new HelperAction(ActionType.FAILED, entry.path(), null, e.getMessage());
        }
    }

    // Região: limpeza da lixeira

    /**
     * Esvazia a lixeira do sistema. Ainda não implementado.
     *
     * @param helper ajudante de limpeza
     * @return as ações realizadas
     * @throws UnsupportedOperationException enquanto não for implementado
     */
    private List<HelperAction> cleanRecycleBin(CleanerHelper helper) {
        throw new UnsupportedOperationException(RECYCLE_BIN_NOT_IMPLEMENTED);
    }
}
