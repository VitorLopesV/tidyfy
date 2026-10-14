package com.tidyfy.infrastructure.logging;

import com.tidyfy.domain.HelperId;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

/**
 * Log das execuções dos ajudantes, usado principalmente no modo sem interface (agendado).
 * <p>
 * Grava linhas de texto UTF-8 em {@code <pasta de logs>/tidyfy-yyyy-MM-dd.log}, um arquivo por dia.
 * Cada linha tem o instante ISO-8601, o nível, o ajudante e a mensagem. Falhas incluem o tipo da
 * exceção, a mensagem e as primeiras linhas da pilha.
 * <p>
 * O log não cresce sem limite: ao gravar, apaga os arquivos de log com mais de
 * {@value #RETENTION_DAYS} dias (pela data no nome do arquivo) e, quando o arquivo do dia chega a
 * {@value #MAX_FILE_BYTES} bytes, deixa de anexar e registra uma única vez que o limite foi atingido
 * (sem rotação, por simplicidade).
 * <p>
 * O log nunca derruba a execução: qualquer falha de E/S ao gravar é engolida.
 */
public final class ExecutionLog {

    /** Pasta onde ficam os arquivos de log. */
    private final Path logsDirectory;

    /** Relógio que define o instante das linhas, o arquivo do dia e a retenção. */
    private final Clock clock;

    // Constantes

    /** Quantidade de dias que um arquivo de log é mantido. */
    public static final int RETENTION_DAYS = 30;

    /** Tamanho máximo, em bytes, de um arquivo diário (1 MiB). */
    public static final long MAX_FILE_BYTES = 1024L * 1024L;

    /** Quantidade de linhas da pilha gravadas em uma falha. */
    public static final int STACK_LINES = 5;

    /** Prefixo do nome dos arquivos de log. */
    private static final String FILE_PREFIX = "tidyfy-";

    /** Sufixo (extensão) do nome dos arquivos de log. */
    private static final String FILE_SUFFIX = ".log";

    /** Formato da data no nome do arquivo. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Nível das linhas informativas. */
    private static final String LEVEL_INFO = "INFO";

    /** Nível das linhas de erro. */
    private static final String LEVEL_ERROR = "ERROR";

    /** Modelo de uma linha: instante, nível, origem e mensagem. */
    private static final String LINE_FORMAT = "%s %s [%s] %s";

    /** Origem das linhas que não pertencem a um ajudante. */
    private static final String NO_HELPER = "-";

    /** Separador de linhas do arquivo. */
    private static final String NEWLINE = "\n";

    /** Expressão que separa as linhas da pilha. */
    private static final String LINE_BREAK_REGEX = "\\R";

    /** Prefixo das linhas de continuação (pilha). */
    private static final String CONTINUATION_PREFIX = "\t";

    /** Mensagem de início de execução. */
    private static final String STARTED_MESSAGE = "Execução iniciada";

    /** Modelo da mensagem de fim de execução. */
    private static final String FINISHED_MESSAGE = "Execução concluída: %d ação(ões)";

    /** Mensagem de falha de execução. */
    private static final String FAILED_MESSAGE = "Execução falhou";

    /** Modelo da mensagem de erro com a causa: descrição, tipo e mensagem da exceção. */
    private static final String ERROR_WITH_CAUSE = "%s: %s: %s";

    /** Mensagem registrada uma única vez quando o arquivo do dia atinge o limite. */
    private static final String LIMIT_MESSAGE = "limite do log do dia atingido";

    /**
     * Cria o log.
     *
     * @param logsDirectory pasta dos logs (criada ao gravar, se faltar)
     * @param clock         relógio usado para datar as linhas e aplicar a retenção
     */
    public ExecutionLog(Path logsDirectory, Clock clock) {
        this.logsDirectory = logsDirectory;
        this.clock = clock;
    }

    /**
     * Registra o início da execução de um ajudante.
     *
     * @param id o ajudante
     */
    public void started(HelperId id) {
        write(LEVEL_INFO, id.value().toString(), STARTED_MESSAGE);
    }

    /**
     * Registra o fim, com sucesso, da execução de um ajudante.
     *
     * @param id      o ajudante
     * @param actions quantidade de ações realizadas
     */
    public void finished(HelperId id, int actions) {
        write(LEVEL_INFO, id.value().toString(), String.format(FINISHED_MESSAGE, actions));
    }

    /**
     * Registra a falha da execução de um ajudante, com a causa.
     *
     * @param id    o ajudante
     * @param error a causa da falha
     */
    public void failed(HelperId id, Throwable error) {
        write(LEVEL_ERROR, id.value().toString(), describe(FAILED_MESSAGE, error));
    }

    /**
     * Registra um erro que não pertence a um ajudante (por exemplo, argumentos inválidos).
     *
     * @param message descrição do erro
     * @param error   a causa
     */
    public void error(String message, Throwable error) {
        write(LEVEL_ERROR, NO_HELPER, describe(message, error));
    }

    /**
     * Monta a mensagem de erro com o tipo, a mensagem e a pilha resumida da exceção.
     */
    private String describe(String message, Throwable error) {
        StringWriter stack = new StringWriter();
        error.printStackTrace(new PrintWriter(stack));
        StringBuilder text = new StringBuilder(String.format(
                ERROR_WITH_CAUSE, message, error.getClass().getName(), error.getMessage()));
        String[] lines = stack.toString().split(LINE_BREAK_REGEX);
        for (int i = 1; i < lines.length && i <= STACK_LINES; i++) {
            text.append(NEWLINE).append(CONTINUATION_PREFIX).append(lines[i].strip());
        }
        return text.toString();
    }

    /**
     * Grava uma linha no arquivo do dia, respeitando a retenção e o limite de tamanho.
     * Qualquer falha de E/S é engolida para não interromper a execução do ajudante.
     */
    private void write(String level, String source, String message) {
        try {
            Files.createDirectories(logsDirectory);
            deleteExpiredFiles();
            Path file = logsDirectory.resolve(FILE_PREFIX + DATE_FORMAT.format(LocalDate.now(clock)) + FILE_SUFFIX);
            String line = String.format(LINE_FORMAT, clock.instant(), level, source, message) + NEWLINE;
            long size = Files.exists(file) ? Files.size(file) : 0;
            if (size + line.getBytes(StandardCharsets.UTF_8).length <= MAX_FILE_BYTES) {
                append(file, line);
            } else if (size <= MAX_FILE_BYTES) {
                append(file, String.format(LINE_FORMAT, clock.instant(), LEVEL_ERROR, NO_HELPER, LIMIT_MESSAGE)
                        + NEWLINE);
            }
        } catch (IOException | RuntimeException e) {
            // O log nunca pode derrubar a execução: a falha ao gravar é ignorada de propósito.
        }
    }

    /** Anexa o texto ao arquivo, criando-o se necessário. */
    private void append(Path file, String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * Apaga os arquivos {@code tidyfy-*.log} cuja data (no nome) tem mais de {@value #RETENTION_DAYS}
     * dias. Arquivos com outro nome ou com data ilegível nunca são apagados.
     */
    private void deleteExpiredFiles() throws IOException {
        LocalDate limit = LocalDate.now(clock).minusDays(RETENTION_DAYS);
        try (Stream<Path> files = Files.list(logsDirectory)) {
            files.filter(file -> isOlderThan(file, limit)).forEach(this::deleteQuietly);
        }
    }

    /** Indica se o arquivo é um log com data anterior ao limite. */
    private boolean isOlderThan(Path file, LocalDate limit) {
        String name = file.getFileName().toString();
        if (!name.startsWith(FILE_PREFIX) || !name.endsWith(FILE_SUFFIX)
                || name.length() < FILE_PREFIX.length() + FILE_SUFFIX.length()) {
            return false;
        }
        String date = name.substring(FILE_PREFIX.length(), name.length() - FILE_SUFFIX.length());
        try {
            return LocalDate.parse(date, DATE_FORMAT).isBefore(limit);
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** Apaga o arquivo; se não for possível, mantém e segue. */
    private void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Arquivo preso por outro processo: será tentado de novo na próxima gravação.
        }
    }
}
