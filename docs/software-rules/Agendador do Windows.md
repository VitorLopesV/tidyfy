---
tags: [agendamento]
---
# Agendador do Windows

Dispara os [[Ajudante]]s no horário da [[Frequência]].

## Regras
- Uma tarefa por ajudante ligado, com um gatilho por horário. Desligado não tem tarefa; remover o ajudante remove a tarefa.
- Roda só com o usuário logado (precisa das pastas e da [[Lixeira]] dele), inclusive na bateria.
- Se o computador estava desligado no horário, executa assim que possível.
- Instância única: se a anterior ainda roda, a nova é ignorada.
- **Falha ao salvar:** registra primeiro no agendador; se falhar, a configuração não é salva e o erro aparece. Ao ligar/desligar, se falhar o interruptor volta ao estado anterior.
- **Sincronização ao abrir a interface:** em segundo plano, iguala o agendador à configuração (registra os ligados, remove os desligados e as tarefas órfãs). Falhas vão para o log e nunca impedem a abertura.

O disparo executa a [[Execução sem interface]].
