package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Regras que um ajudante precisa cumprir, em relação aos demais, para ser salvo:
 * nome único, pasta não sensível e sem sobreposição de pastas entre ajudantes do mesmo tipo.
 */
public final class HelperRules {

    /** Regras de pastas sensíveis. */
    private final ProtectedFolders protectedFolders;

    // Constantes

    /** Nome do parâmetro das pastas protegidas, usado na mensagem de valor nulo. */
    private static final String PROTECTED_FOLDERS_NAME = "protectedFolders";

    /**
     * Cria as regras com as pastas sensíveis informadas.
     *
     * @param protectedFolders regras de pastas sensíveis
     * @throws NullPointerException se {@code protectedFolders} for nulo
     */
    public HelperRules(ProtectedFolders protectedFolders) {
        this.protectedFolders = Objects.requireNonNull(protectedFolders, PROTECTED_FOLDERS_NAME);
    }

    /**
     * Confere se o ajudante pode ser salvo. Recusa:
     * <ul>
     *   <li>o nome repetido (maiúsculas e minúsculas não diferenciam);</li>
     *   <li>a pasta sensível;</li>
     *   <li>a mesma pasta, ou pasta aninhada, de outro ajudante do mesmo tipo
     *       (organização e limpeza podem dividir a pasta);</li>
     *   <li>a lixeira, quando outro ajudante de limpeza já a limpa.</li>
     * </ul>
     * Os demais ajudantes contam estando ligados ou não.
     *
     * @param helper o ajudante a salvar
     * @param others os demais ajudantes; na edição, não inclui o próprio ajudante
     * @throws HelperNameAlreadyUsedException se outro ajudante já tem o mesmo nome
     * @throws ProtectedFolderException       se a pasta do ajudante é sensível
     * @throws FolderAlreadyInUseException    se a pasta ou a lixeira já é usada por outro ajudante do mesmo tipo
     */
    public void assertCanSave(Helper helper, List<Helper> others) {
        assertNameIsUnique(helper, others);
        folderOf(helper).ifPresent(protectedFolders::assertAllowed);
        assertFolderIsFree(helper, others);
    }

    /**
     * Recusa o ajudante cujo nome já é de algum dos demais, ignorando maiúsculas e minúsculas.
     *
     * @param helper o ajudante a salvar
     * @param others os demais ajudantes
     * @throws HelperNameAlreadyUsedException se o nome já estiver em uso
     */
    private void assertNameIsUnique(Helper helper, List<Helper> others) {
        String name = helper.name().value();
        for (Helper other : others) {
            if (other.name().value().equalsIgnoreCase(name)) {
                throw new HelperNameAlreadyUsedException(helper.name());
            }
        }
    }

    /**
     * Recusa o ajudante cuja pasta (ou a lixeira) se sobrepõe à de outro ajudante do mesmo tipo.
     *
     * @param helper o ajudante a salvar
     * @param others os demais ajudantes
     * @throws FolderAlreadyInUseException se houver sobreposição
     */
    private void assertFolderIsFree(Helper helper, List<Helper> others) {
        Optional<Path> folder = folderOf(helper);
        for (Helper other : others) {
            if (other.type() != helper.type()) {
                continue;
            }
            Optional<Path> otherFolder = folderOf(other);
            if (folder.isPresent() && otherFolder.isPresent()) {
                if (overlap(folder.get(), otherFolder.get())) {
                    throw FolderAlreadyInUseException.forFolder(folder.get(), other.name());
                }
            } else if (folder.isEmpty() && otherFolder.isEmpty()) {
                throw FolderAlreadyInUseException.forRecycleBin(other.name());
            }
        }
    }

    /**
     * Devolve a pasta em que o ajudante atua.
     *
     * @param helper o ajudante
     * @return a pasta, ou vazio quando o alvo é a lixeira
     */
    private static Optional<Path> folderOf(Helper helper) {
        return switch (helper) {
            case FolderOrganizerHelper organizer -> Optional.of(organizer.folder());
            case CleanerHelper cleaner -> switch (cleaner.target()) {
                case FolderTarget folderTarget -> Optional.of(folderTarget.folder());
                case RecycleBinTarget recycleBin -> Optional.empty();
            };
        };
    }

    /**
     * Diz se as pastas são a mesma ou se uma contém a outra, comparando caminhos normalizados
     * (no Windows, {@link Path#startsWith(Path)} não diferencia maiúsculas e minúsculas).
     *
     * @param first  uma pasta
     * @param second a outra pasta
     * @return {@code true} se houver sobreposição
     */
    private static boolean overlap(Path first, Path second) {
        Path a = first.toAbsolutePath().normalize();
        Path b = second.toAbsolutePath().normalize();
        return a.startsWith(b) || b.startsWith(a);
    }
}
