---
tags: [persistencia]
---
# Histórico de execuções

Registro de cada execução de um [[Ajudante]]: quando rodou e o que fez (arquivos movidos, removidos, lixeira esvaziada, falhas). É exibido na interface, um diálogo por ajudante.

## Regras
- Os registros são guardados por 30 dias e apagados depois.
- Falhas da [[Execução sem interface]] também entram.
- Remover um ajudante apaga a tarefa do [[Agendador do Windows]], mas não o histórico: fica até expirar.
- Além do histórico há um log em texto, um arquivo por dia, que apaga o que passa de 30 dias e para de gravar ao chegar a 1 MiB no dia. Falhas ao gravar o log nunca derrubam a execução.

Guardado em [[Armazenamento]].
