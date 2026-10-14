package com.tidyfy.infrastructure.persistence;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Repositório de ajudantes que guarda a configuração em {@code helpers.txt} (JSON).
 * <p>
 * Cada operação lê o arquivo do disco, sem cache em memória, porque a interface e a execução
 * agendada são processos diferentes. Arquivo ausente ou vazio equivale a nenhum ajudante.
 * As operações que alteram regravam o arquivo de forma atômica: o conteúdo novo é escrito num
 * arquivo temporário da mesma pasta e depois movido sobre o original, de modo que o arquivo
 * nunca fica pela metade.
 * <p>
 * Se o conteúdo for inválido, a {@link HelperJsonException} do mapeador sobe sem alterar o arquivo.
 * O controle de acesso simultâneo entre processos (locks) não é feito aqui; fica para a issue C4.
 */
public final class JsonHelperRepository implements HelperRepository {

    /** Local de armazenamento, que fornece o caminho do arquivo e cria a pasta. */
    private final StorageLocation location;

    /** Conversor entre a lista de ajudantes e o JSON. */
    private final HelperJsonMapper mapper;

    // Constantes

    /** Prefixo do arquivo temporário de gravação. */
    private static final String TEMP_PREFIX = "helpers-";

    /** Sufixo do arquivo temporário de gravação. */
    private static final String TEMP_SUFFIX = ".tmp";

    /** Mensagem de erro de leitura. */
    private static final String READ_ERROR = "Não foi possível ler o arquivo %s";

    /** Mensagem de erro de gravação. */
    private static final String WRITE_ERROR = "Não foi possível gravar o arquivo %s";

    /**
     * Cria o repositório.
     *
     * @param location local de armazenamento (a pasta é criada na primeira gravação)
     * @param mapper   conversor de ajudantes para JSON e de volta
     */
    public JsonHelperRepository(StorageLocation location, HelperJsonMapper mapper) {
        this.location = location;
        this.mapper = mapper;
    }

    /**
     * Grava o ajudante: substitui o de mesmo id mantendo a posição, ou inclui no fim.
     *
     * @param helper ajudante a gravar
     */
    @Override
    public void save(Helper helper) {
        List<Helper> helpers = load();
        int index = indexOf(helpers, helper.id());
        if (index >= 0) {
            helpers.set(index, helper);
        } else {
            helpers.add(helper);
        }
        store(helpers);
    }

    /**
     * Busca um ajudante pelo id.
     *
     * @param id identificador do ajudante
     * @return o ajudante, ou vazio se não existir
     */
    @Override
    public Optional<Helper> findById(HelperId id) {
        return load().stream().filter(helper -> helper.id().equals(id)).findFirst();
    }

    /**
     * Lista todos os ajudantes na ordem de criação.
     *
     * @return lista de ajudantes; vazia se não houver nenhum
     */
    @Override
    public List<Helper> findAll() {
        return load();
    }

    /**
     * Conta os ajudantes de um tipo.
     *
     * @param type tipo a contar
     * @return quantidade de ajudantes do tipo
     */
    @Override
    public int countByType(HelperType type) {
        return (int) load().stream().filter(helper -> helper.type() == type).count();
    }

    /**
     * Remove o ajudante. Id inexistente não altera nada, nem o arquivo.
     *
     * @param id identificador do ajudante
     */
    @Override
    public void delete(HelperId id) {
        List<Helper> helpers = load();
        if (helpers.removeIf(helper -> helper.id().equals(id))) {
            store(helpers);
        }
    }

    /**
     * Posição do ajudante na lista.
     *
     * @param helpers ajudantes carregados
     * @param id      identificador procurado
     * @return a posição, ou -1 se não existir
     */
    private int indexOf(List<Helper> helpers, HelperId id) {
        for (int i = 0; i < helpers.size(); i++) {
            if (helpers.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Lê o arquivo. Arquivo ausente (ou que não seja arquivo comum) devolve lista vazia.
     *
     * @return lista mutável de ajudantes
     * @throws UncheckedIOException se a leitura falhar
     * @throws HelperJsonException  se o conteúdo for inválido
     */
    private List<Helper> load() {
        Path file = location.helpersFile();
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(mapper.fromJson(Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(READ_ERROR, file), e);
        }
    }

    /**
     * Grava a lista de forma atômica: arquivo temporário na mesma pasta e depois move sobre o original.
     * Se o sistema de arquivos não suportar movimento atômico, usa mover substituindo o destino.
     * O temporário é sempre removido em caso de falha.
     *
     * @param helpers ajudantes a gravar
     * @throws UncheckedIOException se a gravação falhar
     */
    private void store(List<Helper> helpers) {
        Path file = location.helpersFile();
        Path temp = null;
        try {
            temp = Files.createTempFile(location.directory(), TEMP_PREFIX, TEMP_SUFFIX);
            Files.writeString(temp, mapper.toJson(helpers), StandardCharsets.UTF_8);
            move(temp, file);
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(WRITE_ERROR, file), e);
        } finally {
            deleteQuietly(temp);
        }
    }

    /**
     * Move o temporário sobre o destino, de forma atômica quando possível.
     *
     * @param source arquivo temporário
     * @param target arquivo final
     * @throws IOException se o movimento falhar
     */
    private void move(Path source, Path target) throws IOException {
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
    private void deleteQuietly(Path file) {
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
