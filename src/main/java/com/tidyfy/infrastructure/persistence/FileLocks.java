package com.tidyfy.infrastructure.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Exclusão mútua das operações que alteram os arquivos JSON, entre threads e entre processos.
 * <p>
 * Dois níveis, sempre nesta ordem: um {@link ReentrantLock} por arquivo de trava, compartilhado por
 * todas as instâncias do processo (um {@link FileLock} não protege duas threads do mesmo processo,
 * pois a JVM lança {@link OverlappingFileLockException}); e um {@link FileLock} exclusivo sobre o arquivo
 * de trava, que protege contra outros processos (a interface e a execução agendada).
 * <p>
 * O arquivo de trava ({@code <arquivo>.lock}, na mesma pasta) guarda só o bloqueio, nunca dados, e
 * permanece na pasta depois do uso: apagá-lo abriria uma corrida entre processos. A espera tem limite;
 * esgotado, a operação falha com {@link UncheckedIOException} em vez de travar para sempre.
 */
final class FileLocks {

    // Constantes

    /** Espera máxima padrão pela trava. */
    static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /** Sufixo do nome do arquivo de trava, depois do nome do arquivo protegido. */
    private static final String LOCK_SUFFIX = ".lock";

    /** Intervalo, em milissegundos, entre as tentativas de obter a trava. */
    private static final long RETRY_MILLIS = 10;

    /** Mensagem de erro quando a trava não é obtida a tempo. */
    private static final String TIMEOUT_ERROR =
            "Não foi possível obter acesso exclusivo ao arquivo %s em %d ms: outro processo o está usando";

    /** Mensagem de erro ao abrir o arquivo de trava. */
    private static final String OPEN_ERROR = "Não foi possível abrir o arquivo de trava %s";

    /** Mensagem de erro quando a espera é interrompida. */
    private static final String INTERRUPTED_ERROR = "Espera interrompida pelo acesso exclusivo ao arquivo %s";

    /** Travas das threads deste processo, uma por arquivo de trava. */
    private static final Map<Path, ReentrantLock> THREAD_LOCKS = new ConcurrentHashMap<>();

    /** Espera máxima pela trava. */
    private final Duration timeout;

    /**
     * Cria o controle com a espera máxima informada.
     *
     * @param timeout tempo máximo de espera pela trava
     */
    FileLocks(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * Executa a ação com acesso exclusivo ao arquivo (ler, alterar e gravar sem intercalar com outros).
     *
     * @param file   arquivo protegido; o arquivo de trava fica ao lado dele (a pasta já deve existir)
     * @param action ação a executar com a trava
     * @throws UncheckedIOException se a trava não for obtida dentro do limite ou o arquivo de trava
     *                              não puder ser aberto
     */
    void withLock(Path file, Runnable action) {
        Path lockFile = file.toAbsolutePath().normalize().resolveSibling(file.getFileName() + LOCK_SUFFIX);
        long deadline = System.nanoTime() + timeout.toNanos();
        ReentrantLock threadLock = THREAD_LOCKS.computeIfAbsent(lockFile, key -> new ReentrantLock());
        try {
            if (!threadLock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                throw timeoutError(file);
            }
            try {
                runWithFileLock(file, lockFile, deadline, action);
            } finally {
                threadLock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(String.format(INTERRUPTED_ERROR, file), new IOException(e));
        }
    }

    /**
     * Obtém o {@link FileLock} exclusivo, tentando de novo até o prazo, e executa a ação.
     *
     * @param file     arquivo protegido (para as mensagens)
     * @param lockFile arquivo de trava
     * @param deadline prazo, em {@link System#nanoTime()}
     * @param action   ação a executar
     * @throws InterruptedException se a espera for interrompida
     */
    private void runWithFileLock(Path file, Path lockFile, long deadline, Runnable action)
            throws InterruptedException {
        try (FileChannel channel = FileChannel.open(lockFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                FileLock ignored = acquire(channel, file, deadline)) {
            action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(OPEN_ERROR, lockFile), e);
        }
    }

    /**
     * Tenta obter a trava exclusiva até o prazo. A trava já mantida (por outro processo ou por outro
     * canal deste processo) conta como ocupada.
     *
     * @param channel  canal do arquivo de trava
     * @param file     arquivo protegido (para a mensagem)
     * @param deadline prazo, em {@link System#nanoTime()}
     * @return a trava obtida
     * @throws IOException          se o sistema de arquivos falhar
     * @throws InterruptedException se a espera for interrompida
     */
    private FileLock acquire(FileChannel channel, Path file, long deadline) throws IOException, InterruptedException {
        while (true) {
            try {
                FileLock lock = channel.tryLock();
                if (lock != null) {
                    return lock;
                }
            } catch (OverlappingFileLockException busy) {
                // Mantida por outro canal deste processo: trata como ocupada e tenta de novo
            }
            if (System.nanoTime() - deadline >= 0) {
                throw timeoutError(file);
            }
            Thread.sleep(RETRY_MILLIS);
        }
    }

    /**
     * Monta o erro de espera esgotada.
     *
     * @param file arquivo protegido
     * @return o erro a lançar
     */
    private UncheckedIOException timeoutError(Path file) {
        return new UncheckedIOException(String.format(TIMEOUT_ERROR, file, timeout.toMillis()),
                new IOException(String.valueOf(file)));
    }
}
