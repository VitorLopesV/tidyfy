package com.tidyfy.application.fake;

import com.tidyfy.application.port.RecycleBin;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Lixeira em memória, para os testes dos casos de uso.
 * Cada item é identificado só pelo instante em que foi enviado para a lixeira.
 */
public final class FakeRecycleBin implements RecycleBin {

    /** Instantes de envio dos itens que ainda estão na lixeira. */
    private final List<Instant> items = new ArrayList<>();

    /** Instantes de corte recebidos em {@link #emptyOlderThan(Instant)}, em ordem. */
    private final List<Instant> cutoffs = new ArrayList<>();

    /** Total de itens removidos permanentemente por todas as chamadas. */
    private int removedCount;

    /** Quantidade de chamadas a {@link #emptyAll()}. */
    private int emptyAllCalls;

    /** Falha a lançar na próxima limpeza; {@code null} quando não há. */
    private RuntimeException failure;

    /**
     * Coloca um item na lixeira, enviado no instante informado.
     *
     * @param sentToBinAt instante em que o item foi para a lixeira
     * @return este fake, para encadear
     */
    public FakeRecycleBin addItem(Instant sentToBinAt) {
        items.add(sentToBinAt);
        return this;
    }

    /**
     * Faz a próxima limpeza (qualquer uma das duas) lançar a falha informada (uma vez só).
     *
     * @param failure falha a lançar
     */
    public void failNextWith(RuntimeException failure) {
        this.failure = failure;
    }

    /**
     * Quantidade de itens que ainda estão na lixeira.
     *
     * @return itens restantes
     */
    public int size() {
        return items.size();
    }

    /**
     * Total de itens removidos por todas as limpezas feitas.
     *
     * @return itens removidos
     */
    public int removedCount() {
        return removedCount;
    }

    /**
     * Quantas vezes {@link #emptyAll()} foi chamado (inclui chamadas com a lixeira vazia).
     *
     * @return número de chamadas
     */
    public int emptyAllCalls() {
        return emptyAllCalls;
    }

    /**
     * Instantes de corte recebidos em {@link #emptyOlderThan(Instant)}, em ordem.
     *
     * @return cortes recebidos
     */
    public List<Instant> cutoffs() {
        return List.copyOf(cutoffs);
    }

    @Override
    public int emptyAll() {
        throwPendingFailure();
        emptyAllCalls++;
        int removed = items.size();
        items.clear();
        removedCount += removed;
        return removed;
    }

    @Override
    public int emptyOlderThan(Instant cutoff) {
        throwPendingFailure();
        cutoffs.add(cutoff);
        int before = items.size();
        items.removeIf(sentAt -> sentAt.isBefore(cutoff));
        int removed = before - items.size();
        removedCount += removed;
        return removed;
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
}
