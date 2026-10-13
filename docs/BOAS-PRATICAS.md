# Boas práticas de código

Regras de escrita de código do Tidyfy. São **obrigatórias**: código novo que não as siga não está pronto. Para a arquitetura e as regras de negócio, veja o [CLAUDE.md](../.claude/CLAUDE.md).

## 1. Documentação

Todo código de produção (`src/main`) deve ser documentado com Javadoc. A documentação deve ser **simples, direta e completa**: diz o que a coisa faz e, quando não for óbvio, por quê. Não repete o nome do elemento nem narra o código linha a linha.

### Classes, interfaces, enums, records e métodos

Bloco de várias linhas, com o texto na linha seguinte à abertura:

```java
/**
 * Calcula quais arquivos da pasta vão para cada categoria.
 */
public final class FolderOrganizationPlanner {

    /**
     * Devolve a subpasta de destino da extensão informada.
     * Extensões desconhecidas vão para "Outros".
     */
    public String destinationOf(String extension) { ... }
}
```

Em métodos, descreva também os parâmetros, o retorno e as exceções quando isso não estiver claro pelo nome (`@param`, `@return`, `@throws`).

### Variáveis (atributos e constantes)

Bloco de **uma linha**:

```java
/** Ajudantes exibidos na lista. */
private final List<HelperItem> helpers = new ArrayList<>();
```

### Quais elementos documentar

- Todo tipo (classe, interface, enum, record), inclusive os internos.
- Todo método e construtor, de qualquer visibilidade.
- Todo atributo e toda constante.
- Variáveis locais não precisam de Javadoc.

## 2. Sem Strings soltas

Nenhuma `String` literal pode aparecer solta no código. Toda `String` é atribuída a uma **constante da própria classe**, e o código usa a constante.

```java
// Errado
setAccessibleText(used.get() + " de " + max.get() + " ajudantes criados");

// Certo
setAccessibleText(String.format(ACCESSIBLE_TEXT, used.get(), max.get()));
```

Vale para qualquer texto: mensagens da interface, mensagens de exceção, classes CSS, `fx:id`, caminhos de recursos, argumentos de linha de comando, comandos do `schtasks.exe` etc.

- A constante fica na classe que a usa. Não crie classes genéricas de constantes para reaproveitar texto entre classes.
- Texto com valores variáveis usa um modelo na constante (`String.format`, `MessageFormat`), não concatenação de literais.
- Nome da constante em `UPPER_SNAKE_CASE`, descrevendo o papel do texto, não o conteúdo (`ACCESSIBLE_TEXT`, não `DE_AJUDANTES_CRIADOS`).
- Constantes `String` são `private static final`, a menos que outra classe precise delas.

## 3. Ordem dos membros de uma classe

As constantes ficam **depois do último atributo**, separadas por um comentário `// Constantes`:

```java
public final class UsageMeter extends HBox {

    /** Quantidade de vagas ocupadas. */
    private final IntegerProperty used = new SimpleIntegerProperty(this, "used");

    /** Quantidade total de vagas. */
    private final IntegerProperty max = new SimpleIntegerProperty(this, "max", HelperKind.LIMIT_PER_TYPE);

    // Constantes

    /** Classe CSS do medidor. */
    private static final String STYLE_CLASS = "usage-meter";

    /** Modelo do texto de acessibilidade: ocupadas e total. */
    private static final String ACCESSIBLE_TEXT = "%d de %d ajudantes criados";

    public UsageMeter() { ... }
}
```

Ordem geral: atributos de instância, `// Constantes`, construtores, métodos.

- Em enums, as constantes do enum vêm primeiro, como a linguagem exige; as demais constantes seguem a regra acima, depois dos atributos.
- Em classes sem atributos de instância, `// Constantes` vem logo no início do corpo.

## 4. Testes

Testes (`src/test`) seguem a regra de documentação das classes (Javadoc no tipo). Os nomes dos métodos descrevem o comportamento esperado (veja o CLAUDE.md), então o Javadoc por método e a constante para cada `String` não são exigidos nos testes.

## Checklist antes de concluir uma tarefa

- [ ] Todo tipo, método, construtor, atributo e constante novo tem Javadoc no formato certo.
- [ ] Não há `String` literal solta; todas estão em constantes da própria classe.
- [ ] As constantes estão depois do último atributo, sob `// Constantes`.
- [ ] Os testes passam (`mvn clean test`).
