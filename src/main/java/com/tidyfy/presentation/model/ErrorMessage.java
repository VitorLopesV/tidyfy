package com.tidyfy.presentation.model;

/**
 * Erro pronto para ser exibido ao usuário.
 *
 * @param title   título curto do problema
 * @param message explicação clara do que aconteceu e do que o usuário pode fazer
 * @param details detalhe técnico (tipo e mensagem da exceção), mostrado só sob demanda
 */
public record ErrorMessage(String title, String message, String details) {
}
