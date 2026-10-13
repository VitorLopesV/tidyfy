package com.tidyfy.domain;

import java.nio.file.Path;
import java.util.List;

/**
 * Regras de pastas sensíveis. Não acessa o disco nem o Windows: recebe os caminhos prontos.
 * Recusa, com tudo que há dentro, as pastas do sistema (Windows, Program Files,
 * Program Files (x86) e ProgramData). Recusa só elas mesmas a raiz de qualquer disco
 * e a pasta do usuário; as pastas dentro da pasta do usuário (como Downloads) são aceitas.
 */
public final class ProtectedFolders {

    /** Pastas recusadas junto com tudo que há dentro delas. */
    private final List<Path> protectedTrees;

    /** Pastas recusadas somente elas mesmas. */
    private final List<Path> protectedExact;

    /**
     * Cria as regras com as pastas sensíveis informadas.
     *
     * @param protectedTrees pastas recusadas com tudo que há dentro
     * @param protectedExact pastas recusadas somente elas mesmas
     */
    private ProtectedFolders(List<Path> protectedTrees, List<Path> protectedExact) {
        this.protectedTrees = protectedTrees;
        this.protectedExact = protectedExact;
    }

    /**
     * Cria as regras para o Windows a partir dos caminhos das pastas sensíveis.
     *
     * @param windowsDir pasta do Windows (ex.: C:\Windows)
     * @param programFiles pasta Program Files
     * @param programFilesX86 pasta Program Files (x86)
     * @param programData pasta ProgramData
     * @param userHome pasta do usuário (ex.: C:\Users\nome)
     * @return as regras de pastas protegidas
     */
    public static ProtectedFolders forWindows(Path windowsDir, Path programFiles, Path programFilesX86,
                                              Path programData, Path userHome) {
        return new ProtectedFolders(
                List.of(normalize(windowsDir), normalize(programFiles), normalize(programFilesX86),
                        normalize(programData)),
                List.of(normalize(userHome)));
    }

    /**
     * Garante que a pasta pode ser usada por um ajudante.
     *
     * @param folder a pasta escolhida
     * @throws ProtectedFolderException se a pasta é sensível: está dentro de uma pasta do sistema,
     *                                  é a raiz de um disco ou é a pasta do usuário
     */
    public void assertAllowed(Path folder) {
        Path candidate = normalize(folder);
        boolean isRoot = candidate.equals(candidate.getRoot());
        boolean isExact = protectedExact.contains(candidate);
        boolean isInsideTree = protectedTrees.stream().anyMatch(candidate::startsWith);
        if (isRoot || isExact || isInsideTree) {
            throw new ProtectedFolderException(folder);
        }
    }

    /**
     * Devolve o caminho absoluto e sem segmentos redundantes ("." e "..").
     */
    private static Path normalize(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
