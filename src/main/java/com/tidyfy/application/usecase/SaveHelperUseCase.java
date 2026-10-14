package com.tidyfy.application.usecase;

import com.tidyfy.application.port.HelperRepository;
import com.tidyfy.application.port.TaskScheduler;
import com.tidyfy.domain.Helper;
import com.tidyfy.domain.HelperRules;
import java.util.List;
import java.util.Objects;

/**
 * Cria ou edita um ajudante. Confere o limite por tipo (só na criação) e as regras do domínio,
 * sincroniza o agendador (registra se ligado, remove se desligado) e só então grava a configuração.
 * Se o agendador falhar, a configuração não é gravada e a exceção é propagada para a tela.
 */
public final class SaveHelperUseCase {

    /** Repositório de ajudantes. */
    private final HelperRepository repository;

    /** Agendador de tarefas do sistema operacional. */
    private final TaskScheduler scheduler;

    /** Regras de nome, pasta e sobreposição. */
    private final HelperRules rules;

    // Constantes

    /** Nome do parâmetro do repositório, usado na mensagem de valor nulo. */
    private static final String REPOSITORY_NAME = "repository";

    /** Nome do parâmetro do agendador, usado na mensagem de valor nulo. */
    private static final String SCHEDULER_NAME = "scheduler";

    /** Nome do parâmetro das regras, usado na mensagem de valor nulo. */
    private static final String RULES_NAME = "rules";

    /** Nome do parâmetro do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_NAME = "helper";

    /**
     * Cria o caso de uso.
     *
     * @param repository repositório de ajudantes
     * @param scheduler  agendador de tarefas
     * @param rules      regras do domínio para salvar um ajudante
     * @throws NullPointerException se algum argumento for nulo
     */
    public SaveHelperUseCase(HelperRepository repository, TaskScheduler scheduler, HelperRules rules) {
        this.repository = Objects.requireNonNull(repository, REPOSITORY_NAME);
        this.scheduler = Objects.requireNonNull(scheduler, SCHEDULER_NAME);
        this.rules = Objects.requireNonNull(rules, RULES_NAME);
    }

    /**
     * Salva o ajudante (criação ou edição, conforme o id já exista no repositório).
     * A ordem é: limite por tipo (só criação), regras do domínio, agendador e, por fim, repositório.
     *
     * @param helper ajudante a salvar
     * @throws NullPointerException              se {@code helper} for nulo
     * @throws com.tidyfy.domain.DomainException se alguma regra do domínio for violada
     * @throws java.io.UncheckedIOException      se o agendador ou o repositório falharem
     */
    public void execute(Helper helper) {
        Objects.requireNonNull(helper, HELPER_NAME);
        boolean isNew = repository.findById(helper.id()).isEmpty();
        if (isNew) {
            helper.type().assertCanCreate(repository.countByType(helper.type()));
        }
        List<Helper> others = repository.findAll().stream()
                .filter(other -> !other.id().equals(helper.id()))
                .toList();
        rules.assertCanSave(helper, others);
        if (helper.enabled()) {
            scheduler.register(helper);
        } else {
            scheduler.unregister(helper.id());
        }
        repository.save(helper);
    }
}
