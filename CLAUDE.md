# CLAUDE.md

Guia técnico do Tidyfy. Para o contexto de negócio, veja o [README.md](README.md).

## Visão geral

Aplicativo **somente Windows** que cria "ajudantes": tarefas de limpeza/organização que rodam periodicamente através do **Agendador de Tarefas do Windows**. O usuário configura os ajudantes por uma interface JavaFX.

O usuário cria ajudantes escolhendo um **tipo** e dando um **nome**. Tipos iniciais:

1. **Organização de pasta** (`FOLDER_ORGANIZER`): move os arquivos de **uma** pasta para subpastas por tipo (fotos, PDFs, executáveis...).
2. **Limpeza** (`CLEANER`): esvazia a lixeira do Windows **ou** remove os arquivos de **uma** pasta.

Todo ajudante tem: nome, tipo, frequência (a cada N dias, em um ou mais horários do dia) e estado ligado/desligado.

## Tecnologias

| Item | Escolha |
|---|---|
| Linguagem | Java 21 (JDK 21) |
| Build | Maven (3.8+) |
| Interface | JavaFX |
| Testes | JUnit 5 (adicionar Mockito quando houver necessidade de dublês) |
| Agendamento | Agendador de Tarefas do Windows (`schtasks.exe`) |
| Plataforma | Windows apenas |

Estado atual do `pom.xml`: Java 21 + JUnit 5. **JavaFX ainda não foi adicionado**; incluir as dependências `org.openjfx` (e o `javafx-maven-plugin`) ao começar a camada de apresentação.

## Regras do projeto (obrigatórias)

1. **Tudo que for implementado deve ter testes unitários.** Código novo sem teste não está pronto. Bug corrigido ganha um teste que o reproduz.
2. **Arquitetura limpa** (Clean Architecture). A regra de dependência nunca pode ser quebrada (veja abaixo).
3. **Somente Windows.** Código que depende do Windows deve ficar isolado na camada de infraestrutura, atrás de interfaces, para que o restante seja testável em qualquer ambiente.
4. Mantenha o código simples: sem abstrações ou configurações que nenhuma regra de negócio atual exija.

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
└── presentation            Interface JavaFX (views, controllers)
```

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

Os diagramas PlantUML em [docs/diagrams](docs/diagrams) são a referência do design planejado:

| Arquivo | Conteúdo |
|---|---|
| [01-layers.puml](docs/diagrams/01-layers.puml) | Camadas e regra de dependência |
| [02-domain.puml](docs/diagrams/02-domain.puml) | Ajudantes, tipos e limite, alvo da limpeza, agendamento, categorias e planejador de organização |
| [03-application.puml](docs/diagrams/03-application.puml) | Casos de uso e portas |
| [04-infrastructure.puml](docs/diagrams/04-infrastructure.puml) | Implementações das portas (schtasks, disco, lixeira, persistência) |
| [05-presentation.puml](docs/diagrams/05-presentation.puml) | Telas JavaFX, `Main` e `AppContainer` |

**Mantenha os diagramas atualizados:** ao criar, renomear ou remover classes/relacionamentos relevantes, atualize o `.puml` correspondente no mesmo commit.

### Portas previstas

| Porta (application) | Responsabilidade | Implementação (infrastructure) |
|---|---|---|
| Agendador de tarefas | Criar, atualizar, pausar e remover tarefas agendadas | Wrapper do `schtasks.exe` |
| Sistema de arquivos | Listar, mover e descartar arquivos | `java.nio.file` (descarte: a definir, ex.: `Desktop.moveToTrash`) |
| Lixeira | Esvaziar a lixeira do Windows | A definir (ex.: PowerShell `Clear-RecycleBin` ou chamada nativa) |
| Repositório de ajudantes | Salvar/carregar a configuração dos ajudantes e contar por tipo | A definir (ex.: arquivo JSON em `%APPDATA%\Tidyfy`) |

## Funcionamento

1. O usuário cria/edita um ajudante na interface JavaFX.
2. A configuração é salva e uma tarefa correspondente é registrada no Agendador do Windows.
3. No horário agendado, o Windows executa o Tidyfy em **modo sem interface**, informando qual ajudante rodar (por argumento de linha de comando).
4. O Tidyfy carrega a configuração do ajudante, executa o caso de uso e encerra.

Por isso a execução dos ajudantes **não pode depender da interface gráfica**: o `Main` deve distinguir "abrir a interface" de "executar o ajudante X".

## Regras de negócio que o código deve garantir

Todas as regras são validadas no domínio, não só na UI.

**Todos os ajudantes**
- Cada ajudante atua em **um único alvo**: uma pasta, ou a lixeira (só para Limpeza).
- **Nome** definido pelo usuário: obrigatório e sem espaços nas pontas (`HelperName`).
- **Máximo de 20 ajudantes por tipo** (`HelperType.MAX_PER_TYPE`). A checagem acontece só na **criação**; editar um ajudante existente não conta. A contagem vem do repositório (`countByType`) e quem decide é o domínio (`HelperType.assertCanCreate`), lançando `HelperLimitReachedException`.
- **Frequência** (`Schedule`): a cada `intervalDays` dias (≥ 1), em um ou mais horários do dia, sem horários repetidos.
- Ajudante **desligado** não fica registrado no Agendador do Windows e não executa.

**Organização de pasta**
- Organiza apenas arquivos diretamente na pasta; **não entra em subpastas**.
- Arquivos de extensão desconhecida vão para a subpasta **"Outros"**.
- Cada extensão pertence a **uma única categoria** (evita ambiguidade no destino).
- Extensões são comparadas normalizadas: minúsculas e sem ponto (`Foto.JPG` → `jpg`).
- As categorias são um catálogo padrão (`DefaultFileCategories`), não configuradas por ajudante.
- O cálculo de "o que mover para onde" é uma função pura do domínio (`FolderOrganizationPlanner`); a movimentação real fica na infraestrutura.

**Limpeza**
- O alvo (`CleanupTarget`) é a lixeira (`RecycleBinTarget`) **ou** uma pasta (`FolderTarget`).

**Em aberto**: conflito de nome de arquivo, arquivo em uso, nomes de ajudante repetidos, mesma pasta em mais de um ajudante, limpeza de pasta (lixeira ou exclusão definitiva, subpastas), limites da frequência, pastas protegidas e histórico. Estão listados no README; **decida e registre aqui antes de implementar**.

## Testes

- JUnit 5, rodando via Surefire (`mvn test`).
- `domain` e `application` são testados sem tocar no disco nem no Windows (use dublês das portas).
- Testes que precisem do sistema de arquivos devem usar diretórios temporários (`@TempDir`) e nunca pastas reais do usuário.
- Código que chama `schtasks.exe`/lixeira real não deve rodar em testes unitários; teste o comando montado e o tratamento da resposta, não a execução real.
- Nomes de teste descrevem o comportamento esperado.

## Convenções

- Pacotes e classes seguem a camada a que pertencem (veja a árvore acima).
- Identificadores de código em **inglês**; textos da interface e documentação em **português (pt-BR)**.
- Injeção de dependências manual por construtor, sem framework, a menos que a complexidade passe a justificar.

## Comandos

```bash
mvn clean test       # compila e roda os testes
mvn clean package    # gera target/tidyfy-1.0-SNAPSHOT.jar
```
