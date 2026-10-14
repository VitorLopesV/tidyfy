package com.tidyfy.infrastructure.persistence;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperType;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
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
 * Se o conteúdo for inválido ou de versão desconhecida, o arquivo original nunca é alterado: uma cópia
 * de segurança ({@code helpers.txt.bak-yyyyMMdd-HHmmss}) é guardada uma única vez por conteúdo e a
 * {@link CorruptedStorageException} sobe, inclusive nas operações que gravam.
 * As operações que alteram (ler, alterar e gravar) rodam sob uma trava exclusiva entre threads e processos
 * ({@link FileLocks}); as leituras não travam, pois a gravação atômica garante um arquivo sempre completo.
 */
public final class JsonHelperRepository implements HelperRepository {

    /** Local de armazenamento, que fornece o caminho do arquivo e cria a pasta. */
    private final StorageLocation location;

    /** Conversor entre a lista de ajudantes e o JSON. */
    private final HelperJsonMapper mapper;

    /** Relógio usado no carimbo da cópia de segurança. */
    private final Clock clock;

    /** Trava exclusiva das operações que alteram o arquivo. */
    private final FileLocks locks;

    // Constantes

    /** Prefixo do arquivo temporário de gravação. */
    private static final String TEMP_PREFIX = "helpers-";

    /** Mensagem de erro de leitura. */
    private static final String READ_ERROR = "Não foi possível ler o arquivo %s";

    /**
     * Cria o repositório.
     *
     * @param location local de armazenamento (a pasta é criada na primeira gravação)
     * @param mapper   conversor de ajudantes para JSON e de volta
     */
    public JsonHelperRepository(StorageLocation location, HelperJsonMapper mapper) {
        this(location, mapper, Clock.systemDefaultZone());
    }

    /**
     * Cria o repositório com um relógio específico (útil nos testes).
     *
     * @param location local de armazenamento (a pasta é criada na primeira gravação)
     * @param mapper   conversor de ajudantes para JSON e de volta
     * @param clock    relógio do carimbo da cópia de segurança
     */
    public JsonHelperRepository(StorageLocation location, HelperJsonMapper mapper, Clock clock) {
        this(location, mapper, clock, FileLocks.DEFAULT_TIMEOUT);
    }

    /**
     * Cria o repositório com relógio e espera máxima pela trava específicos (útil nos testes).
     *
     * @param location    local de armazenamento
     * @param mapper      conversor de ajudantes para JSON e de volta
     * @param clock       relógio do carimbo da cópia de segurança
     * @param lockTimeout espera máxima pela trava exclusiva
     */
    JsonHelperRepository(StorageLocation location, HelperJsonMapper mapper, Clock clock, Duration lockTimeout) {
        this.location = location;
        this.mapper = mapper;
        this.clock = clock;
        this.locks = new FileLocks(lockTimeout);
    }

    /**
     * Grava o ajudante: substitui o de mesmo id mantendo a posição, ou inclui no fim.
     *
     * @param helper ajudante a gravar
     */
    @Override
    public void save(Helper helper) {
        locks.withLock(lockTarget(), () -> {
            List<Helper> helpers = load();
            int index = indexOf(helpers, helper.id());
            if (index >= 0) {
                helpers.set(index, helper);
            } else {
                helpers.add(helper);
            }
            store(helpers);
        });
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
     * Remove o ajudante. Id inexistente não altera nada, nem o arquivo;
     * sem arquivo, nem a pasta nem a trava são criadas.
     *
     * @param id identificador do ajudante
     */
    @Override
    public void delete(HelperId id) {
        if (!Files.isRegularFile(location.helpersFile())) {
            return;
        }
        locks.withLock(lockTarget(), () -> {
            List<Helper> helpers = load();
            if (helpers.removeIf(helper -> helper.id().equals(id))) {
                store(helpers);
            }
        });
    }

    /**
     * Arquivo protegido pela trava; garante que a pasta exista, pois a trava fica ao lado dele.
     *
     * @return o caminho de {@code helpers.txt}
     */
    private Path lockTarget() {
        location.directory();
        return location.helpersFile();
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
     * @throws CorruptedStorageException se o conteúdo for inválido ou de versão desconhecida
     */
    private List<Helper> load() {
        Path file = location.helpersFile();
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            try {
                return new ArrayList<>(mapper.fromJson(content));
            } catch (HelperJsonException e) {
                CorruptedStorageException corrupted = new CorruptedStorageException(file, e);
                StorageFiles.backUp(file, clock, corrupted);
                throw corrupted;
            }
        } catch (IOException e) {
            throw new UncheckedIOException(String.format(READ_ERROR, file), e);
        }
    }

    /**
     * Grava a lista de forma atômica (veja {@link StorageFiles#writeAtomically}).
     *
     * @param helpers ajudantes a gravar
     * @throws UncheckedIOException se a gravação falhar
     */
    private void store(List<Helper> helpers) {
        StorageFiles.writeAtomically(location.directory(), location.helpersFile(), TEMP_PREFIX,
                mapper.toJson(helpers));
    }
}
