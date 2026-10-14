package com.tidyfy.domain;

import java.util.List;

/**
 * Regras que um ajudante precisa cumprir, em relação aos demais, para ser salvo.
 */
public final class HelperRules {

    /**
     * Confere se o ajudante pode ser salvo.
     * Hoje recusa apenas o nome repetido; maiúsculas e minúsculas não diferenciam
     * ({@code Organizar Downloads} e {@code organizar downloads} são o mesmo nome).
     *
     * @param helper o ajudante a salvar
     * @param others os demais ajudantes; na edição, não inclui o próprio ajudante
     * @throws HelperNameAlreadyUsedException se outro ajudante já tem o mesmo nome
     */
    public void assertCanSave(Helper helper, List<Helper> others) {
        assertNameIsUnique(helper, others);
    }

    /**
     * Recusa o ajudante cujo nome já é de algum dos demais, ignorando maiúsculas e minúsculas.
     *
     * @param helper o ajudante a salvar
     * @param others os demais ajudantes
     * @throws HelperNameAlreadyUsedException se o nome já estiver em uso
     */
    private void assertNameIsUnique(Helper helper, List<Helper> others) {
        String name = helper.name().value();
        for (Helper other : others) {
            if (other.name().value().equalsIgnoreCase(name)) {
                throw new HelperNameAlreadyUsedException(helper.name());
            }
        }
    }
}
