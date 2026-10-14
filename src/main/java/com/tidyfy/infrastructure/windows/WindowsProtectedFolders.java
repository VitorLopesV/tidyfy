package com.tidyfy.infrastructure.windows;

import com.tidyfy.domain.ProtectedFolders;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.function.Function;

/**
 * Monta as {@link ProtectedFolders} a partir das variáveis de ambiente do Windows.
 * <p>
 * Lê {@code WINDIR} (ou {@code SystemRoot}), {@code ProgramFiles}, {@code ProgramFiles(x86)} e
 * {@code ProgramData}. A pasta {@code System32} já está coberta, pois fica dentro de {@code WINDIR}.
 * Quando uma variável falta ou está em branco, a pasta é derivada da raiz do disco do Windows
 * ({@code <raiz>\Program Files}, {@code <raiz>\Program Files (x86)}, {@code <raiz>\ProgramData}).
 * Se nem {@code WINDIR} nem {@code SystemRoot} existirem, usa {@code C:\Windows} e as pastas derivadas
 * de {@code C:\} como último recurso.
 */
public final class WindowsProtectedFolders {

    // Constantes

    /** Variável de ambiente com a pasta do Windows. */
    private static final String WINDIR_VARIABLE = "WINDIR";

    /** Variável de ambiente alternativa com a pasta do Windows. */
    private static final String SYSTEM_ROOT_VARIABLE = "SystemRoot";

    /** Variável de ambiente com a pasta Program Files. */
    private static final String PROGRAM_FILES_VARIABLE = "ProgramFiles";

    /** Variável de ambiente com a pasta Program Files (x86). */
    private static final String PROGRAM_FILES_X86_VARIABLE = "ProgramFiles(x86)";

    /** Variável de ambiente com a pasta ProgramData. */
    private static final String PROGRAM_DATA_VARIABLE = "ProgramData";

    /** Raiz do disco usada como último recurso. */
    private static final String LAST_RESORT_ROOT = "C:\\";

    /** Nome da pasta do Windows usada como último recurso. */
    private static final String WINDOWS_FOLDER = "Windows";

    /** Nome padrão da pasta Program Files. */
    private static final String PROGRAM_FILES_FOLDER = "Program Files";

    /** Nome padrão da pasta Program Files (x86). */
    private static final String PROGRAM_FILES_X86_FOLDER = "Program Files (x86)";

    /** Nome padrão da pasta ProgramData. */
    private static final String PROGRAM_DATA_FOLDER = "ProgramData";

    /** Propriedade do sistema com a pasta do usuário. */
    private static final String USER_HOME_PROPERTY = "user.home";

    /** Mensagem de erro para um caminho inválido: valor informado. */
    private static final String INVALID_PATH_ERROR = "Caminho de pasta do sistema inválido: %s";

    /**
     * Classe utilitária, não deve ser instanciada.
     */
    private WindowsProtectedFolders() {
    }

    /**
     * Monta as regras com o ambiente real da máquina.
     *
     * @return as regras de pastas protegidas
     */
    public static ProtectedFolders fromSystem() {
        return from(System::getenv, Path.of(System.getProperty(USER_HOME_PROPERTY)));
    }

    /**
     * Monta as regras a partir do ambiente informado.
     *
     * @param environment consulta de variáveis de ambiente (devolve {@code null} se não existir)
     * @param userHome    pasta do usuário
     * @return as regras de pastas protegidas
     * @throws IllegalArgumentException se alguma variável contiver um caminho inválido
     */
    public static ProtectedFolders from(Function<String, String> environment, Path userHome) {
        String windowsValue = valueOf(environment, WINDIR_VARIABLE);
        if (windowsValue == null) {
            windowsValue = valueOf(environment, SYSTEM_ROOT_VARIABLE);
        }
        Path windowsDir = windowsValue == null
                ? Path.of(LAST_RESORT_ROOT).resolve(WINDOWS_FOLDER)
                : toPath(windowsValue);
        Path root = windowsDir.toAbsolutePath().getRoot();
        if (root == null) {
            root = Path.of(LAST_RESORT_ROOT);
        }
        return ProtectedFolders.forWindows(
                windowsDir,
                pathOrDefault(environment, PROGRAM_FILES_VARIABLE, root, PROGRAM_FILES_FOLDER),
                pathOrDefault(environment, PROGRAM_FILES_X86_VARIABLE, root, PROGRAM_FILES_X86_FOLDER),
                pathOrDefault(environment, PROGRAM_DATA_VARIABLE, root, PROGRAM_DATA_FOLDER),
                userHome);
    }

    /**
     * Lê uma variável de ambiente, tratando valor ausente ou em branco como inexistente.
     */
    private static String valueOf(Function<String, String> environment, String name) {
        String value = environment.apply(name);
        return value == null || value.isBlank() ? null : value;
    }

    /**
     * Devolve o caminho da variável ou, se ela faltar, a pasta padrão dentro da raiz do disco.
     */
    private static Path pathOrDefault(Function<String, String> environment, String name, Path root,
                                      String defaultFolder) {
        String value = valueOf(environment, name);
        return value == null ? root.resolve(defaultFolder) : toPath(value);
    }

    /**
     * Converte o texto em caminho, traduzindo falhas de sintaxe em {@link IllegalArgumentException}.
     */
    private static Path toPath(String value) {
        try {
            return Path.of(value);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(String.format(INVALID_PATH_ERROR, value), e);
        }
    }
}
