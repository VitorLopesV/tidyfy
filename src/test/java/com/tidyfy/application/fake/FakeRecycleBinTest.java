package com.tidyfy.application.fake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link FakeRecycleBin}: garantem que ele segue o contrato da porta.
 */
class FakeRecycleBinTest {

    private static final Instant CUTOFF = Instant.parse("2026-06-10T00:00:00Z");

    @Test
    void emptyAllRemovesEverythingAndReturnsCount() {
        FakeRecycleBin bin = new FakeRecycleBin().addItem(CUTOFF.minusSeconds(1)).addItem(CUTOFF.plusSeconds(1));

        assertEquals(2, bin.emptyAll());
        assertEquals(0, bin.size());
        assertEquals(2, bin.removedCount());
        assertEquals(1, bin.emptyAllCalls());
    }

    @Test
    void emptyAllOnEmptyBinReturnsZeroButCountsCall() {
        FakeRecycleBin bin = new FakeRecycleBin();

        assertEquals(0, bin.emptyAll());
        assertEquals(1, bin.emptyAllCalls());
    }

    @Test
    void emptyOlderThanRemovesOnlyItemsSentBeforeCutoff() {
        FakeRecycleBin bin = new FakeRecycleBin()
                .addItem(CUTOFF.minusSeconds(1))
                .addItem(CUTOFF)
                .addItem(CUTOFF.plusSeconds(1));

        assertEquals(1, bin.emptyOlderThan(CUTOFF));
        assertEquals(2, bin.size());
        assertEquals(1, bin.removedCount());
        assertEquals(List.of(CUTOFF), bin.cutoffs());
    }

    @Test
    void emptyOlderThanReturnsZeroWhenNoneIsOlder() {
        FakeRecycleBin bin = new FakeRecycleBin().addItem(CUTOFF.plusSeconds(1));

        assertEquals(0, bin.emptyOlderThan(CUTOFF));
        assertEquals(1, bin.size());
    }

    @Test
    void failNextWithThrowsOnceAndKeepsItems() {
        FakeRecycleBin bin = new FakeRecycleBin().addItem(CUTOFF);
        bin.failNextWith(new IllegalStateException("falha"));

        assertThrows(IllegalStateException.class, bin::emptyAll);
        assertEquals(1, bin.size());
        assertEquals(1, bin.emptyAll());
    }
}
