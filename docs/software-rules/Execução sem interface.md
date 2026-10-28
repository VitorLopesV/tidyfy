---
tags: [agendamento]
---
# Execução sem interface

No horário, o [[Agendador do Windows]] abre o Tidyfy sem interface gráfica, informando qual [[Ajudante]] rodar. O programa carrega a configuração, executa, registra no [[Histórico de execuções]] e encerra.

## Código de saída
- 0: sucesso (inclusive ajudante inexistente ou desligado).
- 1: falha (exceção ou ação com falha).
- 2: argumentos inválidos.

## Falhas
Uma falha (arquivo sem permissão, pasta apagada) entra no [[Histórico de execuções]] com a mensagem e no log, e resulta em código 1.
