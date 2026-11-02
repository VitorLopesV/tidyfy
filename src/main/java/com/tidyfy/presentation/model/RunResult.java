package com.tidyfy.presentation.model;

/**
 * Resultado de uma execução manual pronto para ser exibido ao usuário.
 *
 * @param success {@code true} quando a execução terminou sem falhas
 * @param title   título curto do resultado
 * @param message explicação do que aconteceu
 * @param details detalhe das falhas, uma por linha; {@code null} quando a execução teve sucesso
 */
public record RunResult(boolean success, String title, String message, String details) {
}
