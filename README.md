# Tidyfy

Seu computador organizado sem você precisar lembrar disso.

O Tidyfy é um aplicativo para **Windows** que cria **ajudantes**: pequenos robôs que cuidam, de tempos em tempos, das tarefas chatas de limpeza e organização da sua máquina. Você cria o ajudante, dá um nome a ele, diz onde e quando ele deve trabalhar, e ele faz o resto sozinho.

> **Status:** projeto em fase inicial. Este documento descreve o que o Tidyfy se propõe a fazer e as regras que ele deve seguir.

---

## A ideia

Todo mundo tem tarefas de manutenção que sabe que deveria fazer, mas sempre deixa para depois: a pasta de Downloads virando uma bagunça, a lixeira cheia de coisa esquecida...

No Tidyfy, cada uma dessas tarefas vira um **ajudante**. Você configura uma vez, define a frequência e pode esquecer. O ajudante roda no horário combinado, mesmo que o Tidyfy esteja fechado.

Tudo é configurado por uma **interface gráfica simples**, sem precisar mexer em arquivos ou linhas de comando.

---

## Os ajudantes

O usuário cria quantos ajudantes quiser (dentro do limite abaixo), escolhendo o **tipo** de cada um. Hoje existem dois tipos:

| Tipo | O que faz |
|---|---|
| **Organização de pasta** | Separa os arquivos de uma pasta em subpastas por tipo de arquivo |
| **Limpeza** | Esvazia a lixeira do Windows **ou** limpa uma pasta escolhida |

### Regras que valem para todos os ajudantes

- **Uma pasta por ajudante.** Cada ajudante cuida de um único lugar. Quer organizar *Downloads* e *Área de Trabalho*? Crie dois ajudantes.
- **Nome definido pelo usuário.** Cada ajudante tem um nome que descreve o que ele faz, por exemplo *"Organizar Downloads"* ou *"Esvaziar lixeira toda semana"*.
- **Até 20 ajudantes de cada tipo.** São até 20 ajudantes de organização e até 20 de limpeza. Ao atingir o limite, é preciso remover um ajudante daquele tipo para criar outro.
- **Liga e desliga.** Qualquer ajudante pode ser desligado sem ser apagado, e volta a trabalhar quando for ligado de novo.
- **Frequência própria.** Cada ajudante tem sua própria rotina: a cada quantos dias ele trabalha e em quais horários do dia.

### Ajudante de Organização de pasta

Pega os arquivos soltos em uma pasta e os separa em subpastas de acordo com o tipo de cada um.

**Exemplo:** sua pasta *Downloads* tem fotos, PDFs, instaladores e planilhas misturados. Depois que o ajudante passa por ali, ela fica assim:

```
Downloads/
├── Fotos/
├── PDFs/
├── Executáveis/
├── Planilhas/
└── Outros/
```

**O que o usuário configura:**

- **Nome do ajudante.**
- **Qual pasta organizar** (apenas uma).
- **Com que frequência o ajudante trabalha.**
- **Se o ajudante está ligado ou não.**

**Regras importantes:**

- O ajudante só organiza os arquivos que estão diretamente na pasta escolhida. O que já estiver dentro de outras pastas não é tocado.
- Arquivos de um tipo que o ajudante não conhece vão para **"Outros"**, uma pasta separada de todas as demais.

### Ajudante de Limpeza

Faz a faxina de um lugar, para ele não acumular arquivos esquecidos ocupando espaço. O usuário escolhe **o que** será limpo:

- **A lixeira do Windows**, que é esvaziada; ou
- **Uma pasta**, cujos arquivos são removidos (por exemplo, uma pasta de arquivos temporários).

**O que o usuário configura:**

- **Nome do ajudante.**
- **O que limpar:** a lixeira ou uma pasta (apenas uma).
- **Com que frequência a limpeza acontece.**
- **Se o ajudante está ligado ou não.**

---

## Como funciona na prática

1. O usuário abre o Tidyfy e vê a lista dos seus ajudantes.
2. Clica para criar um novo ajudante e escolhe o tipo: **organização** ou **limpeza**.
3. Dá um nome ao ajudante, escolhe a pasta (ou a lixeira) e define a frequência.
4. Salva. A partir daí, o ajudante passa a trabalhar sozinho nos horários definidos.

Na lista, o usuário pode ligar, desligar, editar ou remover qualquer ajudante.

O trabalho dos ajudantes é agendado através do **Agendador de Tarefas do Windows**. Por isso o Tidyfy **funciona somente no Windows**, e os ajudantes continuam trabalhando mesmo com o aplicativo fechado.

---

## Pontos para definir

Algumas regras ainda não foram decididas e merecem atenção antes da implementação:

- **Arquivos com o mesmo nome:** se já existir um arquivo com o mesmo nome na pasta de destino, o ajudante deve renomear, pular ou perguntar?
- **Arquivos em uso:** o que fazer com arquivos abertos em outro programa ou ainda sendo baixados? (O ideal é deixar para a próxima rodada.)
- **Tipos de arquivo padrão:** quais categorias e extensões já vêm prontas, e se o usuário poderá criar as suas.
- **Frequências disponíveis:** qual o intervalo máximo de dias e quantas vezes por dia um ajudante pode rodar.
- **Nomes repetidos:** dois ajudantes podem ter o mesmo nome?
- **Mesma pasta em mais de um ajudante:** pode existir, por exemplo, um ajudante organizando e outro limpando a mesma pasta?
- **Limpeza de pasta:** os arquivos vão para a lixeira (dá para recuperar) ou são apagados de vez? E as subpastas, também são limpas?
- **Limpeza da lixeira:** apagar tudo ou só o que está lá há mais de X dias?
- **Histórico:** se o usuário poderá ver o que cada ajudante já fez (quais arquivos foram movidos, quando a lixeira foi esvaziada).
- **Segurança:** se o ajudante deve recusar pastas sensíveis do sistema (como `C:\Windows` ou `Program Files`).

---

## Ideias para o futuro

Novos tipos de ajudante podem ser adicionados com o tempo, por exemplo:

- Limpar só arquivos mais antigos que X dias.
- Avisar quando o disco estiver ficando cheio.

---

## Para quem vai desenvolver

Informações técnicas (arquitetura, tecnologias, padrões e comandos) ficam no arquivo [CLAUDE.md](CLAUDE.md).
