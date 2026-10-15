package com.tidyfy.architecture.fixture.presentation;

import com.tidyfy.architecture.fixture.infrastructure.FixtureAdapter;

/** Apresentação sintética que viola a regra ao depender da infraestrutura. */
public class FixtureBadPresentation {

    /** Dependência proibida */
    private final FixtureAdapter adapter = new FixtureAdapter();

    /**
     * Devolve o adaptador, só para a dependência existir.
     *
     * @return o adaptador
     */
    public FixtureAdapter adapter() {
        return adapter;
    }
}
