package com.tidyfy.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Catálogo padrão de categorias de arquivos usado pela organização de pasta.
 * "Pastas" e "Outros" não são categorias: não têm extensões.
 */
public final class DefaultFileCategories {

    // Constantes

    /** Nome da categoria de imagens. */
    private static final String IMAGES_NAME = "Imagens";

    /** Pasta de destino das imagens. */
    private static final String IMAGES_FOLDER = "Imagens";

    /** Extensões das imagens. */
    private static final String[] IMAGES_EXTENSIONS = {
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "heic", "tiff", "ico"
    };

    /** Nome da categoria de vídeos. */
    private static final String VIDEOS_NAME = "Vídeos";

    /** Pasta de destino dos vídeos. */
    private static final String VIDEOS_FOLDER = "Vídeos";

    /** Extensões dos vídeos. */
    private static final String[] VIDEOS_EXTENSIONS = {
        "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v"
    };

    /** Nome da categoria de documentos. */
    private static final String DOCUMENTS_NAME = "Documentos";

    /** Pasta de destino dos documentos. */
    private static final String DOCUMENTS_FOLDER = "Documentos";

    /** Extensões dos documentos. */
    private static final String[] DOCUMENTS_EXTENSIONS = {
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "ods", "odp", "csv"
    };

    /** Nome da categoria de arquivos compactados. */
    private static final String ARCHIVES_NAME = "Compactados";

    /** Pasta de destino dos arquivos compactados. */
    private static final String ARCHIVES_FOLDER = "Compactados";

    /** Extensões dos arquivos compactados. */
    private static final String[] ARCHIVES_EXTENSIONS = {"zip", "rar", "7z", "tar", "gz"};

    /** Nome da categoria de executáveis. */
    private static final String EXECUTABLES_NAME = "Executáveis";

    /** Pasta de destino dos executáveis. */
    private static final String EXECUTABLES_FOLDER = "Executáveis";

    /** Extensões dos executáveis. */
    private static final String[] EXECUTABLES_EXTENSIONS = {"exe", "msi", "bat"};

    /** Categorias padrão, na ordem alfabética das pastas de destino. */
    private static final List<FileCategory> ALL = List.of(
        category(ARCHIVES_NAME, ARCHIVES_FOLDER, ARCHIVES_EXTENSIONS),
        category(DOCUMENTS_NAME, DOCUMENTS_FOLDER, DOCUMENTS_EXTENSIONS),
        category(EXECUTABLES_NAME, EXECUTABLES_FOLDER, EXECUTABLES_EXTENSIONS),
        category(IMAGES_NAME, IMAGES_FOLDER, IMAGES_EXTENSIONS),
        category(VIDEOS_NAME, VIDEOS_FOLDER, VIDEOS_EXTENSIONS));

    /**
     * Classe utilitária: não deve ser instanciada.
     */
    private DefaultFileCategories() {
    }

    /**
     * Devolve as categorias padrão: Compactados, Documentos, Executáveis, Imagens e Vídeos.
     * Cada extensão pertence a uma única categoria.
     *
     * @return a lista imutável de categorias
     */
    public static List<FileCategory> all() {
        return ALL;
    }

    /**
     * Monta uma categoria a partir do nome, da pasta de destino e das extensões em texto.
     *
     * @param name       o nome da categoria
     * @param folder     o nome da pasta de destino
     * @param extensions as extensões em texto
     * @return a categoria criada
     */
    private static FileCategory category(String name, String folder, String[] extensions) {
        Set<FileExtension> parsed = Arrays.stream(extensions)
            .map(FileExtension::new)
            .collect(Collectors.toSet());
        return new FileCategory(name, folder, parsed);
    }
}
