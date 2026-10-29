# Checklist de teste manual

Roteiro para validar o Tidyfy de ponta a ponta no Windows, feito por uma pessoa. Marque cada `- [ ]` ao conferir o **Resultado esperado**. No fim, preencha a tabela «Resultado».

## Antes de começar

- [ ] Windows 10 ou 11, com a sua conta logada (o Tidyfy só roda com o usuário logado).
- [ ] JDK 21 e Maven 3.8+ (só para gerar o pacote ou rodar pelo Maven).
- [ ] **Gere o pacote:** `mvn clean package -Ppackage -DskipTests` e abra `target\dist\Tidyfy\Tidyfy.exe`. Alternativa: `mvn javafx:run`. (O Agendador chama o `Tidyfy.exe`; com `mvn javafx:run` as execuções agendadas só funcionam com o pacote gerado, então prefira o pacote nas seções 3, 11 e 12.)
- [ ] **Pasta de testes própria**, por exemplo `C:\TesteTidyfy\Entrada`. Nunca use Downloads, Documentos ou outros dados reais.
- [ ] **Arquivos de teste** nessa pasta (conteúdo qualquer): `foto.jpg`, `foto2.PNG`, `relatorio.pdf`, `planilha.xlsx`, `instalador.exe`, `pacote.zip`, `filme.mp4`, `musica.mp3`, `notas.xyz` e uma subpasta `Projeto` com um arquivo dentro.
- [ ] **Agendador de Tarefas:** abra `taskschd.msc` e, na «Biblioteca do Agendador de Tarefas», a pasta **Tidyfy**. Cada ajudante ligado é uma tarefa chamada pelo id (UUID). Aperte F5 para atualizar.
- [ ] **Arquivos do Tidyfy:** `%APPDATA%\Tidyfy\helpers.txt` (ajudantes), `history.txt` (histórico) e a pasta `logs` (`tidyfy-yyyy-MM-dd.log`). Para começar do zero, feche o Tidyfy e apague `helpers.txt` e `history.txt` (e as tarefas da pasta Tidyfy no Agendador).

> **ATENÇÃO:** a limpeza da lixeira apaga **definitivamente** (não dá para recuperar), e a limpeza de pasta envia arquivos à Lixeira. Faça os testes só com itens de teste e, de preferência, **esvazie a Lixeira antes** (botão direito na Lixeira, «Esvaziar Lixeira»), para não perder nada seu.

## 1. Abrir o aplicativo e lista vazia

1. Abra `Tidyfy.exe`.
   - Resultado esperado: janela «Tidyfy» em tema escuro com o título «Seus ajudantes».
2. Observe a tela sem ajudantes.
   - Resultado esperado: mensagem «Nenhum ajudante por aqui» e o botão «Criar primeiro ajudante»; contadores `0/20` em «Organização de pasta» e «Limpeza»; «Ligados agora» em 0.
3. Clique em «Novo ajudante».
   - Resultado esperado: diálogo «Do que esse ajudante vai cuidar?» com «Organização de pasta» e «Limpeza», cada um com «Criados 0/20».

- [ ] Seção 1 conferida

## 2. Criar um ajudante de organização

1. Em «Novo ajudante», escolha «Organização de pasta».
   - Resultado esperado: formulário com «Nome do ajudante», «Pasta a organizar», «Frequência» e a «PRÉVIA» de como a pasta vai ficar.
2. Deixe o nome em branco e tente criar.
   - Resultado esperado: «O nome é obrigatório.» e nada é salvo.
3. Nome `Organizar Teste`; pasta `C:\TesteTidyfy\Entrada` («Escolher pasta…»); repetir a cada **1 dia**; um horário **daqui a 3 ou 4 minutos** («Adicionar horário»). Clique em «Criar ajudante».
   - Resultado esperado: o diálogo fecha e aparece o cartão «Organizar Teste» com a pasta, o horário e a próxima execução («Hoje, HH:mm»); contador «Organização de pasta» em 1/20 e «Ligados agora» em 1 de 1.
4. No Agendador de Tarefas, atualize a pasta **Tidyfy**.
   - Resultado esperado: uma tarefa (nome = UUID do ajudante, igual ao campo `id` em `helpers.txt`) com um gatilho diário no horário escolhido, ação `Tidyfy.exe --run <uuid>`, «Executar somente quando o usuário estiver conectado».
5. Abra `%APPDATA%\Tidyfy\helpers.txt`.
   - Resultado esperado: JSON com `version` 1 e o ajudante `FOLDER_ORGANIZER`.

- [ ] Seção 2 conferida

## 3. Execução agendada da organização (com o app fechado)

1. **Feche o Tidyfy.** Confira que a pasta de testes ainda está como foi criada.
2. Espere o horário agendado (ou, no Agendador, botão direito na tarefa, «Executar»). Aguarde alguns segundos.
   - Resultado esperado: nenhuma janela aparece; a «Última Hora de Execução» da tarefa é atualizada e o resultado é «A operação foi concluída com êxito (0x0)».
3. Confira a pasta `C:\TesteTidyfy\Entrada`.
   - Resultado esperado: `Imagens\foto.jpg` e `Imagens\foto2.PNG`; `Documentos\relatorio.pdf` e `Documentos\planilha.xlsx`; `Executáveis\instalador.exe`; `Compactados\pacote.zip`; `Vídeos\filme.mp4`; `Outros\musica.mp3` e `Outros\notas.xyz`; a pasta `Projeto` (inteira, com o conteúdo) dentro de `Pastas`. Nada solto sobrou na raiz.
4. **Duplicado:** copie para a raiz um arquivo idêntico a `Imagens\foto.jpg` (mesmo nome e mesmo conteúdo) e rode a tarefa de novo.
   - Resultado esperado: o arquivo de fora vai para a **Lixeira** (confira lá) e `Imagens\foto.jpg` continua único.
5. **Conflito de nome:** crie na raiz um `relatorio.pdf` com conteúdo **diferente** e rode a tarefa.
   - Resultado esperado: `Documentos\relatorio(1).pdf`; o original continua intacto. Repita com outro conteúdo e surge `relatorio(2).pdf`. Faça o mesmo com uma pasta chamada `Projeto` na raiz: vira `Pastas\Projeto(1)`.
6. **Subpastas do ajudante e subpastas do usuário:** coloque um arquivo dentro de `Imagens`, e outro dentro de `Projeto`.
   - Resultado esperado: nada dentro de subpastas é movido; as categorias permanecem onde estão.
7. Abra o Tidyfy e o histórico do ajudante (seção 9).
   - Resultado esperado: cada execução registrada com as ações (movidos, «Duplicado enviado à lixeira: ...»).
8. Veja `%APPDATA%\Tidyfy\logs\tidyfy-<hoje>.log`.
   - Resultado esperado: linhas da execução, sem erro.

- [ ] Seção 3 conferida

## 4. Limpeza de pasta

Crie `C:\TesteTidyfy\Limpar` com: `novo.txt` (criado hoje), `antigo.txt` e uma subpasta `Sub` com `dentro.txt`. Para o arquivo «antigo», use PowerShell: `(Get-Item C:\TesteTidyfy\Limpar\antigo.txt).CreationTime = (Get-Date).AddDays(-10); (Get-Item C:\TesteTidyfy\Limpar\antigo.txt).LastWriteTime = (Get-Date).AddDays(-10)`.

1. Em «Novo ajudante», escolha «Limpeza». Nome `Limpar Pasta`; em «O que limpar» escolha «Uma pasta» («Remove seus arquivos») e a pasta `C:\TesteTidyfy\Limpar`; «Quanto limpar»: **Tudo**; horário daqui a poucos minutos.
   - Resultado esperado: a prévia mostra «Os arquivos desta pasta são removidos.»
2. Salve, feche o Tidyfy e execute a tarefa no horário (ou manualmente no Agendador).
   - Resultado esperado: `novo.txt` e `antigo.txt` somem da pasta e estão na **Lixeira** (recuperáveis); a subpasta `Sub` e `dentro.txt` continuam intactas.
3. Recoloque `novo.txt` e `antigo.txt` (ajuste as datas do antigo de novo). Edite o ajudante: «Quanto limpar» → «Só o que tem mais de» **5** dias; «Contar a idade a partir da» → «Data de criação». Salve e execute.
   - Resultado esperado: só `antigo.txt` vai para a Lixeira; `novo.txt` fica.
4. Teste criação vs. modificação: recoloque `antigo.txt` com `CreationTime` de 10 dias atrás e `LastWriteTime` de hoje. Com «Data de criação» ele é removido; mude para «Última modificação» e ele **não** é removido (reponha o arquivo entre as duas execuções).
   - Resultado esperado: o critério escolhido é o que vale.

- [ ] Seção 4 conferida

## 5. Limpeza da lixeira

> **ATENÇÃO:** este passo apaga **definitivamente** o que está na Lixeira. Use só itens de teste e **esvazie a Lixeira antes de começar**. Confira o conteúdo da Lixeira antes de cada execução.

### 5.1 Esvaziar tudo

1. Envie 2 arquivos de teste (`a.txt`, `b.txt`) para a Lixeira.
2. Crie uma limpeza: nome `Esvaziar Lixeira`, «O que limpar» → «Lixeira do Windows» («Esvazia a lixeira»), «Quanto limpar» → **Tudo**, horário próximo.
   - Resultado esperado: a prévia mostra «A lixeira é esvaziada.»
3. Execute a tarefa (com o app fechado).
   - Resultado esperado: a Lixeira fica vazia; o histórico mostra «Lixeira esvaziada».

### 5.2 Só o mais antigo que N dias (valida `emptyOlderThan`)

Este é o passo que valida de verdade a limpeza da lixeira por idade (a idade conta do dia em que o arquivo foi para lá).

1. Com a Lixeira vazia, envie 3 arquivos de teste em momentos diferentes, anotando a hora de cada um: `um.txt` (agora), `dois.txt` e `tres.txt`. Para ter idades diferentes sem esperar dias, use o relógio do computador: ajuste a data do Windows (Configurações, «Hora e idioma») para 10 dias atrás, envie `um.txt` à Lixeira; avance para 4 dias atrás, envie `dois.txt`; volte a data para hoje e envie `tres.txt`. (Desligue «Definir a hora automaticamente» antes e religue ao terminar.)
2. Na Lixeira, em «Detalhes», confira a coluna «Data de exclusão»: `um.txt` ≈ 10 dias, `dois.txt` ≈ 4 dias, `tres.txt` hoje.
3. Edite `Esvaziar Lixeira`: «Quanto limpar» → «Só o que tem mais de» **7** dias (a ajuda mostra «Na lixeira, a idade conta do dia em que o arquivo foi para lá.»). Salve e execute a tarefa.
   - Resultado esperado: somem apenas `um.txt`; `dois.txt` e `tres.txt` continuam na Lixeira.
4. Mude para **2** dias e execute.
   - Resultado esperado: some também `dois.txt`; `tres.txt` (hoje) permanece.
5. Mude para **1** dia: `tres.txt` permanece (ainda não tem mais de 1 dia).
6. Confira o histórico.
   - Resultado esperado: «Lixeira esvaziada: ...» com a contagem de itens removidos em cada execução (ou «Nada a fazer» quando nada atingiu o limite).

- [ ] Seção 5 conferida

## 6. Regras de validação

Cada violação aparece em um diálogo de erro (botão «Fechar») e o ajudante **não** é salvo. Confira também que nenhuma tarefa nova surgiu no Agendador.

1. **Nome repetido:** crie outro ajudante chamado `organizar teste` (maiúsculas e minúsculas não diferenciam).
   - Resultado esperado: título «Nome já usado» e a mensagem `Já existe um ajudante chamado "organizar teste".`
2. **Pasta em uso (mesmo tipo):** crie outra organização apontando para `C:\TesteTidyfy\Entrada`.
   - Resultado esperado: título «Pasta já usada» e `A pasta ... já está em uso pelo ajudante "Organizar Teste" (mesma pasta ou pasta aninhada).`
3. **Pasta aninhada:** crie outra organização em `C:\TesteTidyfy\Entrada\Imagens`, e uma em `C:\TesteTidyfy` (pasta que contém a outra).
   - Resultado esperado: mesma mensagem «Pasta já usada» nos dois casos.
4. **Tipos diferentes podem dividir a pasta:** crie uma limpeza apontando para `C:\TesteTidyfy\Entrada`.
   - Resultado esperado: salva sem erro (depois remova este ajudante).
5. **Pasta protegida:** crie uma organização em `C:\Windows` (e outras tentativas: `C:\Windows\System32`, `C:\Program Files`, `C:\ProgramData`, a raiz `C:\` e a sua pasta de usuário).
   - Resultado esperado: título «Pasta protegida» e `A pasta ... é protegida e não pode ser usada por um ajudante.` Uma pasta dentro do usuário, como `...\Downloads`, é aceita (não salve de verdade).
6. **Segunda limpeza da lixeira:** com `Esvaziar Lixeira` existindo, crie outra limpeza também com «Lixeira do Windows».
   - Resultado esperado: «Pasta já usada» com `A lixeira já está em uso pelo ajudante "Esvaziar Lixeira".`
7. **Limite de 20 por tipo:** crie 20 ajudantes de um tipo (use pastas diferentes dentro da pasta de testes, por exemplo `C:\TesteTidyfy\P01` a `P20`, e nomes `Teste 01`...). Ao chegar a 20, abra «Novo ajudante».
   - Resultado esperado: o tipo aparece bloqueado com «Limite atingido» e o contador `20/20`.
8. **Horários:** tente adicionar o mesmo horário duas vezes; tente salvar sem nenhum horário.
   - Resultado esperado: «Esse horário já está na lista.» e a regra «Pelo menos um horário, sem repetir.»

- [ ] Seção 6 conferida

## 7. Ligar e desligar

1. No cartão de `Organizar Teste`, use o interruptor (dica «Ligar ou desligar») para **desligar**.
   - Resultado esperado: o cartão fica em estado desligado («Desligado: fora do Agendador do Windows»); «Ligados agora» diminui; a tarefa **some** do Agendador (F5); `enabled` vira `false` em `helpers.txt`.
2. Ligue de novo.
   - Resultado esperado: a tarefa **volta** ao Agendador com o mesmo gatilho; o cartão mostra a próxima execução (dica «Próxima execução»).
3. Com o ajudante desligado, na hora agendada nada executa e nenhum registro novo entra no histórico.

- [ ] Seção 7 conferida

## 8. Editar

1. Selecione **um** ajudante (caixa do cartão) e clique em «Editar».
   - Resultado esperado: formulário «Editar ajudante» com nome, pasta, frequência e (limpeza) alvo, idade e referência **preenchidos** como foram salvos. O botão é «Salvar alterações».
2. Selecione **dois** ajudantes.
   - Resultado esperado: «Editar» fica indisponível («Edite um ajudante por vez.»).
3. Edite `Organizar Teste`: mude o intervalo para 2 dias e o horário. Salve.
   - Resultado esperado: a tarefa no Agendador é atualizada; em `helpers.txt` o `startDate` passa a ser **hoje** (a data de início muda quando a frequência muda).
4. Edite só o nome (sem mexer na frequência).
   - Resultado esperado: o `startDate` não muda.
5. Edite a limpeza com limite de idade (seção 4) e salve sem tocar no limite.
   - Resultado esperado: o limite e a referência de idade (criação/modificação) continuam como estavam.
6. Editar não conta para o limite de 20 (com 20 criados, editar um deles funciona).

- [ ] Seção 8 conferida

## 9. Histórico

1. No cartão, clique no botão de histórico (dica «Histórico»).
   - Resultado esperado: diálogo «Histórico de <nome>» com «N execuções registradas», cada execução com data/hora, resumo («3 ações», «Nada a fazer») e as ações; quando há muitas, «Ver todas as N ações» e «Mostrar menos».
2. Ajudante que nunca rodou.
   - Resultado esperado: «Nenhuma execução registrada», «Este ajudante ainda não foi executado.» e «Quando ele rodar, o que fez aparece aqui.»
3. **Provoque uma falha:** na organização, deixe um arquivo aberto em um programa que o bloqueie (por exemplo, um `.txt` aberto no Word ou um `.pdf` aberto) ou, para a limpeza, apague a pasta do ajudante. Execute a tarefa.
   - Resultado esperado: a execução aparece no histórico com a ação de falha destacada («Falha em <arquivo>: motivo» ou «Falha: motivo»), o resumo traz «N com falha», o log do dia registra o erro e o resultado da tarefa no Agendador é diferente de 0 (código 1).
4. O diálogo mostra o rodapé «O histórico guarda as execuções dos últimos 30 dias.»
5. (Opcional) Edite `history.txt` com uma execução de 31 dias atrás e rode o ajudante: o registro antigo some.

- [ ] Seção 9 conferida

## 10. Remover

1. Selecione um ou mais ajudantes e clique em «Remover».
   - Resultado esperado: confirmação «Remover ajudantes» com o texto «Eles deixam de rodar e saem do Agendador do Windows. As pastas e os arquivos continuam como estão.» e «Cancelar».
2. Cancele.
   - Resultado esperado: nada muda.
3. Remova de verdade.
   - Resultado esperado: os cartões somem; as tarefas **somem** do Agendador; `helpers.txt` não os tem mais; as pastas e arquivos continuam; o contador do tipo diminui.
4. O histórico **permanece** em `history.txt` (registros dos ajudantes removidos ficam até expirar em 30 dias).

- [ ] Seção 10 conferida

## 11. Falhas e robustez

1. **Arquivo corrompido:** feche o Tidyfy, abra `%APPDATA%\Tidyfy\helpers.txt` e estrague o JSON (apague uma chave). Abra o Tidyfy.
   - Resultado esperado: diálogo «Arquivo corrompido»; o original **continua intacto** e surge uma cópia `helpers.txt.bak-<data>-<hora>` ao lado. A interface não perde nem sobrescreve nada. Restaure o arquivo bom depois.
2. **Sincronização — tarefa apagada à mão:** com ajudantes ligados, feche o Tidyfy, apague uma tarefa da pasta Tidyfy no Agendador e abra o app. Aguarde alguns segundos.
   - Resultado esperado: a tarefa **volta** ao Agendador.
3. **Tarefa de ajudante desligado:** crie à mão uma tarefa do ajudante desligado (ou desligue o ajudante com o app fechado editando `enabled` para `false`) e abra o app.
   - Resultado esperado: a tarefa sai do Agendador.
4. **Tarefa órfã:** no Agendador, importe/crie uma tarefa `Tidyfy\<um-uuid-qualquer>` (por exemplo duplicando uma existente com outro nome UUID) e abra o app.
   - Resultado esperado: a tarefa órfã **some**.
5. Se uma dessas operações falhar, a interface ainda abre normalmente e o motivo vai para o log.
6. **Falha ao registrar:** (opcional) se conseguir bloquear o agendador, ao salvar a interface deve mostrar o erro e **não** gravar o ajudante em `helpers.txt`.

- [ ] Seção 11 conferida

## 12. Comportamento do Agendador

1. Abra as propriedades de uma tarefa Tidyfy no Agendador.
   - Resultado esperado: «Executar somente quando o usuário estiver conectado»; na aba «Condições», a opção de iniciar só com energia da tomada está **desmarcada** (roda na bateria); na aba «Configurações», «Executar a tarefa o mais rápido possível após uma inicialização agendada ter sido perdida» está **marcada**.
2. **Horário perdido:** agende um ajudante para daqui a 5 minutos, suspenda o computador (ou desligue) até depois do horário e ligue de novo, fazendo login.
   - Resultado esperado: a tarefa executa logo que possível após o login, e o histórico registra a execução.
3. **Bateria (notebook):** desconecte da tomada, deixe chegar o horário.
   - Resultado esperado: executa normalmente.
4. **Sem login:** (opcional) deixe a sessão bloqueada ou encerrada no horário.
   - Resultado esperado: não executa sem o usuário logado; executa após o login (item 2).

- [ ] Seção 12 conferida

## 13. Instalador e desinstalação (validado em parte)

> **Validado em parte (15/10/2026, issue #71):** os itens 1 a 4, 7 e 8 foram executados de verdade, de forma silenciosa (`msiexec`), com uma tarefa de teste no lugar de ajudantes reais: o MSI foi gerado, instalou por usuário, criou os atalhos (menu Iniciar na pasta «Tidyfy» e área de trabalho), abriu a janela «Tidyfy» e a desinstalação removeu a tarefa, os arquivos, os atalhos e a entrada em «Aplicativos instalados». **Continuam pendentes:** o item 5 com ajudantes reais, a atualização por cima (itens 6 e 9) e a desinstalação pela tela de Configurações (item 7, pela interface).

1. **Pré-requisito:** instale o WiX Toolset 3.x e confirme `candle.exe` e `light.exe` no PATH.
2. **Gerar o MSI:** `mvn clean verify -Ppackage,installer -DskipTests`.
   - Resultado esperado: o build termina com sucesso e existe um `.msi` em `target\installer`.
3. **Instalar** o MSI (instalação por usuário, sem pedir administrador).
   - Resultado esperado: instala sem erros; há atalho no **menu Iniciar** e na **área de trabalho**.
4. **Abrir** pelo menu Iniciar e pelo atalho.
   - Resultado esperado: a interface abre (lista vazia ou com os ajudantes já existentes).
5. **Registrar tarefas:** crie 2 ajudantes ligados e confirme as tarefas no Agendador.
6. **Atualizar por cima:** gere de novo o MSI (mesmo `installer.upgrade.uuid`, versão maior em `installer.version` se quiser) e instale sem desinstalar.
   - Resultado esperado: atualiza a instalação existente (sem duplicar entradas em «Aplicativos instalados»), os ajudantes e as tarefas continuam.
7. **Desinstalar** (Configurações, Aplicativos, Tidyfy, Desinstalar) com as tarefas registradas.
   - Resultado esperado: a desinstalação conclui; o instalador executa `Tidyfy.exe --remove-tasks`; **todas** as tarefas da pasta Tidyfy somem do Agendador; a pasta de instalação, o atalho e a entrada do menu Iniciar somem.
8. Confira `%APPDATA%\Tidyfy`.
   - Resultado esperado: `helpers.txt`, `history.txt` e `logs` **permanecem**, e o log do dia registra a remoção das tarefas.
9. **Atualização não remove tarefas:** repita o item 6 e confirme que as tarefas não foram apagadas (a remoção só ocorre na desinstalação, não na atualização).
10. (Direto, sem MSI) Com o pacote gerado, rode `target\dist\Tidyfy\Tidyfy.exe --remove-tasks` e confira o Agendador; o código de saída é 0 (`echo %ERRORLEVEL%` no `cmd`; o `.exe` é GUI, então use `start /wait`).

- [ ] Seção 13 conferida (itens 5, 6 e 9 e a desinstalação pelas Configurações ainda pendentes)

## 15. Organizador da área de trabalho

> **Validado em parte (28/10/2026, issue #81):** a camada de leitura/gravação e o planejador foram executados de verdade na área de trabalho real (Windows 11, um monitor 1920×1080, 13 ícones, escala 150%): para cada um dos seis layouts os ícones foram reposicionados e lidos de volta (13/13 iguais ao plano, 13 posições distintas, nenhum fora da tela) e, no fim, as posições originais foram restauradas (13/13). Nesse teste a grade lida foi 1920×1008 (barra de tarefas descontada) e as opções «Organizar ícones automaticamente» e «Alinhar à grade» estavam desligadas. **Continuam pendentes** (exigem a interface, o Agendador ou outro hardware): passos 1, 2, 4, 8 e 9 (criar pelo app e executar pela tarefa agendada), 6 (vários monitores), 7 («Organizar automaticamente» ligado) e 5 em 100%. Reposicionar mexe nos seus ícones: anote a disposição atual antes de repetir.

1. Em «Novo ajudante», escolha «Organizador da área de trabalho». Nome `Arrumar Área`; layout **Esquerda**; horário daqui a 3 ou 4 minutos. Salve.
   - Resultado esperado: o cartão aparece com o miniatura do layout; em «Novo ajudante» o tipo fica bloqueado (só pode existir um).
2. Com o Tidyfy fechado, espere o horário (ou execute a tarefa no Agendador).
   - Resultado esperado: os ícones ficam em colunas na borda esquerda, em ordem de nome; nenhum arquivo foi movido; o histórico mostra «Ícones da área de trabalho reposicionados: N ícones».
3. **Cada layout:** edite o ajudante trocando o layout para **Direita**, **Topo**, **Base**, **Bordas** e **Por tipo**, executando a tarefa a cada troca (ou ajuste o horário).
   - Resultado esperado: Direita e Base encostam nas bordas direita e inferior; Topo enche linhas a partir do alto; Bordas deixa o centro livre; Por tipo cria uma coluna por categoria (atalhos, documentos, imagens...). Nunca há dois ícones no mesmo lugar nem fora da tela.
4. **Excesso de ícones:** crie ícones suficientes para não caberem no layout (ou use uma resolução baixa).
   - Resultado esperado: os que couberam são arrumados; os demais ficam onde estavam e o histórico mostra uma falha com o nome deles.
5. **Escala de tela:** repita o passo 2 com a escala em 100% e em 150% (Configurações → Sistema → Tela).
   - Resultado esperado: a área considerada é a visível inteira (descontada a barra de tarefas) nas duas escalas.
6. **Vários monitores:** com dois monitores, repita o passo 2.
   - Resultado esperado: só o monitor principal é arrumado; o outro não muda.
7. **«Organizar ícones automaticamente» ligado:** botão direito na área de trabalho → Exibir → marque a opção e execute a tarefa.
   - Resultado esperado: nada muda, o histórico mostra a falha com a mensagem clara e o Tidyfy não desliga a opção. Desmarque a opção depois. Com «Alinhar ícones à grade» ligado, as posições só são ajustadas à célula mais próxima.
8. **Ajudante agendado:** confira no Agendador que a tarefa do ajudante existe e roda com o app fechado; desligue o ajudante e confira que a tarefa some.
   - Resultado esperado: igual aos demais tipos (seção 12).
9. **Histórico:** abra o histórico do ajudante.
   - Resultado esperado: cada execução lista a ação de reposicionamento (e as falhas destacadas).

- [ ] Seção 15 conferida

## 14. Aparência (ainda não conferida numa janela real)

Itens visuais que só foram verificados por teste automatizado. Confira no aplicativo aberto:

- [ ] **Tema escuro** em todas as telas e diálogos (sem áreas brancas, textos legíveis).
- [ ] **Cartão** do ajudante com o botão de histórico, o interruptor, o nome, a pasta, os horários e a próxima execução, sem cortes de texto.
- [ ] **Diálogo de histórico:** título, subtítulo, lista de ações, falha destacada, rolagem quando há muitas ações.
- [ ] **Formulário de limpeza:** seção «Quanto limpar» («Tudo» / «Só o que tem mais de»), o contador de dias, «Contar a idade a partir da» («Data de criação» / «Última modificação»), a prévia e os botões «Cancelar» e «Criar ajudante».
- [ ] **Diálogo de erro:** título, mensagem, «Mostrar detalhes» e «Fechar».
- [ ] **Janela mínima** (960 × 700): redimensione até o mínimo e confira que nada sobrepõe nem some; na janela padrão (1280 × 900) cabem 9 cartões sem barra de rolagem; com mais ajudantes só a área dos cartões rola.
- [ ] **Seleção:** ao selecionar, as ações («Editar», «Remover», «Selecionar todos») aparecem no lugar do filtro, sem empurrar os cartões.
- [ ] **Diálogos:** arrastar pela área de título, fechar com Esc, lista escurecida por trás.

## Resultado

Preencha ao terminar (data, versão/commit testado e máquina: ______________________).

| Seção | Passou | Falhou | Observações |
|---|---|---|---|
| 1. Abrir e lista vazia | | | |
| 2. Criar organização | | | |
| 3. Execução agendada da organização | | | |
| 4. Limpeza de pasta | | | |
| 5. Limpeza da lixeira | | | |
| 6. Regras de validação | | | |
| 7. Ligar e desligar | | | |
| 8. Editar | | | |
| 9. Histórico | | | |
| 10. Remover | | | |
| 11. Falhas e robustez | | | |
| 12. Comportamento do Agendador | | | |
| 13. Instalador e desinstalação (itens 1–4, 7 e 8 validados em 15/10/2026; 5, 6 e 9 pendentes) | | | |
| 14. Aparência | | | |
| 15. Organizador da área de trabalho (passo 3 e 5 em 150% validados em 28/10/2026; demais pendentes) | | | |

## Limpeza final

- [ ] Remova todos os ajudantes de teste no Tidyfy (e confirme que a pasta Tidyfy do Agendador ficou vazia).
- [ ] Apague a pasta de testes (`C:\TesteTidyfy`).
- [ ] Esvazie da Lixeira os itens de teste.
- [ ] Religue «Definir a hora automaticamente» se desligou na seção 5.
- [ ] (Opcional) Apague `%APPDATA%\Tidyfy` e desinstale o Tidyfy, se instalou o MSI.
