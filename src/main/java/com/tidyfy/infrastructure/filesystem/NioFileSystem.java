package com.tidyfy.infrastructure.filesystem;

import com.tidyfy.application.port.FileSystem;
import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementação da porta {@link FileSystem} com {@code java.nio.file}.
 */
public final class NioFileSystem implements FileSystem {

    // Constantes

    /** Mensagem das operações que ainda não foram implementadas. */
    private static final String NOT_IMPLEMENTED = "Operação ainda não implementada";

    /** Ordem estável da listagem: pelo nome do item, sem diferenciar maiúsculas de minúsculas. */
    private static final Comparator<FolderEntry> BY_NAME =
            Comparator.comparing((FolderEntry entry) -> entry.path().getFileName().toString().toLowerCase())
                    .thenComparing(entry -> entry.path().getFileName().toString());

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
     * Ainda não implementado.
     */
    @Override
    public boolean exists(Path path) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * Ainda não implementado.
     */
    @Override
    public boolean sameContent(Path first, Path second) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * Ainda não implementado.
     */
    @Override
    public void move(FileMove move) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }

    /**
     * Ainda não implementado.
     */
    @Override
    public void moveToTrash(Path path) {
        throw new UnsupportedOperationException(NOT_IMPLEMENTED);
    }
}
