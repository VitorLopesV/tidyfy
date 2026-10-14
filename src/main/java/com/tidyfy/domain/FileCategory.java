package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.Set;

/**
 * Categoria de arquivos da organização de pasta: um nome, a subpasta de destino e as extensões que pertencem a ela.
 * A comparação é feita só pelo nome do arquivo, sem acessar o disco.
 *
 * @param name             o nome da categoria
 * @param targetFolderName o nome da subpasta para onde vão os arquivos da categoria
 * @param extensions       as extensões da categoria (ao menos uma)
 */
public record FileCategory(String name, String targetFolderName, Set<FileExtension> extensions) {

    // Constantes

    /** Mensagem de erro quando o nome da categoria é nulo ou vazio. */
    private static final String NAME_REQUIRED = "O nome da categoria é obrigatório.";

    /** Mensagem de erro quando o nome da pasta de destino é nulo ou vazio. */
    private static final String TARGET_FOLDER_REQUIRED = "O nome da pasta de destino é obrigatório.";

    /** Mensagem de erro quando a categoria não tem extensões. */
    private static final String EXTENSIONS_REQUIRED = "A categoria precisa de ao menos uma extensão.";

    /**
     * Cria a categoria validando os dados e guardando uma cópia imutável das extensões.
     *
     * @throws IllegalArgumentException se o nome ou a pasta de destino forem nulos ou vazios,
     *                                  ou se não houver extensões
     */
    public FileCategory {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        if (targetFolderName == null || targetFolderName.isBlank()) {
            throw new IllegalArgumentException(TARGET_FOLDER_REQUIRED);
        }
        if (extensions == null || extensions.isEmpty()) {
            throw new IllegalArgumentException(EXTENSIONS_REQUIRED);
        }
        extensions = Set.copyOf(extensions);
    }

    /**
     * Diz se o arquivo pertence à categoria, pela extensão normalizada do nome dele
     * ("Foto.JPG" tem a extensão "jpg"). Arquivos sem extensão não pertencem a nenhuma categoria.
     *
     * @param file o caminho do arquivo; só o nome é considerado
     * @return {@code true} se a extensão do arquivo for uma das extensões da categoria
     */
    public boolean matches(Path file) {
        Path fileName = file.getFileName();
        if (fileName == null) {
            return false;
        }
        return FileExtension.of(fileName.toString()).map(extensions::contains).orElse(false);
    }
}
