package com.tidyfy.infrastructure.persistence;

import com.tidyfy.application.port.ExecutionHistoryRepository;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Repositório do histórico de execuções que guarda os registros em {@code history.txt} (JSON).
 * <p>
 * Cada operação lê o arquivo do disco, sem cache em memória, porque a interface e a execução
 * agendada são processos diferentes. Arquivo ausente ou vazio equivale a nenhum registro.
 * As operações que alteram regravam o arquivo de forma atômica (arquivo temporário e depois mover).
 * <p>
 * Se o conteúdo for inválido ou de versão desconhecida, o arquivo original nunca é alterado: uma cópia
 * de segurança ({@code history.txt.bak-yyyyMMdd-HHmmss}) é guardada uma única vez por conteúdo e a
 * {@link CorruptedStorageException} sobe, inclusive nas operações que gravam.
 * As operações que alteram (ler, alterar e gravar) rodam sob uma trava exclusiva entre threads e processos
 * ({@link FileLocks}); as leituras não travam, pois a gravação atômica garante um arquivo sempre completo.
 */
public final class JsonExecutionHistoryRepository implements ExecutionHistoryRepository {

    /** Local de armazenamento, que fornece o caminho do arquivo e cria a pasta. */
    private final StorageLocation location;

    /** Conversor entre a lista de registros e o JSON. */
    private final ExecutionHistoryJsonMapper mapper;

    /** Relógio usado no carimbo da cópia de segurança. */
    private final Clock clock;

    /** Trava exclusiva das operações que alteram o arquivo. */
    private final FileLocks locks;

    // Constantes

    /** Prefixo do arquivo temporário de gravação. */
    private static final String TEMP_PREFIX = "history-";

    /** Mensagem de erro de leitura. */
    private static final String READ_ERROR = "Não foi possível ler o arquivo %s";

    /**
     * Cria o repositório.
     *
     * @param location local de armazenamento (a pasta é criada na primeira gravação)
     * @param mapper   conversor de registros para JSON e de volta
     */
    public JsonExecutionHistoryRepository(StorageLocation location, ExecutionHistoryJsonMapper mapper) {
        this(location, mapper, Clock.systemDefaultZone());
    }

    /**
     * Cria o repositório com um relógio específico (útil nos testes).
     *
     * @param location local de armazenamento (a pasta é criada na primeira gravação)
     * @param mapper   conversor de registros para JSON e de volta
     * @param clock    relógio do carimbo da cópia de segurança
     */
    public JsonExecutionHistoryRepository(StorageLocation location, ExecutionHistoryJsonMapper mapper, Clock clock) {
        this(location, mapper, clock, FileLocks.DEFAULT_TIMEOUT);
    }

    /**
     * Cria o repositório com relógio e espera máxima pela trava específicos (útil nos testes).
     *
     * @param location    local de armazenamento
     * @param mapper      conversor de registros para JSON e de volta
     * @param clock       relógio do carimbo da cópia de segurança
     * @param lockTimeout espera máxima pela trava exclusiva
     */
    JsonExecutionHistoryRepository(StorageLocation location, ExecutionHistoryJsonMapper mapper, Clock clock,
            Duration lockTimeout) {
        this.location = location;
        this.mapper = mapper;
        this.clock = clock;
        this.locks = new FileLocks(lockTimeout);
    }

    /**
     * Acrescenta o registro ao fim do histórico.
     *
     * @param record execução a registrar
     */
    @Override
    public void add(ExecutionRecord record) {
        locks.withLock(lockTarget(), () -> {
            List<ExecutionRecord> records = load();
            records.add(record);
            store(records);
        });
    }

    /**
     * Lista as execuções do ajudante, da mais recente para a mais antiga.
     *
     * @param id identificador do ajudante
     * @return execuções do ajudante; vazia se não houver nenhuma
     */
    @Override
    public List<ExecutionRecord> findByHelper(HelperId id) {
        return load().stream()
                .filter(record -> record.helperId().equals(id))
                .sorted(Comparator.comparing(ExecutionRecord::executedAt).reversed())
                .toList();
    }

    /**
     * Apaga os registros vencidos. Se nenhum venceu, o arquivo não é regravado;
     * sem arquivo, nada é criado.
     *
     * @param now instante de referência
     */
    @Override
    public void deleteExpired(Instant now) {
        if (!Files.isRegularFile(location.historyFile())) {
            return;
        }
        locks.withLock(lockTarget(), () -> {
            List<ExecutionRecord> records = load();
            if (records.removeIf(record -> record.isExpired(now))) {
                store(records);
            }
        });
    }

    /**
     * Arquivo protegido pela trava; garante que a pasta exista, pois a trava fica ao lado dele.
     *
     * @return o caminho de {@code history.txt}
     */
    private Path lockTarget() {
        location.directory();
        return location.historyFile();
    }

    /**
     * Lê o arquivo. Arquivo ausente (ou que não seja arquivo comum) devolve lista vazia.
     *
     * @return lista mutável de registros
     * @throws UncheckedIOException      se a leitura falhar
     * @throws CorruptedStorageException se o conteúdo for inválido ou de versão desconhecida
     */
    private List<ExecutionRecord> load() {
        Path file = location.historyFile();
        if (!Files.isRegularFile(file)) {
            return new ArrayList<>();
        }
        try {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            try {
                return new ArrayList<>(mapper.fromJson(content));
            } catch (HistoryJsonException e) {
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
     * @param records registros a gravar
     * @throws UncheckedIOException se a gravação falhar
     */
    private void store(List<ExecutionRecord> records) {
        StorageFiles.writeAtomically(location.directory(), location.historyFile(), TEMP_PREFIX,
                mapper.toJson(records));
    }
}
