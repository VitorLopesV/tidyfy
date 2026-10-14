package com.tidyfy.domain;

import java.util.Locale;
import java.util.Optional;

/**
 * Extensão de um arquivo, normalizada: em minúsculas e sem o ponto (por exemplo, "Foto.JPG" vira "jpg").
 * Só vale a parte depois do último ponto; um arquivo sem extensão não tem {@code FileExtension}.
 *
 * @param value a extensão em minúsculas, sem ponto e sem espaços nas pontas
 */
public record FileExtension(String value) {

    // Constantes

    /** Caractere que separa o nome do arquivo da extensão. */
    private static final char SEPARATOR = '.';

    /** Mensagem de erro quando a extensão é nula, vazia ou só com espaços e ponto. */
    private static final String EXTENSION_REQUIRED = "A extensão do arquivo é obrigatória.";

    /**
     * Cria a extensão normalizando o valor: remove espaços das pontas e o ponto inicial, e usa minúsculas.
     *
     * @throws IllegalArgumentException se o valor for nulo ou ficar vazio depois da normalização
     */
    public FileExtension {
        if (value == null) {
            throw new IllegalArgumentException(EXTENSION_REQUIRED);
        }
        String normalized = value.strip();
        if (!normalized.isEmpty() && normalized.charAt(0) == SEPARATOR) {
            normalized = normalized.substring(1).strip();
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(EXTENSION_REQUIRED);
        }
        value = normalized.toLowerCase(Locale.ROOT);
    }

    /**
     * Obtém a extensão do nome de um arquivo, pela parte depois do último ponto
     * ("a.tar.gz" dá "gz"). Não há extensão quando o nome é nulo ou vazio, não tem ponto,
     * termina em ponto ou só tem o ponto no início (arquivo oculto, como ".gitignore").
     *
     * @param fileName o nome do arquivo (não o caminho)
     * @return a extensão normalizada, ou vazio se o arquivo não tiver extensão
     */
    public static Optional<FileExtension> of(String fileName) {
        if (fileName == null) {
            return Optional.empty();
        }
        String name = fileName.strip();
        int dot = name.lastIndexOf(SEPARATOR);
        if (dot <= 0 || dot == name.length() - 1) {
            return Optional.empty();
        }
        String extension = name.substring(dot + 1);
        if (extension.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new FileExtension(extension));
    }
}
