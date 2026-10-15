package com.tidyfy.architecture.fixture.infrastructure;

import com.tidyfy.architecture.fixture.presentation.FixtureBadPresentation;

/** Infraestrutura sintética que viola a regra ao depender da apresentação (e fecha um ciclo). */
public class FixtureBadInfrastructure {

    /** Dependência proibida */
    private final FixtureBadPresentation presentation = new FixtureBadPresentation();

    /**
     * Devolve a apresentação, só para a dependência existir.
     *
     * @return a apresentação
     */
    public FixtureBadPresentation presentation() {
        return presentation;
    }
}
