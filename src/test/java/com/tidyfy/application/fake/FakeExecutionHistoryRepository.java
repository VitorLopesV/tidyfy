package com.tidyfy.application.fake;

import com.tidyfy.application.port.ExecutionHistoryRepository;
import com.tidyfy.domain.ExecutionRecord;
import com.tidyfy.domain.HelperId;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Histórico de execuções em memória, para os testes dos casos de uso.
 * Permite simular falha ao registrar com {@link #failNextAddWith(RuntimeException)}.
 */
public final class FakeExecutionHistoryRepository implements ExecutionHistoryRepository {

    /** Registros guardados, na ordem em que foram adicionados. */
    private final List<ExecutionRecord> records = new ArrayList<>();

    /** Falha a lançar no próximo registro; {@code null} quando não há. */
    private RuntimeException addFailure;

    /**
     * Faz o próximo {@link #add(ExecutionRecord)} lançar a falha informada (uma vez só).
     *
     * @param failure falha a lançar
     */
    public void failNextAddWith(RuntimeException failure) {
        this.addFailure = failure;
    }

    /**
     * Todos os registros guardados, na ordem em que foram adicionados.
     *
     * @return cópia da lista de registros
     */
    public List<ExecutionRecord> all() {
        return List.copyOf(records);
    }

    @Override
    public void add(ExecutionRecord record) {
        if (addFailure != null) {
            RuntimeException failure = addFailure;
            addFailure = null;
            throw failure;
        }
        records.add(record);
    }

    @Override
    public List<ExecutionRecord> findByHelper(HelperId id) {
        return records.stream()
                .filter(r -> r.helperId().equals(id))
                .sorted(Comparator.comparing(ExecutionRecord::executedAt).reversed())
                .toList();
    }

    @Override
    public void deleteExpired(Instant now) {
        records.removeIf(r -> r.isExpired(now));
    }
}
