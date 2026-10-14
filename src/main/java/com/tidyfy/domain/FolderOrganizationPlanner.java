package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Calcula o que mover para onde na organização de pasta. É uma função pura: recebe só o que está direto
 * na pasta, não acessa o disco e não entra nas subpastas (elas são movidas inteiras).
 * Conflito de nome no destino não é tratado aqui.
 */
public final class FolderOrganizationPlanner {

    /** Categorias usadas para escolher a subpasta dos arquivos. */
    private final List<FileCategory> categories;

    /** Nomes das pastas do ajudante em minúsculas (categorias, "Pastas" e "Outros"), para comparar sem diferenciar caixa. */
    private final Set<String> helperFolderNames = new HashSet<>();

    // Constantes

    /** Nome da subpasta dos arquivos de extensão desconhecida ou sem extensão. */
    public static final String OTHERS_FOLDER = "Outros";

    /** Nome da subpasta das pastas que não são do ajudante. */
    public static final String FOLDERS_FOLDER = "Pastas";

    /** Nome do parâmetro das categorias, usado na mensagem de valor nulo. */
    private static final String CATEGORIES_NAME = "categories";

    /** Nome do parâmetro da pasta, usado na mensagem de valor nulo. */
    private static final String FOLDER_NAME = "folder";

    /** Nome do parâmetro dos itens, usado na mensagem de valor nulo. */
    private static final String ENTRIES_NAME = "entries";

    /**
     * Cria o planejador com as categorias padrão ({@link DefaultFileCategories}).
     */
    public FolderOrganizationPlanner() {
        this(DefaultFileCategories.all());
    }

    /**
     * Cria o planejador com as categorias informadas.
     *
     * @param categories as categorias dos arquivos
     * @throws NullPointerException se {@code categories} for nulo
     */
    public FolderOrganizationPlanner(List<FileCategory> categories) {
        this.categories = List.copyOf(Objects.requireNonNull(categories, CATEGORIES_NAME));
        for (FileCategory category : this.categories) {
            helperFolderNames.add(normalize(category.targetFolderName()));
        }
        helperFolderNames.add(normalize(FOLDERS_FOLDER));
        helperFolderNames.add(normalize(OTHERS_FOLDER));
    }

    /**
     * Devolve os movimentos necessários, na ordem dos itens recebidos. Arquivo vai para a subpasta da sua
     * categoria, ou para "Outros" se a extensão for desconhecida ou não existir; pasta vai para "Pastas".
     * As pastas do ajudante (categorias, "Pastas" e "Outros", sem diferenciar maiúsculas e minúsculas)
     * ficam onde estão e não geram movimento.
     *
     * @param folder  a pasta organizada
     * @param entries o que há direto na pasta
     * @return os movimentos a fazer, possivelmente vazio
     * @throws NullPointerException se {@code folder} ou {@code entries} for nulo
     */
    public List<FileMove> plan(Path folder, List<FolderEntry> entries) {
        Objects.requireNonNull(folder, FOLDER_NAME);
        Objects.requireNonNull(entries, ENTRIES_NAME);
        List<FileMove> moves = new ArrayList<>();
        for (FolderEntry entry : entries) {
            Path name = entry.path().getFileName();
            if (entry.directory() && isHelperFolder(name)) {
                continue;
            }
            String destination = entry.directory() ? FOLDERS_FOLDER : fileDestination(entry.path());
            moves.add(new FileMove(entry.path(), folder.resolve(destination).resolve(name)));
        }
        return moves;
    }

    /**
     * Escolhe a subpasta de um arquivo pela categoria dele.
     *
     * @param file o caminho do arquivo
     * @return o nome da subpasta da categoria, ou "Outros" se nenhuma servir
     */
    private String fileDestination(Path file) {
        return categories.stream()
            .filter(category -> category.matches(file))
            .map(FileCategory::targetFolderName)
            .findFirst()
            .orElse(OTHERS_FOLDER);
    }

    /**
     * Diz se o nome é de uma pasta do ajudante, sem diferenciar maiúsculas e minúsculas.
     *
     * @param name o nome da pasta
     * @return {@code true} se for uma categoria, "Pastas" ou "Outros"
     */
    private boolean isHelperFolder(Path name) {
        return helperFolderNames.contains(normalize(name.toString()));
    }

    /**
     * Normaliza um nome de pasta para comparação.
     *
     * @param name o nome da pasta
     * @return o nome em minúsculas
     */
    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
