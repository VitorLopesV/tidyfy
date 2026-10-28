package com.tidyfy.infrastructure.windows;

import com.tidyfy.application.port.DesktopIcons;
import com.tidyfy.application.port.DesktopState;
import com.tidyfy.domain.DesktopGrid;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.IconPosition;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Área de trabalho do Windows lida e gravada pelo {@code powershell.exe}, sem janela.
 * O script {@code desktop-icons.ps1} (recurso desta classe) fala com a lista de ícones do Explorer
 * ({@code SysListView32}, dentro de {@code SHELLDLL_DefView}): as mensagens {@code LVM_GETITEMPOSITION},
 * {@code LVM_GETITEMTEXTW} e {@code LVM_SETITEMPOSITION} são enviadas ao Explorer e a memória dele é lida e
 * escrita por {@code ReadProcessMemory}/{@code WriteProcessMemory}. O processo se declara sensível ao DPI,
 * senão a escala de tela distorce a área visível. O comando é entregue como lista de argumentos
 * ({@code -File script -Mode list|move}); o script e o arquivo com as posições vão para arquivos temporários
 * em {@code tempDirectory}, apagados ao final.
 * Saída da leitura (ASCII): {@code AUTOARRANGE}, {@code SNAPTOGRID}, {@code AREA=left,top,width,height},
 * {@code CELL=width,height} e uma linha {@code ICON<TAB>nome em Base64 (UTF-8)<TAB>x<TAB>y} por ícone; o nome
 * vai em Base64 porque o executor decodifica a saída com o charset do console. A gravação lê um arquivo UTF-8
 * com linhas {@code nome<TAB>x<TAB>y}; em caso de erro o próprio script restaura as posições já gravadas.
 * Com "Organizar ícones automaticamente" ligado a gravação é recusada com uma mensagem clara (o Tidyfy não
 * muda essa opção do usuário). Código de saída diferente de zero ou saída inesperada viram
 * {@link UncheckedIOException}. Verificado no Windows 11 (veja o CLAUDE.md); o script real não roda em testes.
 */
public final class PowerShellDesktopIcons implements DesktopIcons {

    /** Executa o PowerShell. */
    private final CommandRunner commandRunner;

    /** Pasta (existente) onde o script e o arquivo de posições temporários são gravados. */
    private final Path tempDirectory;

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

    /** Opção que recebe o arquivo do script. */
    private static final String FILE_OPTION = "-File";

    /** Parâmetro do script que escolhe a operação. */
    private static final String MODE_PARAMETER = "-Mode";

    /** Parâmetro do script que recebe o arquivo de posições. */
    private static final String MOVES_FILE_PARAMETER = "-MovesFile";

    /** Operação que lista os ícones. */
    private static final String LIST_MODE = "list";

    /** Operação que grava posições. */
    private static final String MOVE_MODE = "move";

    /** Nome do recurso com o script, ao lado desta classe. */
    private static final String SCRIPT_RESOURCE = "desktop-icons.ps1";

    /** Prefixo do script temporário. */
    private static final String SCRIPT_PREFIX = "tidyfy-desktop-";

    /** Sufixo do script temporário. */
    private static final String SCRIPT_SUFFIX = ".ps1";

    /** Prefixo do arquivo temporário de posições. */
    private static final String MOVES_PREFIX = "tidyfy-moves-";

    /** Sufixo do arquivo temporário de posições. */
    private static final String MOVES_SUFFIX = ".tsv";

    /** Separador de linhas aceito na saída. */
    private static final String LINE_SEPARATOR_REGEX = "\\R";

    /** Separador de campos, no arquivo de posições e na saída. */
    private static final String TAB = "\t";

    /** Quebra de linha do arquivo de posições. */
    private static final String NEW_LINE = "\n";

    /** Retorno de carro, que também quebra linha no arquivo de posições. */
    private static final String CARRIAGE_RETURN = "\r";

    /** Separador de números nas linhas {@code AREA} e {@code CELL}. */
    private static final String COMMA = ",";

    /** Prefixo da linha que informa se "Organizar ícones automaticamente" está ligado. */
    private static final String AUTO_ARRANGE_PREFIX = "AUTOARRANGE=";

    /** Prefixo da linha que informa se "Alinhar ícones à grade" está ligado. */
    private static final String SNAP_TO_GRID_PREFIX = "SNAPTOGRID=";

    /** Prefixo da linha da área visível. */
    private static final String AREA_PREFIX = "AREA=";

    /** Prefixo da linha da célula. */
    private static final String CELL_PREFIX = "CELL=";

    /** Prefixo da linha de um ícone. */
    private static final String ICON_PREFIX = "ICON" + TAB;

    /** Quantidade de números das linhas {@code AREA}. */
    private static final int AREA_PARTS = 4;

    /** Quantidade de números das linhas {@code CELL}. */
    private static final int CELL_PARTS = 2;

    /** Quantidade de campos de uma linha {@code ICON}. */
    private static final int ICON_PARTS = 4;

    /** Valor que o script escreve para uma opção ligada. */
    private static final String TRUE_VALUE = "true";

    /** Valor que o script escreve para uma opção desligada. */
    private static final String FALSE_VALUE = "false";

    /** Nome da operação de ler, para a mensagem de falha. */
    private static final String READ_OPERATION = "read";

    /** Nome da operação de gravar, para a mensagem de falha. */
    private static final String MOVE_OPERATION = "move";

    /** Mensagem de falha do comando: operação, código de saída e saída. */
    private static final String COMMAND_FAILURE_MESSAGE =
            "PowerShell failed to %s the desktop icons (exit code %d): %s";

    /** Mensagem quando a saída da leitura não pôde ser entendida: motivo e saída recebida. */
    private static final String UNEXPECTED_OUTPUT_MESSAGE =
            "Unexpected PowerShell output when reading the desktop icons (%s): %s";

    /** Motivo: falta uma linha obrigatória; o argumento é o prefixo dela. */
    private static final String MISSING_LINE_REASON = "missing %s";

    /** Motivo: linha mal formada; o argumento é a linha. */
    private static final String MALFORMED_LINE_REASON = "malformed line '%s'";

    /** Mensagem quando o script não é encontrado no jar. */
    private static final String SCRIPT_NOT_FOUND_MESSAGE = "Script resource not found: %s";

    /** Mensagem quando o nome do ícone não cabe no arquivo de posições. */
    private static final String INVALID_ICON_NAME_MESSAGE = "icon name must not contain tabs or line breaks: %s";

    /** Mensagem quando o executor é nulo. */
    private static final String NULL_RUNNER_MESSAGE = "commandRunner must not be null";

    /** Mensagem quando a pasta temporária é nula. */
    private static final String NULL_TEMP_DIRECTORY_MESSAGE = "tempDirectory must not be null";

    /** Mensagem quando a lista de posições é nula. */
    private static final String NULL_PLACEMENTS_MESSAGE = "placements must not be null";

    /**
     * Cria a área de trabalho.
     *
     * @param commandRunner executor dos comandos do PowerShell
     * @param tempDirectory pasta (existente) onde o script e o arquivo de posições temporários são gravados
     * @throws NullPointerException se algum argumento for nulo
     */
    public PowerShellDesktopIcons(CommandRunner commandRunner, Path tempDirectory) {
        this.commandRunner = Objects.requireNonNull(commandRunner, NULL_RUNNER_MESSAGE);
        this.tempDirectory = Objects.requireNonNull(tempDirectory, NULL_TEMP_DIRECTORY_MESSAGE);
    }

    /**
     * Lê os ícones, a área visível e a grade do monitor principal.
     *
     * @return o retrato atual da área de trabalho
     * @throws UncheckedIOException se o PowerShell falhar ou devolver uma saída inesperada
     */
    @Override
    public DesktopState read() {
        Path script = extractScript();
        try {
            return parse(execute(listCommand(script), READ_OPERATION));
        } finally {
            deleteQuietly(script);
        }
    }

    /**
     * Grava as novas posições; se algum ícone falhar, o script devolve os já movidos ao lugar original.
     *
     * @param placements ícones (pelo nome) com a posição desejada; lista vazia não faz nada
     * @throws NullPointerException     se {@code placements} for nulo
     * @throws IllegalArgumentException se algum nome tiver tabulação ou quebra de linha
     * @throws UncheckedIOException     se o PowerShell falhar
     */
    @Override
    public void move(List<DesktopIcon> placements) {
        Objects.requireNonNull(placements, NULL_PLACEMENTS_MESSAGE);
        if (placements.isEmpty()) {
            return;
        }
        String content = movesFileContent(placements);
        Path script = extractScript();
        Path moves = null;
        try {
            moves = Files.createTempFile(tempDirectory, MOVES_PREFIX, MOVES_SUFFIX);
            Files.writeString(moves, content, StandardCharsets.UTF_8);
            execute(moveCommand(script, moves), MOVE_OPERATION);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            deleteQuietly(script);
            if (moves != null) {
                deleteQuietly(moves);
            }
        }
    }

    /**
     * Monta o comando que lista os ícones.
     *
     * @param script caminho do script
     * @return programa e argumentos
     */
    List<String> listCommand(Path script) {
        return List.of(POWERSHELL, NO_PROFILE_OPTION, NON_INTERACTIVE_OPTION, WINDOW_STYLE_OPTION, HIDDEN,
                EXECUTION_POLICY_OPTION, BYPASS, FILE_OPTION, script.toString(), MODE_PARAMETER, LIST_MODE);
    }

    /**
     * Monta o comando que grava as posições.
     *
     * @param script caminho do script
     * @param moves  caminho do arquivo com as posições
     * @return programa e argumentos
     */
    List<String> moveCommand(Path script, Path moves) {
        return List.of(POWERSHELL, NO_PROFILE_OPTION, NON_INTERACTIVE_OPTION, WINDOW_STYLE_OPTION, HIDDEN,
                EXECUTION_POLICY_OPTION, BYPASS, FILE_OPTION, script.toString(), MODE_PARAMETER, MOVE_MODE,
                MOVES_FILE_PARAMETER, moves.toString());
    }

    /**
     * Monta o conteúdo do arquivo de posições: uma linha {@code nome<TAB>x<TAB>y} por ícone.
     *
     * @param placements ícones com a posição desejada
     * @return o conteúdo do arquivo
     * @throws IllegalArgumentException se algum nome tiver tabulação ou quebra de linha
     */
    static String movesFileContent(List<DesktopIcon> placements) {
        StringBuilder content = new StringBuilder();
        for (DesktopIcon icon : placements) {
            if (icon.name().contains(TAB) || icon.name().contains(NEW_LINE) || icon.name().contains("\r")) {
                throw new IllegalArgumentException(INVALID_ICON_NAME_MESSAGE.formatted(icon.name()));
            }
            content.append(icon.name()).append(TAB).append(icon.position().x()).append(TAB)
                    .append(icon.position().y()).append(NEW_LINE);
        }
        return content.toString();
    }

    /**
     * Entende a saída do script de leitura.
     *
     * @param output saída do script
     * @return o retrato da área de trabalho
     * @throws UncheckedIOException se faltar alguma linha obrigatória ou houver linha mal formada
     */
    static DesktopState parse(String output) {
        Boolean autoArrange = null;
        Boolean snapToGrid = null;
        int[] area = null;
        int[] cell = null;
        List<DesktopIcon> icons = new ArrayList<>();
        for (String raw : output.split(LINE_SEPARATOR_REGEX)) {
            String line = raw.stripTrailing();
            try {
                if (line.startsWith(AUTO_ARRANGE_PREFIX)) {
                    autoArrange = parseFlag(line.substring(AUTO_ARRANGE_PREFIX.length()));
                } else if (line.startsWith(SNAP_TO_GRID_PREFIX)) {
                    snapToGrid = parseFlag(line.substring(SNAP_TO_GRID_PREFIX.length()));
                } else if (line.startsWith(AREA_PREFIX)) {
                    area = parseNumbers(line.substring(AREA_PREFIX.length()), AREA_PARTS);
                } else if (line.startsWith(CELL_PREFIX)) {
                    cell = parseNumbers(line.substring(CELL_PREFIX.length()), CELL_PARTS);
                } else if (line.startsWith(ICON_PREFIX)) {
                    icons.add(parseIcon(line));
                }
            } catch (IllegalArgumentException e) {
                throw unexpected(MALFORMED_LINE_REASON.formatted(line), output, e);
            }
        }
        if (autoArrange == null) {
            throw unexpected(MISSING_LINE_REASON.formatted(AUTO_ARRANGE_PREFIX), output, null);
        }
        if (snapToGrid == null) {
            throw unexpected(MISSING_LINE_REASON.formatted(SNAP_TO_GRID_PREFIX), output, null);
        }
        if (area == null) {
            throw unexpected(MISSING_LINE_REASON.formatted(AREA_PREFIX), output, null);
        }
        if (cell == null) {
            throw unexpected(MISSING_LINE_REASON.formatted(CELL_PREFIX), output, null);
        }
        try {
            DesktopGrid grid = new DesktopGrid(area[0], area[1], area[2], area[3], cell[0], cell[1]);
            return new DesktopState(icons, grid, autoArrange, snapToGrid);
        } catch (IllegalArgumentException e) {
            throw unexpected(MALFORMED_LINE_REASON.formatted(AREA_PREFIX + CELL_PREFIX), output, e);
        }
    }

    /**
     * Lê o valor {@code true} ou {@code false} de uma linha de opção.
     *
     * @param value texto depois do prefixo
     * @return o valor lógico
     * @throws IllegalArgumentException se não for {@code true} nem {@code false}
     */
    private static boolean parseFlag(String value) {
        if (TRUE_VALUE.equals(value)) {
            return true;
        }
        if (FALSE_VALUE.equals(value)) {
            return false;
        }
        throw new IllegalArgumentException(value);
    }

    /**
     * Lê números inteiros separados por vírgula.
     *
     * @param value    texto depois do prefixo
     * @param expected quantidade de números esperada
     * @return os números
     * @throws IllegalArgumentException se a quantidade ou algum número estiver errado
     */
    private static int[] parseNumbers(String value, int expected) {
        String[] parts = value.split(COMMA);
        if (parts.length != expected) {
            throw new IllegalArgumentException(value);
        }
        int[] numbers = new int[expected];
        for (int i = 0; i < expected; i++) {
            numbers[i] = Integer.parseInt(parts[i].strip());
        }
        return numbers;
    }

    /**
     * Lê uma linha {@code ICON<TAB>nome em Base64<TAB>x<TAB>y}.
     *
     * @param line a linha
     * @return o ícone
     * @throws IllegalArgumentException se a linha estiver mal formada
     */
    private static DesktopIcon parseIcon(String line) {
        String[] parts = line.split(TAB);
        if (parts.length != ICON_PARTS) {
            throw new IllegalArgumentException(line);
        }
        String name = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        return new DesktopIcon(name, new IconPosition(Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
    }

    /**
     * Cria a falha de saída inesperada.
     *
     * @param reason motivo
     * @param output saída recebida
     * @param cause  causa, ou nulo
     * @return a exceção a lançar
     */
    private static UncheckedIOException unexpected(String reason, String output, Throwable cause) {
        return new UncheckedIOException(new IOException(UNEXPECTED_OUTPUT_MESSAGE.formatted(reason, output), cause));
    }

    /**
     * Executa o comando e exige sucesso.
     *
     * @param command   comando a executar
     * @param operation nome da operação, para a mensagem de falha
     * @return saída do comando
     */
    private String execute(List<String> command, String operation) {
        CommandResult result = commandRunner.run(command);
        if (!result.succeeded()) {
            throw new UncheckedIOException(new IOException(
                    COMMAND_FAILURE_MESSAGE.formatted(operation, result.exitCode(), result.output().strip())));
        }
        return result.output();
    }

    /**
     * Copia o script do jar para um arquivo temporário, pois o PowerShell precisa de um arquivo.
     *
     * @return o caminho do script temporário
     * @throws UncheckedIOException se o recurso não existir ou o arquivo não puder ser gravado
     */
    private Path extractScript() {
        try (InputStream stream = PowerShellDesktopIcons.class.getResourceAsStream(SCRIPT_RESOURCE)) {
            if (stream == null) {
                throw new IOException(SCRIPT_NOT_FOUND_MESSAGE.formatted(SCRIPT_RESOURCE));
            }
            Path script = Files.createTempFile(tempDirectory, SCRIPT_PREFIX, SCRIPT_SUFFIX);
            Files.copy(stream, script, StandardCopyOption.REPLACE_EXISTING);
            return script;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Apaga um arquivo temporário; falhas são ignoradas, pois o resultado já foi decidido.
     *
     * @param file o arquivo
     */
    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // arquivo temporário: sobra, mas não derruba a operação
        }
    }
}
