package com.tidyfy.application.fake;

import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agendador de tarefas em memória, para os testes dos casos de uso.
 * Guarda as tarefas registradas (uma por ajudante), as chamadas feitas e permite simular falha
 * com {@link #failNextRegisterWith(RuntimeException)}.
 */
public final class FakeTaskScheduler implements TaskScheduler {

    /** Tarefas atualmente registradas, por ajudante. */
    private final Map<HelperId, Helper> tasks = new LinkedHashMap<>();

    /** Todas as chamadas a {@link #register(Helper)} que tiveram sucesso, em ordem. */
    private final List<Helper> registerCalls = new ArrayList<>();

    /** Todas as chamadas a {@link #unregister(HelperId)}, em ordem. */
    private final List<HelperId> unregisterCalls = new ArrayList<>();

    /** Falha a lançar na próxima remoção; {@code null} quando não há. */
    private RuntimeException unregisterFailure;

    /** Falha a lançar na próxima listagem de ids; {@code null} quando não há. */
    private RuntimeException registeredIdsFailure;

    /** Falha a lançar no próximo registro; {@code null} quando não há. */
    private RuntimeException registerFailure;

    /**
     * Faz o próximo {@link #register(Helper)} lançar a falha informada (uma vez só).
     * A chamada que falha não altera as tarefas nem entra em {@link #registerCalls()}.
     *
     * @param failure falha a lançar
     */
    public void failNextRegisterWith(RuntimeException failure) {
        this.registerFailure = failure;
    }

    /**
     * Informa se o ajudante tem tarefa registrada.
     *
     * @param id identificador do ajudante
     * @return {@code true} se houver tarefa
     */
    public boolean isRegistered(HelperId id) {
        return tasks.containsKey(id);
    }

    /**
     * Ids dos ajudantes com tarefa registrada agora, na ordem do primeiro registro.
     *
     * @return ids registrados
     */
    public List<HelperId> registered() {
        return List.copyOf(tasks.keySet());
    }

    /**
     * Ajudante como foi enviado no último registro da tarefa dele.
     *
     * @param id identificador do ajudante
     * @return o ajudante registrado, ou {@code null} se não houver tarefa
     */
    public Helper registeredHelper(HelperId id) {
        return tasks.get(id);
    }

    /**
     * Todas as chamadas bem-sucedidas a {@link #register(Helper)}, em ordem (inclui atualizações).
     *
     * @return ajudantes enviados ao agendador
     */
    public List<Helper> registerCalls() {
        return List.copyOf(registerCalls);
    }

    /**
     * Todas as chamadas a {@link #unregister(HelperId)}, em ordem (inclui ids sem tarefa).
     *
     * @return ids enviados para remoção
     */
    public List<HelperId> unregisterCalls() {
        return List.copyOf(unregisterCalls);
    }

    @Override
    public void register(Helper helper) {
        if (registerFailure != null) {
            RuntimeException failure = registerFailure;
            registerFailure = null;
            throw failure;
        }
        tasks.put(helper.id(), helper);
        registerCalls.add(helper);
    }

    @Override
    public void unregister(HelperId id) {
        unregisterCalls.add(id);
        if (unregisterFailure != null) {
            RuntimeException failure = unregisterFailure;
            unregisterFailure = null;
            throw failure;
        }
        tasks.remove(id);
    }

    @Override
    public List<HelperId> registeredIds() {
        if (registeredIdsFailure != null) {
            RuntimeException failure = registeredIdsFailure;
            registeredIdsFailure = null;
            throw failure;
        }
        return registered();
    }

    /**
     * Faz o próximo {@link #unregister(HelperId)} lançar a falha informada (uma vez só).
     *
     * @param failure falha a lançar
     */
    public void failNextUnregisterWith(RuntimeException failure) {
        this.unregisterFailure = failure;
    }

    /**
     * Faz o próximo {@link #registeredIds()} lançar a falha informada (uma vez só).
     *
     * @param failure falha a lançar
     */
    public void failNextRegisteredIdsWith(RuntimeException failure) {
        this.registeredIdsFailure = failure;
    }

    /**
     * Cria uma tarefa para um id sem ajudante salvo (órfã), como se tivesse sobrado no agendador.
     *
     * @param helper ajudante usado como conteúdo da tarefa; o id dele identifica a tarefa
     */
    public void addOrphanTask(Helper helper) {
        tasks.put(helper.id(), helper);
    }
}
