# CLAUDE.md

Guia técnico do Tidyfy. Para o contexto de negócio, veja o [README.md](../README.md).

## Visão geral

Aplicativo **somente Windows** que cria "ajudantes": tarefas de limpeza/organização que rodam periodicamente através do **Agendador de Tarefas do Windows**. O usuário configura os ajudantes por uma interface JavaFX.

O usuário cria ajudantes escolhendo um **tipo** e dando um **nome**. Tipos iniciais:

1. **Organização de pasta** (`FOLDER_ORGANIZER`): move os arquivos de **uma** pasta para subpastas por tipo (imagens, documentos, executáveis...) e as pastas soltas para a subpasta "Pastas".
2. **Limpeza** (`CLEANER`): esvazia a lixeira do Windows **ou** remove os arquivos de **uma** pasta.

Todo ajudante tem: nome, tipo, frequência (a cada N dias, em um ou mais horários do dia) e estado ligado/desligado.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 21 (JDK 21) |
| Build | Maven (3.8+) |
| Interface | JavaFX 21 (FXML + CSS) |
| Testes | JUnit 5 (adicionar Mockito quando houver necessidade de dublês) |
| Persistência | Arquivos `.txt` com conteúdo em JSON, lidos e gravados com Jackson (+ `jackson-datatype-jsr310` para `java.time`) |
| Agendamento | Agendador de Tarefas do Windows (`schtasks.exe`, com tarefa definida em XML) |
| Plataforma | Windows apenas |

Estado atual do `pom.xml`: Java 21, JavaFX 21 (`javafx-controls` e `javafx-fxml`, mais o `javafx-maven-plugin` para rodar a interface), Jackson 2.18 (`jackson-databind` e `jackson-datatype-jsr310`) e JUnit 5.

## Regras do projeto (obrigatórias)

1. **Tudo que for implementado deve ter testes unitários.** Código novo sem teste não está pronto. Bug corrigido ganha um teste que o reproduz.
2. **Arquitetura limpa** (Clean Architecture). A regra de dependência nunca pode ser quebrada (veja abaixo).
3. **Somente Windows.** Código que depende do Windows deve ficar isolado na camada de infraestrutura, atrás de interfaces, para que o restante seja testável em qualquer ambiente.
4. Mantenha o código simples: sem abstrações ou configurações que nenhuma regra de negócio atual exija.
5. **Siga as [boas práticas de código](../docs/BOAS-PRATICAS.md).** Resumo: todo tipo, método, construtor, atributo e constante tem Javadoc (blocos de várias linhas para tipos e métodos, `/** Texto */` para variáveis); nenhuma `String` solta, todas em constantes da própria classe; as constantes ficam depois do último atributo, sob o comentário `// Constantes`.

## Arquitetura

Pacote raiz: `com.tidyfy`

```
com.tidyfy                  Main + AppContainer (composition root)
├── domain                  Regras de negócio puras (entidades, objetos de valor, exceções de domínio)
├── application
│   ├── usecase             Casos de uso
│   └── port                Portas (interfaces) para o mundo externo
├── infrastructure
│   ├── windows             schtasks, lixeira, execução de processos
│   ├── filesystem          Acesso ao disco (java.nio.file)
│   └── persistence         Configuração dos ajudantes
└── presentation            Interface JavaFX: TidyfyApplication, UiServices, HistoryLoader, controllers das telas e diálogos (formulários, remoção, histórico, erro)
    ├── model               Modelos de tela (o que a interface exibe; Java puro, sem JavaFX)
    └── component           Componentes visuais reutilizáveis (cartão, interruptor, ícones, animações...)
```

FXML e CSS ficam em `src/main/resources/com/tidyfy/presentation` (mesmo pacote das classes que os carregam).

**Regra de dependência:** as dependências apontam sempre para dentro.

```
presentation ──► application ──► domain
infrastructure ──► application ──► domain
```

- `domain` não depende de nenhuma outra camada nem de framework (nem JavaFX, nem APIs do Windows). Só Java puro.
- `application` depende apenas de `domain`. Define as **portas** (ex.: agendador de tarefas, acesso ao sistema de arquivos, lixeira, repositório de configurações).
- `infrastructure` implementa as portas de `application`.
- `presentation` chama os casos de uso; não contém regra de negócio.
- A montagem das dependências (composition root) fica em `com.tidyfy.AppContainer`, criado pelo `Main`.

### Diagramas de classe

Os diagramas PlantUML em [docs/diagrams](../docs/diagrams) são a referência do design planejado:

| Arquivo | Conteúdo |
|---|---|
| [01-layers.puml](../docs/diagrams/01-layers.puml) | Camadas e regra de dependência |
| [02-domain.puml](../docs/diagrams/02-domain.puml) | Ajudantes, tipos e limite, alvo e idade da limpeza, agendamento, regras de nome e pasta, pastas sensíveis, categorias, planejador de organização e histórico |
| [03-application.puml](../docs/diagrams/03-application.puml) | Casos de uso (incluindo execução e histórico) e portas |
| [04-infrastructure.puml](../docs/diagrams/04-infrastructure.puml) | Implementações das portas (schtasks, disco, lixeira, persistência das configurações e do histórico) |
| [05-presentation.puml](../docs/diagrams/05-presentation.puml) | Telas JavaFX, modelos de tela, componentes, `Main` e `AppContainer` |

**Mantenha os diagramas atualizados:** ao criar, renomear ou remover classes/relacionamentos relevantes, atualize o `.puml` correspondente no mesmo commit.

### Portas previstas

| Porta (application) | Responsabilidade | Implementação (infrastructure) |
|---|---|---|
| Agendador de tarefas | Criar, atualizar, pausar e remover tarefas agendadas | Wrapper do `schtasks.exe` |
| Sistema de arquivos | Listar o que há na pasta, comparar conteúdo, mover e enviar à lixeira | `java.nio.file` (lixeira: PowerShell + `Microsoft.VisualBasic`, veja "Enviar à lixeira sem interface") |
| Lixeira | Esvaziar a lixeira do Windows, tudo ou só o que está lá há mais de X dias | A definir (ex.: PowerShell `Clear-RecycleBin` ou chamada nativa) |
| Repositório de ajudantes | Salvar/carregar a configuração dos ajudantes e contar por tipo | Arquivo `%APPDATA%\Tidyfy\helpers.txt` (JSON) |
| Histórico de execuções | Registrar e consultar quando cada ajudante rodou e o que fez | Arquivo `%APPDATA%\Tidyfy\history.txt` (JSON) |

### Decisões técnicas

- **Persistência:** configurações em `helpers.txt` e histórico em `history.txt`, ambos em `%APPDATA%\Tidyfy`, com extensão `.txt` e conteúdo em JSON. A gravação é atômica (arquivo temporário e depois mover).
- **Acesso simultâneo:** as operações que alteram `helpers.txt`/`history.txt` (ler-alterar-gravar) rodam sob uma trava exclusiva: `ReentrantLock` por arquivo (threads) mais `FileLock` em `<arquivo>.lock` (processos), com espera máxima de 10 s e `UncheckedIOException` ao esgotar (`FileLocks`). O `.lock` fica na pasta; leituras não travam.
- **Agendador:** uma única tarefa por ajudante, definida em **XML**, com um gatilho por horário. Roda só com o usuário logado (precisa das pastas e da lixeira dele), permite rodar na bateria e executa assim que possível quando perdeu o horário.
- **Falha ao salvar:** o Tidyfy tenta registrar no agendador primeiro; se o agendador falhar, a configuração **não** é salva e o erro aparece na tela.
- **Sincronização:** ao abrir a interface, em segundo plano (thread daemon, `SchedulerSyncStarter`), o `SyncSchedulerUseCase` torna o agendador igual à configuração: registra de novo (o `/F` substitui) a tarefa de cada ajudante ligado, remove a dos desligados e as tarefas órfãs `Tidyfy\{uuid}` (`TaskScheduler.registeredIds`). Falha em um item não impede os demais; as falhas vão para o log e nunca impedem a abertura da interface.
- **Falha ao executar:** vai para o histórico (`FAILED`) e para o log.
- **Log (`ExecutionLog`):** um arquivo por dia, `%APPDATA%\Tidyfy\logs	idyfy-yyyy-MM-dd.log` (UTF-8, anexando); apaga logs com mais de 30 dias e, ao chegar a 1 MiB no dia, para de gravar e registra o limite uma vez. Falhas ao gravar o log são engolidas, nunca derrubam a execução.

#### Comando da tarefa agendada

- **Resolução:** `LauncherLocator` (`infrastructure.windows`) decide o comando na composição (`AppContainer.create`) e entrega um `TaskCommand` (executável + argumentos base) ao `SchtasksTaskScheduler`. O XML fica `<Command>executável</Command><Arguments>argumentos base --run {id}</Arguments>`; o `Arguments` é uma única linha, com aspas nos argumentos que têm espaço (caminhos) e escape XML.
- **Empacotado** (`mvn clean package -Ppackage`): o processo atual é `Tidyfy.exe`, então a tarefa executa `Tidyfy.exe --run {id}`.
- **Desenvolvimento** (`mvn javafx:run`, IDE): o processo atual é o Java, então a tarefa executa `<java.home>\bin\javaw.exe -cp <java.class.path> com.tidyfy.Main --run {id}`. **Limitação:** depende do classpath do momento; se o projeto for recompilado, movido ou o classpath mudar, a tarefa quebra até o ajudante ser salvo de novo ou a interface abrir (a sincronização registra de novo com o classpath atual). Sem informação do sistema, usa `javaw.exe` do `java.home` (sem `-cp` se o classpath faltar).
- **Comportamento da tarefa (DEC4):** roda **só com o usuário logado** (`LogonType` `InteractiveToken`, `RunLevel` `LeastPrivilege`, pois precisa das pastas e da lixeira dele); **roda na bateria** (`DisallowStartIfOnBatteries` e `StopIfGoingOnBatteries` falsos); se o computador estava desligado no horário, **executa assim que possível** (`StartWhenAvailable`); **instância única** (`MultipleInstancesPolicy` `IgnoreNew`: se a anterior ainda roda, a nova é ignorada).

#### Enviar à lixeira sem interface

A execução agendada não tem janela, então `FileSystem.moveToTrash` não pode depender de `java.awt.Desktop`. Testado no Windows 11 (Java 21), com arquivos e pastas de teste em um diretório temporário:

| Alternativa | Sem interface? | Arquivo | Pasta | Acentos, espaços e `'` | Arquivo inexistente | Tempo |
|---|---|---|---|---|---|---|
| `Desktop.moveToTrash` com `-Djava.awt.headless=true` | **Não**: `isDesktopSupported()` dá `false` e `isSupported`/`moveToTrash` lançam `HeadlessException` | falha | falha | n/a | n/a | n/a |
| `Desktop.moveToTrash` sem a opção (JVM com AWT) | Só com sessão gráfica; depende do modo do AWT | ok | ok | ok | `false`/exceção | ~150 ms |
| **PowerShell + `Microsoft.VisualBasic` `SendToRecycleBin`** (`-NoProfile -NonInteractive -WindowStyle Hidden`, chamado de um processo Java headless) | **Sim** | ok (`DeleteFile`) | ok (`DeleteDirectory`, com o conteúdo) | ok (`'` dobrado no script) | código de saída 1 e `FileNotFoundException`/`DirectoryNotFoundException` na saída | ~350 a 800 ms |
| `Shell.Application` COM `InvokeVerb('delete')` | Sim, mas é o caminho do Explorer | ok | ok | ok | sem erro útil (`ParseName` devolve nulo) | ~2,3 s |

- **Escolhida:** PowerShell + `Microsoft.VisualBasic.FileIO.FileSystem`, montada por `TrashCommands` (`infrastructure.filesystem`): `powershell.exe -NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -Command "Add-Type -AssemblyName Microsoft.VisualBasic; [Microsoft.VisualBasic.FileIO.FileSystem]::DeleteFile('<caminho>', 'OnlyErrorDialogs', 'SendToRecycleBin')"` (`DeleteDirectory` para pastas). O item é recuperável na Lixeira.
- **Erros:** o código de saída é `0` em sucesso e `1` em falha, com a exceção do .NET na saída; a implementação deve virar `UncheckedIOException` e deixar o item como está.
- **Limitação:** caminhos com mais de 260 caracteres falham (`Could not find file`, código 1), porque o `Microsoft.VisualBasic` do Windows PowerShell 5.1 não aceita caminho longo. Vira falha da execução (`FAILED` no histórico).
- A API nativa do Java 21 (FFM) segue em preview e não foi usada.

#### Formato do `helpers.txt`

Arquivo em `%APPDATA%\Tidyfy\helpers.txt`, extensão `.txt`, conteúdo **JSON em UTF-8**. Arquivo ausente ou vazio significa nenhum ajudante. A gravação é atômica (arquivo temporário e depois mover).

Objeto raiz: `version` (inteiro, começa em `1`) e `helpers` (lista). Uma `version` desconhecida é erro (o arquivo não é lido nem sobrescrito, como no caso de arquivo corrompido). Qualquer mudança incompatível no formato incrementa a versão.

Campos comuns a todo ajudante:

| Campo | Descrição |
|---|---|
| `type` | Discriminador: `FOLDER_ORGANIZER` ou `CLEANER` |
| `id` | UUID em texto |
| `name` | Nome do ajudante |
| `enabled` | `true` ou `false` |
| `schedule` | `intervalDays` (inteiro ≥ 1), `times` (lista de horários `HH:mm`, sem repetição, ao menos um) e `startDate` (data ISO `yyyy-MM-dd`) |

Campos por tipo:

- `FOLDER_ORGANIZER`: `folder`, o caminho da pasta em texto (barras invertidas escapadas: `C:\\Users\\ana\\Downloads`).
- `CLEANER`: `target` e `ageLimitDays`.
  - `target` é `{"type": "FOLDER", "folder": "...", "ageReference": "CREATED" | "MODIFIED"}` ou `{"type": "RECYCLE_BIN"}`.
  - `ageLimitDays` é um inteiro ≥ 1 (só limpa o que é mais antigo que isso). `null` ou ausente significa limpar tudo.

Organização de pasta:

```json
{
  "version": 1,
  "helpers": [
    {
      "type": "FOLDER_ORGANIZER",
      "id": "3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10",
      "name": "Organizar Downloads",
      "enabled": true,
      "schedule": {"intervalDays": 1, "times": ["09:00", "18:00"], "startDate": "2026-10-14"},
      "folder": "C:\\Users\\ana\\Downloads"
    }
  ]
}
```

Limpeza de pasta com limite de idade (item da lista `helpers`):

```json
{
  "type": "CLEANER",
  "id": "a91c5d72-0b3e-4f68-8d14-6e7f2c3b5a09",
  "name": "Limpar Temporários",
  "enabled": true,
  "schedule": {"intervalDays": 7, "times": ["20:30"], "startDate": "2026-10-14"},
  "target": {"type": "FOLDER", "folder": "C:\\Users\\ana\\Temp", "ageReference": "MODIFIED"},
  "ageLimitDays": 30
}
```

Limpeza da lixeira sem limite, limpa tudo (item da lista `helpers`):

```json
{
  "type": "CLEANER",
  "id": "c47e0f13-9d2a-4b85-a6c1-3e8d5f7a2b64",
  "name": "Esvaziar Lixeira",
  "enabled": false,
  "schedule": {"intervalDays": 3, "times": ["12:00"], "startDate": "2026-10-14"},
  "target": {"type": "RECYCLE_BIN"},
  "ageLimitDays": null
}
```

#### Formato do `history.txt`

Arquivo em `%APPDATA%\Tidyfy\history.txt`, ao lado do `helpers.txt`, extensão `.txt`, conteúdo **JSON em UTF-8**. Arquivo ausente ou vazio significa histórico vazio. A gravação é atômica e arquivo corrompido ou de versão desconhecida segue a mesma política do `helpers.txt` (original intacto, cópia `history.txt.bak-<carimbo>` e `CorruptedStorageException`).

Objeto raiz: `version` (inteiro, começa em `1`) e `records` (lista, na ordem em que foram gravados). Cada registro tem `helperId` (UUID em texto), `helperName` (nome na hora da execução), `executedAt` (instante ISO-8601 em UTC, com fração de segundo se houver) e `actions` (lista). Cada ação tem `type` (`MOVED`, `DUPLICATE_TRASHED`, `TRASHED`, `RECYCLE_BIN_EMPTIED` ou `FAILED`) e os opcionais `source`, `target` e `message` (`null` ou ausentes quando não há; caminhos com barras invertidas escapadas). Os registros com mais de 30 dias são apagados por `deleteExpired`.

```json
{
  "version": 1,
  "records": [
    {
      "helperId": "3f2b8c1e-6a4d-4e0b-9c57-1d2a7b9e4f10",
      "helperName": "Organizar Downloads",
      "executedAt": "2026-10-14T12:00:00Z",
      "actions": [
        {"type": "MOVED", "source": "C:\\Users\\ana\\Downloads\\foto.jpg", "target": "C:\\Users\\ana\\Downloads\\Imagens\\foto.jpg", "message": null},
        {"type": "FAILED", "source": "C:\\Users\\ana\\Downloads\\a.zip", "target": null, "message": "Acesso negado"}
      ]
    }
  ]
}
```

## Funcionamento

1. O usuário cria/edita um ajudante na interface JavaFX.
2. A configuração é salva e uma tarefa correspondente é registrada no Agendador do Windows.
3. No horário agendado, o Windows executa o Tidyfy em **modo sem interface**, informando qual ajudante rodar (por argumento de linha de comando).
4. O Tidyfy carrega a configuração do ajudante, executa o caso de uso e encerra.

Por isso a execução dos ajudantes **não pode depender da interface gráfica**: o `Main` deve distinguir "abrir a interface" de "executar o ajudante X".

## Interface (presentation)

- **Telas em FXML sem `fx:controller`.** O controller é criado no código e entregue ao `FXMLLoader` por `Fxml.load` (componentes `fx:root` usam `Fxml.loadRoot`). Assim os casos de uso são injetados por construtor.
- **Tema escuro único** em `tidyfy.css`. As cores são *looked-up colors* no `.root` (`-tf-bg`, `-tf-ink`, `-tf-org`, `-tf-clean`...); FXML e Java não usam cores fixas, só classes CSS. A cor de cada tipo vem das classes `kind-org` / `kind-clean`.
- **Fontes do Windows:** Segoe UI (texto) e Consolas (caminhos, horários e números). Nada é embutido no jar.
- **Regras de tela:** editar só com **um** ajudante selecionado; remover com um ou vários (`HelperSelection`). O tipo que atingiu o limite aparece bloqueado em "Novo ajudante", mas quem garante o limite é o domínio.
- **A tela principal não rola.** Cabeçalho, contadores e filtro ficam fixos e os cartões (compactos) ocupam o espaço restante: na janela padrão (1280 × 900) cabem 9 sem barra de rolagem. Só a área dos cartões (`cardsScroll`) rola, e apenas quando há mais ajudantes do que cabem. Com seleção, as ações aparecem **no lugar do filtro**, na mesma linha, para não cobrir nem empurrar os cartões. Os testes de `HelperListControllerTest` verificam isso; ao mexer no layout, mantenha-os passando.
- **Diálogos** (`Dialogs.showModal`): janela modal sem moldura, arrastável pelo nó com a classe `drag-handle`, fecha com Esc e escurece a lista enquanto está aberta.
- **Ligação com os casos de uso:** o JavaFX instancia `TidyfyApplication` sem argumentos, então `InterfaceLauncher.launch(AppContainer, Clock, args)` monta um `UiServices` (record em `presentation` com `ListHelpersUseCase`, `SaveHelperUseCase`, `RemoveHelperUseCase`, `ListExecutionHistoryUseCase` e `Clock`) e o entrega por `TidyfyApplication.configure` antes de `Application.launch`; `TidyfyApplication.start` cria a `HelperListController` e liga cada serviço (`setSaveServices`, `setRemoveService`, `setHistoryLoader`). `presentation` nunca importa `AppContainer`.
- **Estado atual:** a interface está ligada aos casos de uso e funciona de ponta a ponta.
  - **Lista:** carrega os ajudantes reais (`ListHelpersUseCase` + `HelperItemMapper`) ao abrir e em `reload()`, que roda depois de salvar, remover ou ligar/desligar e mantém filtro (e, no interruptor, a seleção). Falha na leitura deixa a lista vazia e aciona o `errorHandler`; `TidyfyApplication` liga esse handler ao diálogo de erro (`showError`). O construtor sem argumentos (testes) começa vazio.
  - **Criar e editar:** o formulário (`FolderOrganizerFormController` ou `CleanerFormController`) monta o `Helper` e chama o `SaveHelperUseCase` (`startDate` = hoje ao criar ou quando a frequência muda). Só fecha se der certo. A Limpeza tem «Quanto limpar» (tudo ou só o que passou de N dias, com a referência criação/modificação só para pasta) e carrega esses valores ao editar.
  - **Erros de regra** aparecem no campo certo: nome repetido em `nameHelp`; pasta em uso, aninhada, protegida ou lixeira em uso em `folderError` (campo `invalid`); limite e demais falhas no rodapé `saveError`. O erro do campo some ao editá-lo.
  - **Ligar e desligar:** o interruptor do cartão salva o ajudante com `Helper.withEnabled` pelo `SaveHelperUseCase` (registra ou remove a tarefa). Se falhar, o interruptor volta ao estado anterior e o erro aparece em diálogo; se der certo, a lista é relida sem animar os cartões.
  - **Remover:** `RemoveHelperUseCase` ao confirmar (`RemoveConfirmationController`); cancelar não remove nada. Se falhar, a lista é recarregada e o erro aparece em diálogo. O histórico do ajudante permanece.
  - **Histórico:** a ação do cartão abre `ExecutionHistoryController`, com os itens carregados por `HistoryLoader` (`ListExecutionHistoryUseCase` + `ExecutionItemMapper`, fuso do sistema) e modelos `ExecutionItem`/`ExecutionAction` (falhas destacadas).
  - **Erros inesperados:** `ErrorMessageMapper` converte a exceção em `ErrorMessage` (título, mensagem e detalhes) e `ErrorDialogController` o mostra, com os detalhes recolhíveis.
  - **Ainda não existe:** executável e instalador (veja as issues P1 a P7).
- **`Main`:** `LaunchArguments.parse` interpreta os argumentos (sem argumentos → interface; `--run <uuid>` → executa o ajudante; `--remove-tasks` → remove todas as tarefas do agendador (`RemoveTasksRunner`; veja «Instalador e desinstalação»); o resto é inválido, com mensagem). Só `InterfaceLauncher` referencia o JavaFX, então `--run` nunca o carrega. `HeadlessRunner` executa o ajudante (`RunHelperUseCase`), registra no `ExecutionLog` e devolve o código de saída: 0 sucesso (inclusive ajudante inexistente/desligado), 1 falha (exceção ou ação `FAILED` no histórico), 2 argumentos inválidos. Só o `Main` chama `System.exit`, e apenas fora do modo interface. `RunHelperUseCase.execute` devolve `Optional<ExecutionRecord>` (vazio se o ajudante não existe ou está desligado).

## Regras de negócio que o código deve garantir

Todas as regras são validadas no domínio, não só na UI.

**Todos os ajudantes**
- Cada ajudante atua em **um único alvo**: uma pasta, ou a lixeira (só para Limpeza).
- **Nome** definido pelo usuário: obrigatório e sem espaços nas pontas (`HelperName`).
- **Máximo de 20 ajudantes por tipo** (`HelperType.MAX_PER_TYPE`). A checagem acontece só na **criação**; editar um ajudante existente não conta. A contagem vem do repositório (`countByType`) e quem decide é o domínio (`HelperType.assertCanCreate`), lançando `HelperLimitReachedException`.
- **Nome único:** dois ajudantes não podem ter o mesmo nome; maiúsculas e minúsculas não diferenciam (`Organizar Downloads` e `organizar downloads` são o mesmo nome).
- **Sem sobreposição:** dois ajudantes do **mesmo tipo** não podem atuar na mesma pasta (organização e limpeza podem). Para a limpeza, a lixeira conta como um lugar: só um `CLEANER` pode ter `RecycleBinTarget`. A regra vale também para pastas aninhadas: um ajudante do mesmo tipo não pode atuar numa pasta que contém ou está contida na pasta de outro (ex.: `Downloads` e `Downloads\Imagens`).
- **Frequência** (`Schedule`): a cada `intervalDays` dias (≥ 1), em um ou mais horários do dia, sem horários repetidos. Sem limite máximo fixo: quem define é o usuário. O `Schedule` guarda também a **data de início**: o dia em que o ajudante foi salvo ou teve a frequência alterada. É a partir dela que se conta "a cada N dias" (e o `/SD` do agendador).
- **Histórico:** toda execução é registrada (quando rodou e o que fez: arquivos movidos, removidos ou renomeados, lixeira esvaziada) e pode ser consultada. Os registros são guardados por **30 dias**. Falhas de uma execução (arquivo sem permissão, pasta apagada) também entram no histórico (`FAILED`, com a mensagem) e em um log em `%APPDATA%\Tidyfy\logs`. O histórico é **exibido na interface** (um diálogo por ajudante). Ao remover um ajudante, a tarefa dele é **removida do Agendador do Windows**, mas o histórico **não** é apagado: fica até expirar.
- **Pastas sensíveis** são recusadas pelo domínio. Recusadas **com tudo que há dentro**: `C:\Windows` (incluindo `System32`), `C:\Program Files`, `C:\Program Files (x86)` e `C:\ProgramData`. Recusadas **só elas mesmas**: a raiz de qualquer disco e a pasta do usuário (`C:\Users\nome`); as pastas dentro dela, como Downloads, são aceitas.
- Ajudante **desligado** não fica registrado no Agendador do Windows e não executa.

**Organização de pasta**
- Olha apenas o que está diretamente na pasta; **não entra em subpastas**. Arquivos vão para a categoria deles; pastas que o ajudante não criou vão, inteiras, para a subpasta **"Pastas"**.
- As subpastas do próprio ajudante (as categorias, `Pastas` e `Outros`) ficam onde estão; uma pasta do usuário com o mesmo nome de uma categoria conta como do ajudante.
- Arquivos de extensão desconhecida vão para a subpasta **"Outros"**.
- **Conflito de nome no destino:** arquivo duplicado (mesmo conteúdo) → o arquivo de fora vai para a lixeira; senão → o item movido ganha um número no fim do nome (`teste(1).pdf`, `teste(2).pdf`...), inclusive pastas em `Pastas`.
- Cada extensão pertence a **uma única categoria** (evita ambiguidade no destino).
- Extensões são comparadas normalizadas: minúsculas e sem ponto (`Foto.JPG` → `jpg`).
- As categorias são um catálogo padrão (`DefaultFileCategories`), não configuradas por ajudante, e o usuário **não** cria categorias próprias (talvez no futuro). Pastas de destino: `Compactados`, `Documentos`, `Executáveis`, `Imagens`, `Vídeos`, `Pastas` e `Outros`.
- **Extensões por categoria:** `Imagens`: jpg, jpeg, png, gif, bmp, webp, svg, heic, tiff, ico. `Vídeos`: mp4, mkv, avi, mov, wmv, flv, webm, m4v. `Documentos`: pdf, doc, docx, xls, xlsx, ppt, pptx, txt, rtf, odt, ods, odp, csv. `Compactados`: zip, rar, 7z, tar, gz. `Executáveis`: exe, msi, bat. O resto vai para `Outros` (inclusive músicas, que não têm categoria).
- O cálculo de "o que mover para onde" é uma função pura do domínio (`FolderOrganizationPlanner`); a movimentação real fica na infraestrutura.

**Limpeza**
- O alvo (`CleanupTarget`) é a lixeira (`RecycleBinTarget`) **ou** uma pasta (`FolderTarget`).
- Limpar a **lixeira** remove os arquivos de forma **permanente**; limpar uma **pasta** **move** os arquivos para a lixeira.
- Nos dois casos o usuário configura o **quanto limpar**: tudo, ou só o que é mais antigo que X dias. Para pasta, o usuário também escolhe se a idade conta da **criação** ou da **última modificação**; na lixeira, conta do dia em que o arquivo foi para lá.
- A limpeza de pasta atua **só nos arquivos soltos**; subpastas não são tocadas.

**Fora desta rodada:** arquivos em uso (abertos em outro programa ou ainda sendo baixados).

## Testes

- JUnit 5, rodando via Surefire (`mvn test`).
- **Teste manual:** o roteiro de ponta a ponta no Windows (organização, limpezas, histórico, Agendador, instalador e desinstalação) está em [docs/TESTE-MANUAL.md](../docs/TESTE-MANUAL.md); os itens do instalador seguem pendentes de validação.
- **Cobertura:** o JaCoCo (`jacoco-maven-plugin`) mede a cobertura em todo `mvn test`/`mvn clean test` e grava o relatório em `target/site/jacoco/` (`index.html`, `jacoco.csv`, `jacoco.xml`). Só mede, sem limite mínimo que quebre o build. O agente entra pela propriedade `argLine`; as JVMs filhas do teste de concorrência usam `java.class.path` e não herdam o agente. O perfil `package` não é afetado (o `jpackage` não usa `argLine`).
- **CI:** `.github/workflows/ci.yml` roda `mvn -B clean test` em `windows-latest` (JDK 21 Temurin) a cada push e pull request para a `master`, inclusive os testes de tela (os runners Windows têm sessão gráfica), e publica como artefatos os relatórios do Surefire e do JaCoCo. Não roda o perfil `package`.
- A regra de dependência entre camadas é verificada por `ArchitectureTest` (ArchUnit).
- `domain` e `application` são testados sem tocar no disco nem no Windows (use dublês das portas).
- Testes que precisem do sistema de arquivos devem usar diretórios temporários (`@TempDir`) e nunca pastas reais do usuário.
- Código que chama `schtasks.exe`/lixeira real não deve rodar em testes unitários; teste o comando montado e o tratamento da resposta, não a execução real.
- Testes de tela usam `JavaFx` (em `src/test`): ele inicia o toolkit uma vez, executa o teste na thread da interface (`JavaFx.run` / `JavaFx.call`) e monta a tela numa janela não exibida (`JavaFx.mount`). Precisam de sessão gráfica do Windows. Encontre os nós por `fx:id` (`lookup("#saveButton")`). Diálogos (`showAndWait`) e o seletor de pastas não são abertos nos testes.
- Nomes de teste descrevem o comportamento esperado.

## Convenções

- Pacotes e classes seguem a camada a que pertencem (veja a árvore acima).
- Identificadores de código em **inglês**; textos da interface e documentação em **português (pt-BR)**.
- Injeção de dependências manual por construtor, sem framework, a menos que a complexidade passe a justificar.

## Comandos

```bash
mvn clean test       # compila, roda os testes e gera a cobertura (JaCoCo): target/site/jacoco/index.html (e jacoco.csv/jacoco.xml)
mvn javafx:run       # abre a interface
mvn clean package    # gera target/tidyfy-1.0-SNAPSHOT.jar
mvn clean package -Ppackage -DskipTests  # gera o executável: target/dist/Tidyfy/Tidyfy.exe
mvn clean verify -Ppackage,installer -DskipTests  # gera o instalador: target/installer/Tidyfy-1.0.0.msi (exige o WiX Toolset 3.x no PATH)
```

O perfil `package` (fora dele o build não muda) gera um JAR único em `target/jpackage-input/tidyfy.jar` (`maven-shade-plugin`, com os jars do JavaFX para Windows) e chama o `jpackage` do JDK em uso (`exec-maven-plugin`) para montar a **imagem de aplicação** `target/dist/Tidyfy` (~87 MB), com runtime embutido (os módulos ficam em `jpackage.modules` no `pom.xml`; confira com `jdeps --print-module-deps` ao adicionar dependências). Não exige JDK nem JavaFX na máquina. `Tidyfy.exe` é aplicativo GUI (sem console): sem argumentos abre a interface; `Tidyfy.exe --run <id>` executa o ajudante sem carregar o JavaFX; `Tidyfy.exe --remove-tasks` remove todas as tarefas do Tidyfy do Agendador e sai (veja «Instalador e desinstalação»). O perfil `package` não gera instalador.

#### Instalador e desinstalação

- **Modo `--remove-tasks`:** `LaunchArguments` (`REMOVE_TASKS`, só com esse único argumento; com argumentos extras é inválido) → `RemoveTasksRunner` (pacote raiz, sem JavaFX, nos moldes do `HeadlessRunner`) → `RemoveAllTasksUseCase` (`registeredIds()` + `unregister(id)` de cada uma, sem olhar a configuração dos ajudantes). Uma falha numa tarefa não impede as demais; falha ao listar vai para o relatório (`RemoveTasksResult`, id nulo) e nada é removido. Registra no `ExecutionLog` (início, quantidade removida, falhas). Código de saída: **0** sucesso (inclusive nenhuma tarefa), **1** alguma falha. Só o `Main` chama `System.exit`. `helpers.txt` e `history.txt` não são tocados.
- **Perfil `installer`** (só com `-Pinstaller`; não afeta `mvn test` nem `package`): na fase `verify`, depois da imagem do perfil `package`, chama `jpackage --type msi --app-image target/dist/Tidyfy` com menu Iniciar, atalho na área de trabalho, instalação por usuário (`--win-per-user-install`, sem pedir administrador), versão numérica `installer.version` (MSI não aceita `1.0-SNAPSHOT`) e `--win-upgrade-uuid` **fixo** (`installer.upgrade.uuid`; nunca mude, é ele que faz uma nova versão atualizar a instalada). Saída em `target/installer`. **Exige o WiX Toolset 3.x** (`candle.exe` e `light.exe` no PATH): o `jpackage` do JDK 21 não usa o WiX 4+. Sem ícone próprio por enquanto (usa o padrão do jpackage).
- **Desinstalação:** `src/installer/resources/main.wxs` é o modelo do `jpackage` do JDK 21 (extraído de `jdk.jpackage` com `jimage extract`) mais uma `CustomAction` (`TidyfyRemoveTasks`: `Directory="INSTALLDIR"`, `ExeCommand` = `"[INSTALLDIR]Tidyfy.exe" --remove-tasks`, `Return="ignore"`, `Impersonate="yes"`) agendada em `InstallExecuteSequence` logo após `InstallInitialize` quando `REMOVE="ALL" AND NOT UPGRADINGPRODUCTCODE` (desinstalação, não atualização), portanto antes de `RemoveFiles`. Se o JDK mudar, compare com o `main.wxs` novo do JDK. Se a remoção falhar, a desinstalação continua (`ignore`).
- **Não verificado em máquina com WiX:** a geração do MSI, a instalação, a abertura do app instalado e a remoção das tarefas ao desinstalar não foram executadas (o WiX não estava instalado). Estão cobertos por teste apenas o contrato do `pom.xml`, o `main.wxs` bem formado e o modo `--remove-tasks`.
