package com.tidyfy.application.fake;

import com.tidyfy.application.port.FileSystem;
import com.tidyfy.domain.FileMove;
import com.tidyfy.domain.FolderEntry;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sistema de arquivos em memória, para os testes dos casos de uso.
 * É um mapa de caminho para entrada (arquivo com conteúdo, ou pasta). Pais de itens adicionados são
 * criados como pastas. Registra as movimentações e os envios à lixeira.
 */
public final class FakeFileSystem implements FileSystem {

    /** Entradas existentes, por caminho. */
    private final Map<Path, Node> nodes = new LinkedHashMap<>();

    /** Movimentações feitas por {@link #move(FileMove)}, em ordem. */
    private final List<FileMove> moves = new ArrayList<>();

    /** Caminhos enviados à lixeira por {@link #moveToTrash(Path)}, em ordem. */
    private final List<Path> trashed = new ArrayList<>();

    /** Falha a lançar na próxima operação de escrita; {@code null} quando não há. */
    private RuntimeException failure;

    // Constantes

    /** Instante usado nas pastas criadas automaticamente como pais. */
    private static final Instant DEFAULT_TIME = Instant.EPOCH;

    /** Mensagem quando a origem da operação não existe. */
    private static final String MISSING = "Não existe: %s";

    /** Mensagem quando o destino da movimentação já está ocupado. */
    private static final String OCCUPIED = "Destino já ocupado: %s";

    /** Mensagem quando o caminho esperado como pasta não é uma pasta. */
    private static final String NOT_A_FOLDER = "Não é uma pasta: %s";

    /** Mensagem quando o caminho esperado como arquivo não é um arquivo. */
    private static final String NOT_A_FILE = "Não é um arquivo: %s";

    /**
     * Adiciona um arquivo de texto, criando as pastas que o contêm.
     *
     * @param path       caminho do arquivo
     * @param content    conteúdo do arquivo
     * @param createdAt  instante de criação
     * @param modifiedAt instante da última modificação
     * @return este fake, para encadear
     */
    public FakeFileSystem addFile(Path path, String content, Instant createdAt, Instant modifiedAt) {
        addParents(path);
        nodes.put(path, new Node(false, content.getBytes(StandardCharsets.UTF_8), createdAt, modifiedAt));
        return this;
    }

    /**
     * Adiciona uma pasta (e as que a contêm), com as datas padrão {@link Instant#EPOCH}.
     *
     * @param path caminho da pasta
     * @return este fake, para encadear
     */
    public FakeFileSystem addFolder(Path path) {
        return addFolder(path, DEFAULT_TIME, DEFAULT_TIME);
    }

    /**
     * Adiciona uma pasta (e as que a contêm) com as datas informadas.
     *
     * @param path       caminho da pasta
     * @param createdAt  instante de criação
     * @param modifiedAt instante da última modificação
     * @return este fake, para encadear
     */
    public FakeFileSystem addFolder(Path path, Instant createdAt, Instant modifiedAt) {
        addParents(path);
        nodes.put(path, new Node(true, new byte[0], createdAt, modifiedAt));
        return this;
    }

    /**
     * Faz a próxima {@link #move(FileMove)} ou {@link #moveToTrash(Path)} lançar a falha (uma vez só).
     * Útil para simular arquivo sem permissão.
     *
     * @param failure falha a lançar
     */
    public void failNextWriteWith(RuntimeException failure) {
        this.failure = failure;
    }

    /**
     * Movimentações feitas com sucesso, em ordem.
     *
     * @return cópia da lista de movimentações
     */
    public List<FileMove> moves() {
        return List.copyOf(moves);
    }

    /**
     * Caminhos enviados à lixeira com sucesso, em ordem.
     *
     * @return cópia da lista de caminhos
     */
    public List<Path> trashed() {
        return List.copyOf(trashed);
    }

    /**
     * Lê o conteúdo de um arquivo como texto.
     *
     * @param path caminho do arquivo
     * @return o conteúdo
     * @throws UncheckedIOException se não existir ou não for um arquivo
     */
    public String contentOf(Path path) {
        Node node = nodes.get(path);
        if (node == null) {
            throw error(MISSING, path);
        }
        if (node.directory()) {
            throw error(NOT_A_FILE, path);
        }
        return new String(node.content(), StandardCharsets.UTF_8);
    }

    @Override
    public List<FolderEntry> listEntries(Path folder) {
        Node node = nodes.get(folder);
        if (node == null) {
            throw error(MISSING, folder);
        }
        if (!node.directory()) {
            throw error(NOT_A_FOLDER, folder);
        }
        return nodes.entrySet().stream()
                .filter(e -> folder.equals(e.getKey().getParent()))
                .map(e -> new FolderEntry(e.getKey(), e.getValue().directory(),
                        e.getValue().createdAt(), e.getValue().modifiedAt()))
                .toList();
    }

    @Override
    public boolean exists(Path path) {
        return nodes.containsKey(path);
    }

    @Override
    public boolean sameContent(Path first, Path second) {
        return Arrays.equals(fileNode(first).content(), fileNode(second).content());
    }

    @Override
    public void move(FileMove move) {
        throwPendingFailure();
        Node node = nodes.get(move.source());
        if (node == null) {
            throw error(MISSING, move.source());
        }
        if (nodes.containsKey(move.target())) {
            throw error(OCCUPIED, move.target());
        }
        addParents(move.target());
        for (Path path : descendantsAndSelf(move.source())) {
            Node moved = nodes.remove(path);
            nodes.put(move.target().resolve(move.source().relativize(path)), moved);
        }
        moves.add(move);
    }

    @Override
    public void moveToTrash(Path path) {
        throwPendingFailure();
        if (!nodes.containsKey(path)) {
            throw error(MISSING, path);
        }
        descendantsAndSelf(path).forEach(nodes::remove);
        trashed.add(path);
    }

    /**
     * Devolve o arquivo do caminho, ou lança erro se não existir ou for pasta.
     *
     * @param path caminho do arquivo
     * @return a entrada do arquivo
     */
    private Node fileNode(Path path) {
        Node node = nodes.get(path);
        if (node == null) {
            throw error(MISSING, path);
        }
        if (node.directory()) {
            throw error(NOT_A_FILE, path);
        }
        return node;
    }

    /**
     * Lista o caminho e tudo que está dentro dele.
     *
     * @param root caminho raiz
     * @return o caminho e seus descendentes
     */
    private List<Path> descendantsAndSelf(Path root) {
        return nodes.keySet().stream().filter(p -> p.startsWith(root)).toList();
    }

    /**
     * Cria como pastas os pais do caminho que ainda não existem.
     *
     * @param path caminho cujos pais devem existir
     */
    private void addParents(Path path) {
        for (Path parent = path.getParent(); parent != null; parent = parent.getParent()) {
            nodes.putIfAbsent(parent, new Node(true, new byte[0], DEFAULT_TIME, DEFAULT_TIME));
        }
    }

    /**
     * Lança a falha preparada, se houver, e a descarta.
     */
    private void throwPendingFailure() {
        if (failure != null) {
            RuntimeException pending = failure;
            failure = null;
            throw pending;
        }
    }

    /**
     * Monta o erro de E/S com a mensagem formatada.
     *
     * @param template modelo da mensagem
     * @param path     caminho citado na mensagem
     * @return o erro a lançar
     */
    private static UncheckedIOException error(String template, Path path) {
        return new UncheckedIOException(new IOException(String.format(template, path)));
    }

    /**
     * Entrada do sistema de arquivos: arquivo (com conteúdo) ou pasta.
     *
     * @param directory  se é pasta
     * @param content    conteúdo em bytes (vazio para pastas)
     * @param createdAt  instante de criação
     * @param modifiedAt instante da última modificação
     */
    private record Node(boolean directory, byte[] content, Instant createdAt, Instant modifiedAt) {
    }
}
