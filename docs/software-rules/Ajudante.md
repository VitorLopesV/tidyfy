---
tags: [dominio]
---
# Ajudante

Tarefa de limpeza ou organização que roda periodicamente. Tem nome, tipo ([[Organização de pasta]] ou [[Limpeza]]), [[Frequência]] e estado ligado/desligado.

## Regras
- **Alvo único:** atua em uma só pasta ou, só na [[Limpeza]], na [[Lixeira]].
- **Nome obrigatório:** definido pelo usuário, sem espaços nas pontas.
- **Nome único:** sem repetir, ignorando maiúsculas e minúsculas ("Organizar Downloads" e "organizar downloads" são o mesmo).
- **Máximo de 20 por tipo:** checado só na criação; editar não conta. A interface bloqueia o tipo cheio, mas quem garante é o domínio.
- **Sem sobreposição:** dois ajudantes do mesmo tipo não atuam na mesma pasta nem em pastas aninhadas (Downloads e Downloads\Imagens). Organização e limpeza podem atuar na mesma. Só uma [[Limpeza]] pode ter a [[Lixeira]].
- **Pastas sensíveis recusadas:** veja [[Pastas sensíveis]].
- **Desligado não executa:** não fica registrado no [[Agendador do Windows]].

Cada execução vai para o [[Histórico de execuções]]; a configuração é guardada em [[Armazenamento]].
