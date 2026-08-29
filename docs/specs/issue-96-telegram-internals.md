# Refatoração interna do módulo Telegram

## 1. Metadata

**Issue:** #96
**Author:** Bryan Pinheiro
**Date:** 2026-08-29
**Status:** Approved — escopo derivado da issue e autorizado para implementação nesta conversa
**Reviewers:** mantenedor do projeto e revisão de PR

## Context

O módulo `financebot-telegram-bot` concentra interpretação de linguagem natural,
formatação de mensagens, roteamento e handlers de vários fluxos. Atualmente,
`TelegramIntentService` e `TelegramMessageFormatter` são classes grandes, o que
dificulta testar e evoluir um fluxo sem afetar os demais.

A issue #96 pede uma refatoração incremental de organização interna, preservando
o comportamento externo do bot. A implementação deve aproveitar a separação de
parsers já iniciada na issue #95 e não deve antecipar a integração com IA.

## Functional Requirements

- FR-1: O sistema MUST preservar os intents, campos parseados e mensagens de erro existentes para entradas suportadas.
- FR-2: O parsing MUST ser separado por responsabilidade de fluxo, mantendo uma fachada estável para os consumidores atuais.
- FR-3: O parsing de transações, parcelamentos, consultas financeiras e datas MUST ficar isolado em componentes testáveis.
- FR-4: Vocabulário e palavras-chave MUST permanecer separados das regras de decisão.
- FR-5: A formatação MUST ser organizada por fluxo de mensagem sem alterar o texto externo salvo quando um teste existente já permitir a mudança.
- FR-6: Handlers MUST depender de serviços/formatadores do fluxo correspondente, sem assumir detalhes de parsing.
- FR-7: O módulo MUST manter o comportamento dos fluxos de conversa, confirmação, edição, consultas e transações.

## Non-Functional Requirements

- NFR-1: A refatoração MUST NOT alterar contratos HTTP, autenticação interna, Redis ou modelos persistidos.
- NFR-2: A compilação e a suíte existente do bot MUST continuar passando.
- NFR-3: Cada componente extraído MUST possuir testes unitários para caminhos felizes e falhas relevantes.
- NFR-4: A mudança MUST NOT adicionar dependências externas.
- NFR-5: O caminho principal de processamento MUST manter complexidade e latência compatíveis com a implementação atual; não há orçamento de performance disponível, portanto qualquer regressão observável bloqueia a entrega.

## Acceptance Criteria

### AC-1: Preservar parsing (FR-1, FR-2)

Given um texto atualmente suportado, when o bot o processa, then o mesmo `TelegramIntentType` e os mesmos dados parseados são produzidos.

### AC-2: Isolar parsers (FR-3, FR-4)

Given um parser isolado, when recebe entradas válidas, inválidas e ambíguas do seu fluxo, then retorna o resultado esperado sem depender de outro parser concreto para decidir o fluxo.

### AC-3: Preservar formatação (FR-5)

Given cada família de mensagem coberta pelos testes atuais, when o formatter correspondente é usado, then o texto gerado permanece compatível.

### AC-4: Preservar handlers (FR-6, FR-7)

Given cada handler existente, when executa seu fluxo normal e seus erros conhecidos, then as chamadas ao backend e as respostas ao usuário permanecem compatíveis.

### AC-5: Manter build e contratos (NFR-1, NFR-2)

Given o módulo após a refatoração, when `./mvnw clean verify` é executado no bot, then a compilação e os testes passam sem alteração de contrato.

### AC-6: Cobrir componentes extraídos (NFR-3)

Given cada componente novo ou extraído, when sua suíte unitária é executada, then os cenários principais e de falha estão cobertos.

## Edge Cases

- EC-1: texto nulo, vazio ou somente espaços continua resultando em `UNKNOWN`.
- EC-2: texto que não pertence a nenhum fluxo não deve gerar transação ou consulta.
- EC-3: mensagens com parcelas, datas, contas ou valores incompletos continuam seguindo o fluxo de confirmação/pendência existente.
- EC-4: falha do backend continua sendo convertida pelo mapeador atual, sem vazar detalhes técnicos ao usuário.
- EC-5: contexto ausente ou expirado continua sendo tratado pelo fluxo atual de conversa.

## API Contracts

N/A — esta issue não altera endpoints, payloads HTTP, headers, autenticação ou
contratos entre API e bot.

## Data Models

N/A — não há alteração de banco, Redis, migrations ou DTOs de contrato.

## Out of Scope

- OS-1: integração com OpenAI ou qualquer outra IA.
- OS-2: novas intenções ou novos comandos financeiros.
- OS-3: alteração do contrato com o backend.
- OS-4: alteração do Redis ou do modelo de contexto.
- OS-5: novas funcionalidades financeiras.
- OS-6: reescrita completa do módulo em uma única etapa; a organização pode evoluir em PRs incrementais.
