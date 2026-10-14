package com.tidyfy.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tidyfy.application.fake.FakeHelperRepository;
import com.tidyfy.domain.FolderOrganizerHelper;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import com.tidyfy.domain.HelperName;
import com.tidyfy.domain.Schedule;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testes do {@link ListHelpersUseCase}, com o fake em memória do repositório.
 */
class ListHelpersUseCaseTest {

    private static final Schedule SCHEDULE =
            new Schedule(1, List.of(LocalTime.of(9, 0)), LocalDate.of(2026, 1, 1));

    private FakeHelperRepository repository;
    private ListHelpersUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new FakeHelperRepository();
        useCase = new ListHelpersUseCase(repository);
    }

    private Helper helper(String name) {
        return new FolderOrganizerHelper(
                HelperId.newId(), new HelperName(name), SCHEDULE, true, Path.of("C:\\" + name));
    }

    @Test
    void returnsTheRepositoryHelpersInCreationOrder() {
        Helper a = helper("A");
        Helper b = helper("B");
        repository.with(a).with(b);

        assertEquals(List.of(a, b), useCase.execute());
    }

    @Test
    void returnsEmptyListWhenThereAreNoHelpers() {
        assertTrue(useCase.execute().isEmpty());
    }

    @Test
    void returnedListIsImmutable() {
        repository.with(helper("A"));

        List<Helper> result = useCase.execute();

        assertThrows(UnsupportedOperationException.class, () -> result.add(helper("B")));
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new ListHelpersUseCase(null));
    }
}
