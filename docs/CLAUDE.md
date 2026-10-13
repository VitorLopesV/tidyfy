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

Estado atual do `pom.xml`: Java 21, JavaFX 21 (`javafx-controls` e `javafx-fxml`, mais o `javafx-maven-plugin` para rodar a interface) e JUnit 5.

## Regras do projeto (obrigatórias)

1. **Tudo que for implementado deve ter testes unitários.** Código novo sem teste não está pronto. Bug corrigido ganha um teste que o reproduz.
2. **Arquitetura limpa** (Clean Architecture). A regra de dependência nunca pode ser quebrada (veja abaixo).
3. **Somente Windows.** Código que depende do Windows deve ficar isolado na camada de infraestrutura, atrás de interfaces, para que o restante seja testável em qualquer ambiente.
4. Mantenha o código simples: sem abstrações ou configurações que nenhuma regra de negócio atual exija.
5. **Siga as [boas práticas de código](BOAS-PRATICAS.md).** Resumo: todo tipo, método, construtor, atributo e constante tem Javadoc (blocos de várias linhas para tipos e métodos, `/** Texto */` para variáveis); nenhuma `String` solta, todas em constantes da própria classe; as constantes ficam depois do último atributo, sob o comentário `// Constantes`.

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
└── presentation            Interface JavaFX: TidyfyApplication, controllers das telas e diálogos
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

Os diagramas PlantUML em [docs/diagrams](diagrams) são a referência do design planejado:

| Arquivo | Conteúdo |
|---|---|
| [01-layers.puml](diagrams/01-layers.puml) | Camadas e regra de dependência |
| [02-domain.puml](diagrams/02-domain.puml) | Ajudantes, tipos e limite, alvo e idade da limpeza, agendamento, regras de nome e pasta, pastas sensíveis, categorias, planejador de organização e histórico |
| [03-application.puml](diagrams/03-application.puml) | Casos de uso (incluindo execução e histórico) e portas |
| [04-infrastructure.puml](diagrams/04-infrastructure.puml) | Implementações das portas (schtasks, disco, lixeira, persistência das configurações e do histórico) |
| [05-presentation.puml](diagrams/05-presentation.puml) | Telas JavaFX, modelos de tela, componentes, `Main` e `AppContainer` |

**Mantenha os diagramas atualizados:** ao criar, renomear ou remover classes/relacionamentos relevantes, atualize o `.puml` correspondente no mesmo commit.

### Portas previstas

| Porta (application) | Responsabilidade | Implementação (infrastructure) |
|---|---|---|
| Agendador de tarefas | Criar, atualizar, pausar e remover tarefas agendadas | Wrapper do `schtasks.exe` |
| Sistema de arquivos | Listar o que há na pasta, comparar conteúdo, mover e enviar à lixeira | `java.nio.file` (lixeira: `Desktop.moveToTrash`) |
| Lixeira | Esvaziar a lixeira do Windows, tudo ou só o que está lá há mais de X dias | A definir (ex.: PowerShell `Clear-RecycleBin` ou chamada nativa) |
| Repositório de ajudantes | Salvar/carregar a configuração dos ajudantes e contar por tipo | Arquivo `%APPDATA%\Tidyfy\helpers.txt` (JSON) |
| Histórico de execuções | Registrar e consultar quando cada ajudante rodou e o que fez | Arquivo `%APPDATA%\Tidyfy\history.txt` (JSON) |

### Decisões técnicas

- **Persistência:** configurações em `helpers.txt` e histórico em `history.txt`, ambos em `%APPDATA%\Tidyfy`, com extensão `.txt` e conteúdo em JSON. A gravação é atômica (arquivo temporário e depois mover).
- **Agendador:** uma única tarefa por ajudante, definida em **XML**, com um gatilho por horário. Roda só com o usuário logado (precisa das pastas e da lixeira dele), permite rodar na bateria e executa assim que possível quando perdeu o horário.
- **Falha ao salvar:** o Tidyfy tenta registrar no agendador primeiro; se o agendador falhar, a configuração **não** é salva e o erro aparece na tela.
- **Falha ao executar:** vai para o histórico (`FAILED`) e para o log.

## Funcionamento

1. O usuário cria/edita um ajudante na interface JavaFX.
2. A configuração é salva e uma tarefa correspondente é registrada no Agendador do Windows.
3. No horário agendado, o Windows executa o Tidyfy em **modo sem interface**, informando qual ajudante rodar (por argumento de linha de comando).
4. O Tidyfy carrega a configuração do ajudante, executa o caso de uso e encerra.

Por isso a execução dos ajudantes **não pode depender da interface gráfica**: o `Main` deve distinguir "abrir a interface" de "executar o ajudante X".

## Interface (presentation)

- **Telas em FXML sem `fx:controller`.** O controller é criado no código e entregue ao `FXMLLoader` por `Fxml.load` (componentes `fx:root` usam `Fxml.loadRoot`). Assim os casos de uso poderão ser injetados por construtor.
- **Tema escuro único** em `tidyfy.css`. As cores são *looked-up colors* no `.root` (`-tf-bg`, `-tf-ink`, `-tf-org`, `-tf-clean`...); FXML e Java não usam cores fixas, só classes CSS. A cor de cada tipo vem das classes `kind-org` / `kind-clean`.
- **Fontes do Windows:** Segoe UI (texto) e Consolas (caminhos, horários e números). Nada é embutido no jar.
- **Regras de tela:** editar só com **um** ajudante selecionado; remover com um ou vários (`HelperSelection`). O tipo que atingiu o limite aparece bloqueado em "Novo ajudante", mas quem garante o limite é o domínio.
- **A tela principal não rola.** Cabeçalho, contadores e filtro ficam fixos e os cartões (compactos) ocupam o espaço restante: na janela padrão (1280 × 900) cabem 9 sem barra de rolagem. Só a área dos cartões (`cardsScroll`) rola, e apenas quando há mais ajudantes do que cabem. Com seleção, as ações aparecem **no lugar do filtro**, na mesma linha, para não cobrir nem empurrar os cartões. Os testes de `HelperListControllerTest` verificam isso; ao mexer no layout, mantenha-os passando.
- **Diálogos** (`Dialogs.showModal`): janela modal sem moldura, arrastável pelo nó com a classe `drag-handle`, fecha com Esc e escurece a lista enquanto está aberta.
- **Estado atual:** os casos de uso ainda não existem. A lista começa vazia (`HelperListController.showHelpers` recebe os dados quando o `ListHelpersUseCase` existir); Salvar e Remover só fecham os diálogos; o `Main` só abre a interface (o modo `--run {id}` chega com o `RunHelperUseCase`).

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
mvn clean test       # compila e roda os testes
mvn javafx:run       # abre a interface
mvn clean package    # gera target/tidyfy-1.0-SNAPSHOT.jar
```
