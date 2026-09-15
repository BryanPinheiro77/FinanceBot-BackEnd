# Especificação — Issue #140: detectar gasto fora do padrão

## 1. Metadata

- **Status:** Approved — escopo derivado da issue #119 e autorizado para implementação nesta conversa
- **Autor:** Bryan Pinheiro / Codex
- **Data:** 2026-09-14
- **Issue:** #140 (subtask da #119)
- **Revisores:** pendente no pull request

## Context

A issue #119 precisa identificar despesas que merecem uma explicação antes de
serem enviadas pelo Telegram. A primeira fatia deve produzir uma evidência
determinística e legível; entrega, deduplicação e preferências do usuário serão
tratadas nas subtasks seguintes.

Para evitar alertas instáveis durante um mês incompleto, a análise usa o último
mês fechado. A referência é a média da mesma categoria nos três meses fechados
anteriores. Sem três meses de histórico, não há evidência suficiente para
classificar o gasto como atípico.

## Functional Requirements

- **FR-1:** O detector MUST analisar somente transações do tipo `EXPENSE`.
- **FR-2:** O período observado MUST ser o último mês fechado em relação ao relógio injetado.
- **FR-3:** A referência MUST usar a média mensal da mesma categoria nos três meses fechados anteriores.
- **FR-4:** Uma categoria MUST gerar candidato somente quando o histórico tiver três meses e o valor observado for pelo menos 50% maior que a média, com diferença mínima de R$ 100,00.
- **FR-5:** O resultado MUST explicar categoria, período observado, valor observado, média histórica e percentual de variação.
- **FR-6:** O detector MUST retornar resultados ordenados pela maior variação percentual e limitar a três categorias.
- **FR-7:** A regra MUST ser independente de Telegram, RabbitMQ, OpenAI e persistência de notificações.

## Non-Functional Requirements

- **NFR-1:** Valores monetários MUST usar `BigDecimal`.
- **NFR-2:** O relógio MUST ser injetável para permitir testes determinísticos.
- **NFR-3:** A consulta MUST filtrar por usuário, período e tipo no banco; o serviço não deve carregar transações fora da janela analisada.
- **NFR-4:** Nenhuma migration ou alteração de contrato HTTP é necessária nesta fatia.

## Acceptance Criteria

### AC-1: detectar categoria acima do padrão (FR-1, FR-2, FR-3, FR-4)

**Given:** três meses anteriores com média de R$ 200,00 e o último mês fechado com R$ 350,00 na categoria.
**When:** o detector é executado.
**Then:** retorna um candidato para essa categoria, pois o aumento é de 75% e R$ 150,00.

### AC-2: explicar o resultado (FR-5)

**Given:** um candidato detectado.
**When:** seus dados são consultados.
**Then:** categoria, mês observado, valor atual, média, percentual e explicação textual estão preenchidos.

### AC-3: não alertar sem histórico suficiente ou sem limiar (FR-3, FR-4)

**Given:** menos de três meses de histórico, aumento inferior a 50% ou diferença inferior a R$ 100,00.
**When:** o detector é executado.
**Then:** não retorna candidato para a categoria.

### AC-4: ordenar e limitar resultados (FR-6)

**Given:** mais de três categorias acima do limiar.
**When:** o detector é executado.
**Then:** retorna no máximo três resultados, do maior para o menor percentual.

### AC-5: isolar a análise (FR-7, NFR-4)

**Given:** execução do detector.
**When:** a regra é avaliada.
**Then:** nenhuma chamada a Telegram, RabbitMQ, OpenAI ou gravação de notificação ocorre.

## Edge Cases

- **EC-1:** Valor histórico igual a zero não gera percentual de variação nem candidato.
- **EC-2:** Categorias sem transações em um dos três meses de referência não têm histórico completo e não geram candidato.
- **EC-3:** Valores nulos retornados pelo repository devem ser tratados como zero ou ignorados sem lançar erro.
- **EC-4:** Usuário nulo deve ser rejeitado com `IllegalArgumentException`.
- **EC-5:** Datas futuras ficam fora da janela fechada analisada.

## API Contracts

Não há endpoint HTTP nesta subtask. O contrato interno é:

```java
List<AtypicalExpenseAlert> detect(User user)
```

## Data Models

`AtypicalExpenseAlert` é um record imutável em memória:

| Campo | Tipo | Restrições |
| --- | --- | --- |
| categoryName | String | obrigatório |
| observedMonth | YearMonth | último mês fechado |
| observedAmount | BigDecimal | >= 0 |
| historicalAverage | BigDecimal | > 0 |
| variationPercentage | BigDecimal | percentual com 2 casas |
| explanation | String | texto em português |

## Out of Scope

- **OS-1:** Persistir alertas ou controlar duplicidade.
- **OS-2:** Preferências de ativação, frequência ou opt-out.
- **OS-3:** Enviar mensagens pelo Telegram ou RabbitMQ.
- **OS-4:** Detectar parcelas, orçamento apertado ou gerar resumos.
- **OS-5:** Criar endpoint público.
