package com.tidyfy.application.usecase;

import com.tidyfy.application.port.FileSystem;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import com.tidyfy.domain.FolderOrganizationPlanner;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.HelperAction;
import com.tidyfy.domain.NumberedName;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Organiza a pasta de um ajudante: lista o que há nela, pede ao planejador o que mover para onde e trata
 * o destino de cada movimento. Destino livre: move. Destino ocupado por arquivo de mesmo conteúdo: o
 * arquivo de fora vai para a lixeira. Destino ocupado com conteúdo diferente, ou sendo o item uma pasta
 * (pasta nunca é considerada duplicada): move com o nome numerado. Falha de E/S em um item é registrada
 * como {@link ActionType#FAILED} e os demais itens continuam. Arquivos em uso não são tratados.
 */
public final class OrganizeFolderUseCase {

    /** Porta de acesso ao sistema de arquivos. */
    private final FileSystem fileSystem;

    /** Planejador que decide o que mover para onde. */
    private final FolderOrganizationPlanner planner;

    // Constantes

    /** Nome do parâmetro do sistema de arquivos, usado na mensagem de valor nulo. */
    private static final String FILE_SYSTEM_NAME = "fileSystem";

    /** Nome do parâmetro do planejador, usado na mensagem de valor nulo. */
    private static final String PLANNER_NAME = "planner";

    /** Nome do parâmetro do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_NAME = "helper";

    /**
     * Cria o caso de uso.
     *
     * @param fileSystem porta do sistema de arquivos
     * @param planner    planejador da organização
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public OrganizeFolderUseCase(FileSystem fileSystem, FolderOrganizationPlanner planner) {
        this.fileSystem = Objects.requireNonNull(fileSystem, FILE_SYSTEM_NAME);
        this.planner = Objects.requireNonNull(planner, PLANNER_NAME);
    }

    /**
     * Organiza a pasta do ajudante. Se a pasta não puder ser listada, a falha de E/S é propagada
     * ao chamador, que a registra como falha da execução.
     *
     * @param helper ajudante de organização de pasta
     * @return as ações realizadas, na ordem dos movimentos; vazia se não havia nada a fazer
     * @throws NullPointerException se {@code helper} for nulo
     * @throws UncheckedIOException se a pasta não puder ser listada
     */
    public List<HelperAction> execute(FolderOrganizerHelper helper) {
        Objects.requireNonNull(helper, HELPER_NAME);
        List<FolderEntry> entries = fileSystem.listEntries(helper.folder());
        Set<Path> directories = new HashSet<>();
        for (FolderEntry entry : entries) {
            if (entry.directory()) {
                directories.add(entry.path());
            }
        }
        List<HelperAction> actions = new ArrayList<>();
        for (FileMove move : planner.plan(helper.folder(), entries)) {
            actions.add(organize(move, directories.contains(move.source())));
        }
        return actions;
    }

    /**
     * Aplica um movimento tratando o conflito de nome no destino.
     *
     * @param move      movimento planejado
     * @param directory se a origem é uma pasta
     * @return a ação realizada, ou a falha ocorrida
     */
    private HelperAction organize(FileMove move, boolean directory) {
        try {
            if (!fileSystem.exists(move.target())) {
                return moveTo(move.source(), move.target());
            }
            if (!directory && fileSystem.sameContent(move.source(), move.target())) {
                fileSystem.moveToTrash(move.source());
                return new HelperAction(ActionType.DUPLICATE_TRASHED, move.source(), null, null);
            }
            Path parent = move.target().getParent();
            String freeName = NumberedName.firstFree(move.target().getFileName().toString(),
                name -> fileSystem.exists(parent.resolve(name)));
            return moveTo(move.source(), parent.resolve(freeName));
        } catch (UncheckedIOException e) {
            return new HelperAction(ActionType.FAILED, move.source(), move.target(), e.getMessage());
        }
    }

    /**
     * Move o item e devolve a ação correspondente.
     *
     * @param source origem
     * @param target destino livre
     * @return a ação {@link ActionType#MOVED}
     */
    private HelperAction moveTo(Path source, Path target) {
        fileSystem.move(new FileMove(source, target));
        return new HelperAction(ActionType.MOVED, source, target, null);
    }
}
