package com.tidyfy.infrastructure.filesystem;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Monta o comando do {@code powershell.exe} que envia um arquivo ou uma pasta à Lixeira do Windows sem
 * precisar de interface gráfica (o {@code Desktop.moveToTrash} falha com {@code HeadlessException}).
 * Usa {@code Microsoft.VisualBasic.FileIO.FileSystem.DeleteFile} ou {@code DeleteDirectory} com
 * {@code SendToRecycleBin}. O comando é entregue como lista de argumentos (sem shell); só o caminho dentro
 * do script precisa de escape: aspas simples são dobradas. Limitação conhecida: caminhos com mais de
 * 260 caracteres não são aceitos pelo {@code Microsoft.VisualBasic} e o PowerShell sai com código 1.
 */
public final class TrashCommands {

    // Constantes

    /** Programa do PowerShell. */
    private static final String POWERSHELL = "powershell.exe";

    /** Opção que não carrega o perfil do usuário. */
    private static final String NO_PROFILE_OPTION = "-NoProfile";

    /** Opção que impede perguntas ao usuário. */
    private static final String NON_INTERACTIVE_OPTION = "-NonInteractive";

    /** Opção que define o estilo da janela. */
    private static final String WINDOW_STYLE_OPTION = "-WindowStyle";

    /** Valor que mantém a janela oculta. */
    private static final String HIDDEN = "Hidden";

    /** Opção da política de execução. */
    private static final String EXECUTION_POLICY_OPTION = "-ExecutionPolicy";

    /** Valor da política de execução que não bloqueia o script. */
    private static final String BYPASS = "Bypass";

    /** Opção que recebe o script. */
    private static final String COMMAND_OPTION = "-Command";

    /** Modelo do script: método do {@code FileSystem} e caminho (já escapado). */
    private static final String SCRIPT_TEMPLATE =
            "Add-Type -AssemblyName Microsoft.VisualBasic; "
                    + "[Microsoft.VisualBasic.FileIO.FileSystem]::%s('%s', 'OnlyErrorDialogs', 'SendToRecycleBin')";

    /** Método que envia um arquivo à lixeira. */
    private static final String DELETE_FILE_METHOD = "DeleteFile";

    /** Método que envia uma pasta, com o conteúdo, à lixeira. */
    private static final String DELETE_DIRECTORY_METHOD = "DeleteDirectory";

    /** Aspas simples, que delimitam o caminho no script. */
    private static final String SINGLE_QUOTE = "'";

    /** Aspas simples dobradas, a forma de escapá-las dentro de um texto entre aspas simples. */
    private static final String ESCAPED_SINGLE_QUOTE = "''";

    /** Mensagem quando o caminho é nulo. */
    private static final String NULL_PATH_MESSAGE = "path must not be null";

    /**
     * Classe utilitária: não deve ser instanciada.
     */
    private TrashCommands() {
    }

    /**
     * Monta o comando que envia um arquivo à lixeira.
     *
     * @param file caminho do arquivo
     * @return programa e argumentos
     */
    public static List<String> sendFileToTrash(Path file) {
        return powerShell(DELETE_FILE_METHOD, file);
    }

    /**
     * Monta o comando que envia uma pasta, com tudo que há dentro, à lixeira.
     *
     * @param directory caminho da pasta
     * @return programa e argumentos
     */
    public static List<String> sendDirectoryToTrash(Path directory) {
        return powerShell(DELETE_DIRECTORY_METHOD, directory);
    }

    /**
     * Monta a chamada do PowerShell para o método informado.
     *
     * @param method método do {@code FileSystem} do Visual Basic
     * @param path   caminho do item
     * @return programa e argumentos
     */
    private static List<String> powerShell(String method, Path path) {
        Objects.requireNonNull(path, NULL_PATH_MESSAGE);
        String escaped = path.toString().replace(SINGLE_QUOTE, ESCAPED_SINGLE_QUOTE);
        String script = SCRIPT_TEMPLATE.formatted(method, escaped);
        return List.of(POWERSHELL, NO_PROFILE_OPTION, NON_INTERACTIVE_OPTION, WINDOW_STYLE_OPTION, HIDDEN,
                EXECUTION_POLICY_OPTION, BYPASS, COMMAND_OPTION, script);
    }
}
