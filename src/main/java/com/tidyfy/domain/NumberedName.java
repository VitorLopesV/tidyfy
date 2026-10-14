package com.tidyfy.domain;

import java.util.function.Predicate;

/**
 * Escolhe um nome livre para um arquivo ou pasta quando o destino já existe, numerando o nome:
 * "teste.pdf" vira "teste(1).pdf", depois "teste(2).pdf". O número entra antes da última extensão
 * ("a.tar.gz" vira "a.tar(1).gz"). Pastas, arquivos sem extensão, arquivos ocultos (".gitignore")
 * e nomes terminados em ponto recebem o número no fim ("Fotos(1)", ".gitignore(1)", "nome.(1)").
 */
public final class NumberedName {

    // Constantes

    /** Caractere que separa o nome da extensão. */
    private static final char SEPARATOR = '.';

    /** Modelo do nome numerado: nome sem extensão, número e extensão (com o ponto, se houver). */
    private static final String NUMBERED_TEMPLATE = "%s(%d)%s";

    /** Extensão vazia, usada quando o nome não tem extensão. */
    private static final String NO_EXTENSION = "";

    /** Mensagem de erro quando o nome é nulo ou vazio. */
    private static final String NAME_REQUIRED = "O nome é obrigatório.";

    /** Mensagem de erro quando o teste de existência é nulo. */
    private static final String EXISTS_REQUIRED = "O teste de existência é obrigatório.";

    /** Primeiro número usado na numeração. */
    private static final int FIRST_NUMBER = 1;

    /**
     * Classe utilitária: não deve ser instanciada.
     */
    private NumberedName() {
    }

    /**
     * Devolve o primeiro nome livre: o próprio nome, se estiver livre, ou o nome numerado
     * com o menor número (a partir de 1) que ainda não existe.
     *
     * @param name   o nome desejado (não o caminho)
     * @param exists diz se um nome já existe no destino
     * @return o primeiro nome livre
     * @throws IllegalArgumentException se o nome for nulo ou vazio, ou se {@code exists} for nulo
     */
    public static String firstFree(String name, Predicate<String> exists) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException(NAME_REQUIRED);
        }
        if (exists == null) {
            throw new IllegalArgumentException(EXISTS_REQUIRED);
        }
        if (!exists.test(name)) {
            return name;
        }
        int dot = name.lastIndexOf(SEPARATOR);
        boolean hasExtension = dot > 0 && dot < name.length() - 1;
        String stem = hasExtension ? name.substring(0, dot) : name;
        String extension = hasExtension ? name.substring(dot) : NO_EXTENSION;
        int number = FIRST_NUMBER;
        String candidate = String.format(NUMBERED_TEMPLATE, stem, number, extension);
        while (exists.test(candidate)) {
            number++;
            candidate = String.format(NUMBERED_TEMPLATE, stem, number, extension);
        }
        return candidate;
    }
}
