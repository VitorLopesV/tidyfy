---
tags: [persistencia]
---
# Armazenamento

Como a configuração dos [[Ajudante]]s e o [[Histórico de execuções]] são guardados em arquivos.

## Regras
- **Gravação atômica:** grava num arquivo temporário e depois move, para nunca deixar o arquivo pela metade.
- **Acesso simultâneo:** alterações (ler, alterar, gravar) rodam sob trava entre threads e entre processos (interface e execuções agendadas), com espera máxima de 10 segundos; ao esgotar, dá erro. Leituras não travam.
- **Arquivo corrompido ou versão desconhecida:** o original fica intacto, é feita uma cópia de segurança e o erro aparece; nunca é sobrescrito.
- **Versão:** cada arquivo tem número de versão; mudança incompatível no formato incrementa.
- Arquivo ausente ou vazio significa nenhum ajudante ou histórico vazio.
