package com.tidyfy.infrastructure.windows;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Executa processos externos com {@link ProcessBuilder}.
 * A saída de erro é unida à saída padrão e lida inteira. Se o processo não terminar no tempo
 * limite, ele é destruído. Falhas de execução (processo inexistente, interrupção, timeout)
 * são sempre {@link UncheckedIOException}, com mensagem que inclui o comando.
 */
public final class ProcessCommandRunner implements CommandRunner {

    /** Tempo máximo de espera pelo término do processo. */
    private final Duration timeout;

    /** Codificação usada para decodificar a saída do processo. */
    private final Charset charset;

    /** Inicia o processo de um comando. */
    private final ProcessStarter starter;

    // Constantes

    /** Tempo limite padrão. */
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    /** Propriedade do sistema com a codificação do console (Java 19+). */
    private static final String STDOUT_ENCODING_PROPERTY = "stdout.encoding";

    /** Propriedade do sistema com a codificação nativa do sistema operacional. */
    private static final String NATIVE_ENCODING_PROPERTY = "native.encoding";

    /** Mensagem de timeout nulo ou não positivo. */
    private static final String INVALID_TIMEOUT_MESSAGE = "timeout must be positive";

    /** Mensagem de comando vazio. */
    private static final String EMPTY_COMMAND_MESSAGE = "command must not be empty";

    /** Modelo da mensagem de falha ao iniciar o processo. */
    private static final String START_FAILURE_MESSAGE = "Could not start command: %s";

    /** Modelo da mensagem de timeout. */
    private static final String TIMEOUT_MESSAGE = "Command timed out after %d ms: %s";

    /** Modelo da mensagem de falha ao ler a saída. */
    private static final String READ_FAILURE_MESSAGE = "Could not read output of command: %s";

    /** Modelo da mensagem de interrupção. */
    private static final String INTERRUPTED_MESSAGE = "Interrupted while running command: %s";

    /**
     * Cria um executor com o tempo limite padrão e a codificação do console.
     */
    public ProcessCommandRunner() {
        this(DEFAULT_TIMEOUT, consoleCharset());
    }

    /**
     * Cria um executor com tempo limite configurado e a codificação do console.
     *
     * @param timeout tempo máximo de espera (positivo)
     */
    public ProcessCommandRunner(Duration timeout) {
        this(timeout, consoleCharset());
    }

    /**
     * Cria um executor totalmente configurado.
     *
     * @param timeout tempo máximo de espera (positivo)
     * @param charset codificação usada para ler a saída
     */
    public ProcessCommandRunner(Duration timeout, Charset charset) {
        this(timeout, charset, ProcessCommandRunner::startWithProcessBuilder);
    }

    /**
     * Cria um executor com o iniciador de processos informado (ponto de injeção para testes).
     *
     * @param timeout tempo máximo de espera (positivo)
     * @param charset codificação usada para ler a saída
     * @param starter inicia o processo do comando
     */
    ProcessCommandRunner(Duration timeout, Charset charset, ProcessStarter starter) {
        Objects.requireNonNull(timeout, INVALID_TIMEOUT_MESSAGE);
        this.starter = Objects.requireNonNull(starter);
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(INVALID_TIMEOUT_MESSAGE);
        }
        this.timeout = timeout;
        this.charset = Objects.requireNonNull(charset);
    }

    /**
     * Descobre a codificação do console: {@code stdout.encoding}, depois {@code native.encoding}
     * e, por fim, a codificação padrão da JVM. Mantido simples de propósito: se a JVM não tiver
     * console anexado, {@code stdout.encoding} já cai para a codificação nativa do sistema.
     *
     * @return a codificação a usar quando nenhuma é informada
     */
    static Charset consoleCharset() {
        for (String property : List.of(STDOUT_ENCODING_PROPERTY, NATIVE_ENCODING_PROPERTY)) {
            String name = System.getProperty(property);
            if (name != null && Charset.isSupported(name)) {
                return Charset.forName(name);
            }
        }
        return Charset.defaultCharset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandResult run(List<String> command) {
        Objects.requireNonNull(command);
        if (command.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_COMMAND_MESSAGE);
        }
        List<String> safeCommand = List.copyOf(command);
        Process process = start(safeCommand);
        // Lê em outra thread: um processo mudo e travado bloquearia a leitura além do timeout.
        CompletableFuture<String> output = CompletableFuture.supplyAsync(() -> readAll(process.getInputStream()));
        try {
            if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                process.destroyForcibly();
                throw new UncheckedIOException(new IOException(
                        String.format(TIMEOUT_MESSAGE, timeout.toMillis(), safeCommand)));
            }
            return new CommandResult(process.exitValue(), output.get());
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new IOException(String.format(INTERRUPTED_MESSAGE, safeCommand), e));
        } catch (ExecutionException e) {
            throw new UncheckedIOException(
                    new IOException(String.format(READ_FAILURE_MESSAGE, safeCommand), e.getCause()));
        }
    }

    /**
     * Inicia o processo real com a saída de erro unida à saída padrão.
     */
    private static Process startWithProcessBuilder(List<String> command) throws IOException {
        return new ProcessBuilder(command).redirectErrorStream(true).start();
    }

    /**
     * Inicia o processo, convertendo falhas de início em {@link UncheckedIOException}.
     */
    private Process start(List<String> command) {
        try {
            return starter.start(command);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(START_FAILURE_MESSAGE, command), e);
        }
    }

    /**
     * Lê o fluxo inteiro com a codificação configurada.
     */
    private String readAll(InputStream stream) {
        try (stream) {
            return new String(stream.readAllBytes(), charset);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
