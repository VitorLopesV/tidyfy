package com.tidyfy.presentation.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Ajudantes marcados na lista: vários podem ser removidos juntos, mas só um é editado por vez.
 */
public final class HelperSelection {

    /** Identificadores dos ajudantes marcados, na ordem em que foram marcados. */
    private final Set<String> ids = new LinkedHashSet<>();

    // Constantes

    /** Texto para um único ajudante selecionado. */
    private static final String SUMMARY_ONE = "ajudante selecionado";

    /** Texto para vários ajudantes selecionados. */
    private static final String SUMMARY_MANY = "ajudantes selecionados";

    /** Texto do botão de remover quando há um único ajudante. */
    private static final String REMOVE_ONE = "Remover";

    /** Modelo do texto do botão de remover quando há vários ajudantes; recebe a quantidade. */
    private static final String REMOVE_MANY = "Remover %d";

    /**
     * Marca ou desmarca um ajudante.
     *
     * @param id       identificador do ajudante
     * @param selected {@code true} para marcar, {@code false} para desmarcar
     */
    public void set(String id, boolean selected) {
        if (selected) {
            ids.add(id);
        } else {
            ids.remove(id);
        }
    }

    /**
     * Substitui toda a seleção pelos identificadores informados.
     *
     * @param newIds identificadores que passam a ficar marcados
     */
    public void replaceWith(Collection<String> newIds) {
        ids.clear();
        ids.addAll(newIds);
    }

    /**
     * Desmarca todos os ajudantes.
     */
    public void clear() {
        ids.clear();
    }

    /**
     * Informa se o ajudante está marcado.
     *
     * @param id identificador do ajudante
     * @return {@code true} quando está marcado
     */
    public boolean contains(String id) {
        return ids.contains(id);
    }

    /**
     * Conta os ajudantes marcados.
     *
     * @return quantidade de ajudantes marcados
     */
    public int count() {
        return ids.size();
    }

    /**
     * Informa se não há ajudantes marcados.
     *
     * @return {@code true} quando a seleção está vazia
     */
    public boolean isEmpty() {
        return ids.isEmpty();
    }

    /**
     * Devolve os identificadores marcados, sem permitir alterá-los.
     *
     * @return visão somente leitura da seleção
     */
    public Set<String> ids() {
        return Collections.unmodifiableSet(ids);
    }

    /**
     * Informa se a edição está liberada, o que só acontece com um ajudante marcado.
     *
     * @return {@code true} quando há exatamente um ajudante marcado
     */
    public boolean canEdit() {
        return ids.size() == 1;
    }

    /**
     * Informa se a remoção está liberada, o que acontece com um ou mais ajudantes marcados.
     *
     * @return {@code true} quando há pelo menos um ajudante marcado
     */
    public boolean canRemove() {
        return !ids.isEmpty();
    }

    /**
     * Devolve o ajudante que pode ser editado.
     *
     * @return o identificador do único ajudante marcado, ou vazio quando a edição não está liberada
     */
    public Optional<String> editableId() {
        return canEdit() ? Optional.of(ids.iterator().next()) : Optional.empty();
    }

    /**
     * Devolve o texto que acompanha a quantidade de selecionados.
     *
     * @return o texto no singular ou no plural
     */
    public String summary() {
        return ids.size() == 1 ? SUMMARY_ONE : SUMMARY_MANY;
    }

    /**
     * Devolve o texto do botão de remover.
     *
     * @return "Remover", ou "Remover N" quando há mais de um ajudante marcado
     */
    public String removeLabel() {
        return ids.size() > 1 ? String.format(REMOVE_MANY, ids.size()) : REMOVE_ONE;
    }
}
