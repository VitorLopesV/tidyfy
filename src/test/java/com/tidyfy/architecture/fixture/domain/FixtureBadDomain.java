package com.tidyfy.architecture.fixture.domain;

import com.tidyfy.architecture.fixture.infrastructure.FixtureAdapter;

/** Domínio sintético que viola a regra ao depender da infraestrutura. */
public class FixtureBadDomain {

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
