package com.tidyfy.application.usecase;

import com.tidyfy.application.port.DesktopIcons;
import com.tidyfy.application.port.DesktopState;
import com.tidyfy.domain.ActionType;
import com.tidyfy.domain.DesktopArrangerHelper;
import com.tidyfy.domain.DesktopIcon;
import com.tidyfy.domain.DesktopLayoutPlan;
import com.tidyfy.domain.DesktopLayoutPlanner;
import com.tidyfy.domain.HelperAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Reposiciona os ícones da área de trabalho: lê os ícones pela porta, pede ao planejador as novas posições
 * do layout do ajudante e as grava. Devolve uma ação {@link ActionType#ARRANGED} com a quantidade de ícones
 * posicionados e, se algum não couber na área visível, uma ação {@link ActionType#FAILED} com o nome deles.
 * Falhas da porta (por exemplo, "Organizar ícones automaticamente" ligado) são propagadas ao chamador, que
 * as registra como falha da execução.
 */
public final class ArrangeDesktopUseCase {

    /** Porta de acesso à área de trabalho. */
    private final DesktopIcons desktopIcons;

    /** Planejador que calcula as posições. */
    private final DesktopLayoutPlanner planner;

    // Constantes

    /** Nome do parâmetro da porta, usado na mensagem de valor nulo. */
    private static final String DESKTOP_ICONS_NAME = "desktopIcons";

    /** Nome do parâmetro do planejador, usado na mensagem de valor nulo. */
    private static final String PLANNER_NAME = "planner";

    /** Nome do parâmetro do ajudante, usado na mensagem de valor nulo. */
    private static final String HELPER_NAME = "helper";

    /** Mensagem da ação quando há um único ícone. */
    private static final String ONE_ICON = "1 ícone";

    /** Mensagem da ação com a quantidade de ícones. */
    private static final String MANY_ICONS = "%d ícones";

    /** Mensagem da falha dos ícones que não couberam. */
    private static final String LEFT_OUT_MESSAGE = "%d ícone(s) não couberam na área visível: %s";

    /** Separador dos nomes dos ícones que não couberam. */
    private static final String NAME_SEPARATOR = ", ";

    /**
     * Cria o caso de uso.
     *
     * @param desktopIcons porta da área de trabalho
     * @param planner      planejador dos layouts
     * @throws NullPointerException se algum parâmetro for nulo
     */
    public ArrangeDesktopUseCase(DesktopIcons desktopIcons, DesktopLayoutPlanner planner) {
        this.desktopIcons = Objects.requireNonNull(desktopIcons, DESKTOP_ICONS_NAME);
        this.planner = Objects.requireNonNull(planner, PLANNER_NAME);
    }

    /**
     * Reposiciona os ícones conforme o layout do ajudante.
     *
     * @param helper ajudante do Organizador da área de trabalho
     * @return as ações realizadas: {@code ARRANGED} e, se houver ícones sem lugar, {@code FAILED}
     * @throws NullPointerException         se {@code helper} for nulo
     * @throws java.io.UncheckedIOException se a área de trabalho não puder ser lida ou gravada
     */
    public List<HelperAction> execute(DesktopArrangerHelper helper) {
        Objects.requireNonNull(helper, HELPER_NAME);
        DesktopState state = desktopIcons.read();
        DesktopLayoutPlan plan = planner.plan(state.icons(), state.grid(), helper.layout());
        desktopIcons.move(plan.placements());
        List<HelperAction> actions = new ArrayList<>();
        int count = plan.placements().size();
        actions.add(new HelperAction(ActionType.ARRANGED, null, null,
                count == 1 ? ONE_ICON : String.format(MANY_ICONS, count)));
        if (!plan.leftOut().isEmpty()) {
            String names = plan.leftOut().stream().map(DesktopIcon::name).collect(Collectors.joining(NAME_SEPARATOR));
            actions.add(new HelperAction(ActionType.FAILED, null, null,
                    String.format(LEFT_OUT_MESSAGE, plan.leftOut().size(), names)));
        }
        return actions;
    }
}
