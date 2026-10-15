# Tidyfy

Seu computador organizado sem você precisar lembrar disso.

O Tidyfy é um aplicativo para **Windows** que cria **ajudantes**: pequenos robôs que cuidam, de tempos em tempos, das tarefas chatas de limpeza e organização da sua máquina. Você cria o ajudante, dá um nome a ele, diz onde e quando ele deve trabalhar, e ele faz o resto sozinho.

> **Status:** o Tidyfy já funciona de ponta a ponta no Windows: pela interface dá para criar, editar, ligar, desligar e remover ajudantes, que são agendados no Agendador de Tarefas, executam sozinhos e deixam um histórico. Ainda falta o empacotamento: por enquanto o aplicativo roda a partir do código-fonte (Maven), sem executável nem instalador. Este documento descreve o que o Tidyfy faz e as regras que ele segue.

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
- **Nome definido pelo usuário.** Cada ajudante tem um nome que descreve o que ele faz, por exemplo *"Organizar Downloads"* ou *"Esvaziar lixeira toda semana"*. Dois ajudantes não podem ter o mesmo nome.
- **Sem sobreposição.** Dois ajudantes do **mesmo tipo** não podem trabalhar na mesma pasta. Já um ajudante de organização e um de limpeza podem cuidar da mesma pasta. Na limpeza, a lixeira também conta como um lugar: só um ajudante de limpeza pode cuidar dela. Isso vale também para pastas uma dentro da outra: não pode haver um ajudante em `Downloads` e outro do mesmo tipo em `Downloads\Imagens`.
- **Até 20 ajudantes de cada tipo.** São até 20 ajudantes de organização e até 20 de limpeza. Ao atingir o limite, é preciso remover um ajudante daquele tipo para criar outro.
- **Liga e desliga.** Qualquer ajudante pode ser desligado sem ser apagado, e volta a trabalhar quando for ligado de novo.
- **Frequência própria.** Cada ajudante tem sua própria rotina: a cada quantos dias ele trabalha e em quais horários do dia. Tudo isso é configurado pelo usuário.
- **Histórico.** Cada execução é registrada: quando o ajudante rodou e o que ele fez. O usuário vê esse histórico na própria interface, e ele é guardado por **30 dias**, mesmo que o ajudante seja removido. Ao remover um ajudante, a tarefa dele também é removida do Agendador do Windows.
- **Pastas sensíveis são recusadas.** O Tidyfy não deixa escolher estas pastas: `C:\Windows` (incluindo `System32`), `C:\Program Files`, `C:\Program Files (x86)`, `C:\ProgramData`, a raiz de qualquer disco (como `C:\`) e a pasta do usuário em si (`C:\Users\nome`; as pastas dentro dela, como Downloads, podem).

### Ajudante de Organização de pasta

Pega os arquivos soltos em uma pasta e os separa em subpastas de acordo com o tipo de cada um. As pastas soltas também são arrumadas: vão todas para a subpasta **Pastas**.

**Exemplo:** sua pasta *Downloads* tem imagens, documentos, instaladores e vídeos misturados. Depois que o ajudante passa por ali, ela fica assim:

```
Downloads/
├── Compactados/
├── Documentos/
├── Executáveis/
├── Imagens/
├── Vídeos/
├── Pastas/
└── Outros/
```

As categorias são fixas (o usuário não cria as suas, talvez no futuro). Qualquer arquivo que não se encaixe em uma delas vai para **Outros**, e toda pasta que não foi criada nem é gerenciada pelo ajudante vai para **Pastas**.

**O que o usuário configura:**

- **Nome do ajudante.**
- **Qual pasta organizar** (apenas uma).
- **Com que frequência o ajudante trabalha.**
- **Se o ajudante está ligado ou não.**

**Regras importantes:**

- O ajudante só olha o que está diretamente na pasta escolhida: os arquivos vão para a categoria deles e as pastas vão para **Pastas**. Ele não entra nas pastas: o que está dentro delas não é tocado, e elas são movidas inteiras.
- As subpastas do próprio ajudante (as categorias, **Pastas** e **Outros**) ficam onde estão.
- Arquivos de um tipo que o ajudante não conhece vão para **"Outros"**, uma pasta separada de todas as demais.
- Se já existir um arquivo com o mesmo nome na pasta de destino:
  - se for um **duplicado** (o mesmo arquivo), o arquivo de fora vai para a lixeira;
  - senão, o arquivo que está sendo movido ganha um número no fim do nome, como `teste(1).pdf`, `teste(2).pdf`... (vale também para pastas em **Pastas**).

### Ajudante de Limpeza

Faz a faxina de um lugar, para ele não acumular arquivos esquecidos ocupando espaço. O usuário escolhe **o que** será limpo:

- **A lixeira do Windows**, que é esvaziada: os arquivos são **removidos permanentemente**; ou
- **Uma pasta**, cujos arquivos são **movidos para a lixeira** (por exemplo, uma pasta de arquivos temporários). Assim ainda dá para recuperá-los até a lixeira ser esvaziada.

Nos dois casos o usuário escolhe **quanto limpar**: tudo, ou só o que está lá há mais de X dias. Na limpeza de pasta, só os **arquivos soltos** são limpos: as subpastas não são tocadas.

**O que o usuário configura:**

- **Nome do ajudante.**
- **O que limpar:** a lixeira ou uma pasta (apenas uma).
- **Quanto limpar:** tudo, ou só o que é mais antigo que X dias. Para uma pasta, o usuário também escolhe se a idade conta a partir da **criação** ou da **última modificação** do arquivo; na lixeira, conta a partir do dia em que o arquivo foi para lá.
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

## Ideias para o futuro

Ideias para as próximas rodadas, como novos tipos de ajudante:

- Avisar quando o disco estiver ficando cheio.
- Tratar arquivos em uso (abertos em outro programa ou ainda sendo baixados).

---

## Para quem vai desenvolver

Informações técnicas (arquitetura, tecnologias, padrões e comandos) ficam no arquivo [CLAUDE.md](.claude/CLAUDE.md).

Para gerar o executável (sem precisar de Java instalado): `mvn clean package -Ppackage -DskipTests`; o resultado fica em `target/dist/Tidyfy/Tidyfy.exe`.
