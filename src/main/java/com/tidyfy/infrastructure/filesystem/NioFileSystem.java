package com.tidyfy.infrastructure.filesystem;

import com.tidyfy.application.port.FileSystem;
import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import com.tidyfy.infrastructure.windows.CommandResult;
import com.tidyfy.infrastructure.windows.CommandRunner;
import com.tidyfy.infrastructure.windows.ProcessCommandRunner;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Implementação da porta {@link FileSystem} com {@code java.nio.file}.
 */
public final class NioFileSystem implements FileSystem {

    /** Executor dos comandos externos (PowerShell) usados para enviar à lixeira. */
    private final CommandRunner commandRunner;

    // Constantes

    /** Mensagem quando o item a enviar à lixeira não existe. */
    private static final String TRASH_NOT_FOUND = "Não é possível enviar à lixeira, o caminho não existe: %s";

    /** Mensagem quando o comando que envia à lixeira falha: caminho, código de saída e saída do comando. */
    private static final String TRASH_FAILED =
            "Não foi possível enviar à lixeira: %s (código de saída %d). Caminhos com mais de 260 caracteres não são aceitos. Saída: %s";

    /** Mensagem quando o executor de comandos é nulo. */
    private static final String NULL_RUNNER_MESSAGE = "commandRunner must not be null";

    /** Mensagem de quando o caminho comparado não é um arquivo comum. */
    private static final String NOT_A_FILE = "Não é um arquivo: %s";

    /** Retorno de {@link Files#mismatch} quando os conteúdos são idênticos. */
    private static final long NO_MISMATCH = -1L;

    /** Ordem estável da listagem: pelo nome do item, sem diferenciar maiúsculas de minúsculas. */
    private static final Comparator<FolderEntry> BY_NAME =
            Comparator.comparing((FolderEntry entry) -> entry.path().getFileName().toString().toLowerCase())
                    .thenComparing(entry -> entry.path().getFileName().toString());

    /**
     * Cria o sistema de arquivos usando processos reais para enviar à lixeira.
     */
    public NioFileSystem() {
        this(new ProcessCommandRunner());
    }

    /**
     * Cria o sistema de arquivos com o executor de comandos informado.
     *
     * @param commandRunner executor dos comandos externos (PowerShell) usados para enviar à lixeira
     */
    public NioFileSystem(CommandRunner commandRunner) {
        this.commandRunner = Objects.requireNonNull(commandRunner, NULL_RUNNER_MESSAGE);
    }

    /**
     * Lista o que está diretamente na pasta, sem entrar nas subpastas.
     * Links simbólicos não são seguidos: um link é tratado como arquivo, com as datas do próprio link.
     * As entradas vêm ordenadas pelo nome, sem diferenciar maiúsculas de minúsculas.
     *
     * @param folder pasta a listar
     * @return entradas da pasta; vazia se ela estiver vazia
     * @throws UncheckedIOException se a pasta não existir, não for uma pasta ou não puder ser lida
     */
    @Override
    public List<FolderEntry> listEntries(Path folder) {
        List<FolderEntry> entries = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder)) {
            for (Path path : stream) {
                entries.add(toEntry(path));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        entries.sort(BY_NAME);
        return entries;
    }

    /**
     * Lê os atributos do item, sem seguir links simbólicos, e monta a entrada.
     */
    private FolderEntry toEntry(Path path) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        return new FolderEntry(path, attributes.isDirectory(),
                attributes.creationTime().toInstant(), attributes.lastModifiedTime().toInstant());
    }

    /**
     * Informa se o caminho existe, sem seguir links simbólicos: um link quebrado conta como existente.
     *
     * @param path caminho a verificar
     * @return {@code true} se existir
     */
    @Override
    public boolean exists(Path path) {
        return Files.exists(path, LinkOption.NOFOLLOW_LINKS);
    }

    /**
     * Compara o conteúdo de dois arquivos. Tamanhos diferentes já resultam em {@code false}, sem ler o conteúdo.
     *
     * @param first primeiro arquivo
     * @param second segundo arquivo
     * @return {@code true} se os conteúdos forem idênticos
     * @throws UncheckedIOException se algum caminho não existir, não for um arquivo ou não puder ser lido
     */
    @Override
    public boolean sameContent(Path first, Path second) {
        try {
            requireRegularFile(first);
            requireRegularFile(second);
            if (Files.size(first) != Files.size(second)) {
                return false;
            }
            return Files.mismatch(first, second) == NO_MISMATCH;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Garante que o caminho existe e é um arquivo comum.
     */
    private void requireRegularFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            throw new NoSuchFileException(path.toString());
        }
        if (!Files.isRegularFile(path)) {
            throw new IOException(String.format(NOT_A_FILE, path));
        }
    }

    /**
     * Move o arquivo ou a pasta (com todo o conteúdo) para o destino, criando a pasta de destino se preciso.
     * Nunca sobrescreve. Entre discos diferentes, o arquivo é copiado e a origem removida; uma pasta é copiada
     * recursivamente e só depois a origem é apagada, então uma falha no meio pode deixar uma cópia parcial no destino.
     *
     * @param move origem e destino
     * @throws UncheckedIOException se a origem não existir, o destino já existir ou a operação falhar
     */
    @Override
    public void move(FileMove move) {
        Path source = move.source();
        Path target = move.target();
        try {
            if (!exists(source)) {
                throw new NoSuchFileException(source.toString());
            }
            if (exists(target)) {
                throw new FileAlreadyExistsException(target.toString());
            }
            Path parent = target.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            moveWithoutOverwrite(source, target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Tenta o movimento atômico; se o sistema não o suportar (outro disco), copia e remove a origem.
     */
    private void moveWithoutOverwrite(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            if (Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
                copyFolder(source, target);
                deleteFolder(source);
            } else {
                Files.move(source, target);
            }
        }
    }

    /**
     * Copia a pasta e tudo o que há dentro dela para o destino.
     */
    private void copyFolder(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            /** Cria a pasta correspondente no destino antes de visitar o conteúdo dela. */
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectory(target.resolve(source.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            /** Copia o arquivo para o destino, sem sobrescrever. */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()),
                        StandardCopyOption.COPY_ATTRIBUTES, LinkOption.NOFOLLOW_LINKS);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Apaga a pasta e tudo o que há dentro dela.
     */
    private void deleteFolder(Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            /** Apaga o arquivo. */
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            /** Apaga a pasta depois de esvaziada. */
            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) {
                    throw error;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Envia o arquivo ou a pasta (com o conteúdo) para a Lixeira do Windows, de onde dá para recuperar,
     * por um PowerShell sem interface (veja {@link TrashCommands}). Links simbólicos não são seguidos.
     * Caminhos com mais de 260 caracteres falham.
     *
     * @param path caminho a enviar
     * @throws UncheckedIOException se o caminho não existir ou o comando falhar
     */
    @Override
    public void moveToTrash(Path path) {
        if (!exists(path)) {
            throw new UncheckedIOException(new NoSuchFileException(path.toString(), null,
                    String.format(TRASH_NOT_FOUND, path)));
        }
        List<String> command = Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)
                ? TrashCommands.sendDirectoryToTrash(path)
                : TrashCommands.sendFileToTrash(path);
        CommandResult result = commandRunner.run(command);
        if (!result.succeeded()) {
            throw new UncheckedIOException(new IOException(
                    String.format(TRASH_FAILED, path, result.exitCode(), result.output().strip())));
        }
    }
}
