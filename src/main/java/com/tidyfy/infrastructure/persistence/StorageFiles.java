package com.tidyfy.infrastructure.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

/**
 * Operações de arquivo compartilhadas pelos repositórios JSON: gravação atômica e cópia de
 * segurança de arquivo corrompido.
 */
final class StorageFiles {

    // Constantes

    /** Infixo do nome da cópia de segurança, depois do nome do arquivo. */
    private static final String BACKUP_INFIX = ".bak-";

    /** Formato do carimbo da cópia de segurança. */
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** Mensagem de erro ao guardar a cópia de segurança. */
    private static final String BACKUP_ERROR = "Não foi possível guardar a cópia de segurança de %s";

    /** Sufixo do arquivo temporário de gravação. */
    private static final String TEMP_SUFFIX = ".tmp";

    /** Mensagem de erro de gravação. */
    private static final String WRITE_ERROR = "Não foi possível gravar o arquivo %s";

    /**
     * Classe utilitária: não deve ser instanciada.
     */
    private StorageFiles() {
    }

    /**
     * Grava o conteúdo de forma atômica: arquivo temporário na mesma pasta e depois move sobre o original.
     * Se o sistema de arquivos não suportar movimento atômico, usa mover substituindo o destino.
     * O temporário é sempre removido em caso de falha.
     *
     * @param directory  pasta do arquivo (já existente)
     * @param file       arquivo final
     * @param tempPrefix prefixo do arquivo temporário
     * @param content    conteúdo a gravar, em UTF-8
     * @throws UncheckedIOException se a gravação falhar
     */
    static void writeAtomically(Path directory, Path file, String tempPrefix, String content) {
        Path temp = null;
        try {
            temp = Files.createTempFile(directory, tempPrefix, TEMP_SUFFIX);
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            move(temp, file);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(WRITE_ERROR, file), e);
        } finally {
            deleteQuietly(temp);
        }
    }

    /**
     * Guarda uma cópia do arquivo problemático ao lado dele ({@code <nome>.bak-yyyyMMdd-HHmmss}),
     * a menos que já exista uma com conteúdo idêntico. Falha ao copiar não esconde o erro principal:
     * fica anexada a ele como suprimida.
     *
     * @param file      arquivo problemático
     * @param clock     relógio do carimbo
     * @param corrupted erro que será lançado
     */
    static void backUp(Path file, Clock clock, CorruptedStorageException corrupted) {
        try {
            if (hasIdenticalBackup(file)) {
                return;
            }
            String stamp = LocalDateTime.now(clock).format(BACKUP_STAMP);
            Path backup = file.resolveSibling(file.getFileName() + BACKUP_INFIX + stamp);
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            corrupted.addSuppressed(new UncheckedIOException(String.format(BACKUP_ERROR, file), e));
        }
    }

    /**
     * Indica se já existe, na mesma pasta, uma cópia de segurança com o mesmo conteúdo.
     *
     * @param file arquivo problemático
     * @return {@code true} se houver cópia idêntica
     * @throws IOException se a pasta ou os arquivos não puderem ser lidos
     */
    private static boolean hasIdenticalBackup(Path file) throws IOException {
        String prefix = file.getFileName() + BACKUP_INFIX;
        try (Stream<Path> siblings = Files.list(file.getParent())) {
            for (Path candidate : siblings.toList()) {
                if (candidate.getFileName().toString().startsWith(prefix)
                        && Files.isRegularFile(candidate) && Files.mismatch(file, candidate) == -1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Move o temporário sobre o destino, de forma atômica quando possível.
     *
     * @param source arquivo temporário
     * @param target arquivo final
     * @throws IOException se o movimento falhar
     */
    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Apaga o arquivo se ainda existir, ignorando falhas.
     *
     * @param file arquivo a apagar (pode ser nulo)
     */
    private static void deleteQuietly(Path file) {
        if (file == null) {
            return;
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // Nada a fazer: o temporário é apenas lixo
        }
    }
}
