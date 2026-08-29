# Especificação — Issue #94: integração Telegram em arquitetura hexagonal

## 1. Metadata

- **Status:** Approved — escopo derivado da issue #94 e autorizado para implementação nesta conversa
- **Autor:** Bryan Pinheiro / Codex
- **Data:** 2026-08-29
- **Issue:** #94
- **Revisores:** pendente no pull request

## Context

O backend expõe a integração usada pelo bot Telegram em um controller e em um
`TelegramIntegrationService` que atualmente concentra resolução de usuário,
conta e categoria, criação de transações, parcelamentos, consultas e análise
financeira. Essa concentração aumenta o acoplamento e dificulta a evolução
para novos fluxos conversacionais e para a futura integração com IA.

O objetivo desta mudança é aproximar a integração Telegram da arquitetura
hexagonal já usada nos módulos de domínio, mantendo o controller como adapter
HTTP e isolando as regras de aplicação em casos de uso próprios. A refatoração
deve ser interna: os contratos consumidos pelo bot Telegram não serão alterados.

## Functional Requirements

- FR-1: O controller Telegram MUST permanecer responsável apenas por receber
  requests HTTP, validar entradas e delegar para a camada de aplicação.
- FR-2: A camada de aplicação MUST separar os fluxos de perfil/conta,
  transação simples, parcelamento, consultas e análise financeira.
- FR-3: Os casos de uso MUST receber comandos internos ou parâmetros de
  aplicação, sem depender diretamente de DTOs HTTP.
- FR-4: Dependências diretas de repositories da integração MUST ser isoladas
  em ports quando a operação pertencer ao fluxo Telegram.
- FR-5: A criação de transação simples MUST continuar reutilizando o use case
  de domínio existente e os resolvers de conta/categoria.
- FR-6: A criação de parcelamento e de parcelamento já iniciado MUST manter
  os comandos e use cases de domínio existentes.
- FR-7: Consultas de resumo mensal, transações, parcelas ativas, contagem e
  capacidade MUST preservar o comportamento atual.
- FR-8: Perfil, renda mensal, desconexão e conta padrão MUST preservar o
  comportamento atual.
- FR-9: O `TelegramIntegrationService` MUST ser reduzido a uma fachada
  compatível ou removido quando todos os adapters estiverem migrados.
- FR-10: A integração MUST manter compatibilidade com o bot Telegram atual.

## Non-Functional Requirements

- **NFR-1:** Não haverá alteração intencional nos paths, métodos HTTP, formatos
  de request/response ou códigos de erro existentes.
- **NFR-2:** Nenhum caso de uso MUST depender de classes de controller ou DTOs da
  camada web.
- **NFR-3:** A refatoração MUST NOT introduzir chamadas externas, persistência
  adicional, mudança de schema ou novas filas.
- **NFR-4:** Cada fluxo extraído MUST possuir testes unitários; os testes de
  controller/integrados existentes MUST continuar compilando e passando.
- **NFR-5:** A solução SHOULD manter transações nos limites atuais das operações
  de escrita e leitura.

## Acceptance Criteria

### AC-1: Preservar o contrato HTTP (FR-1, NFR-1)

**Given:** um request válido em qualquer endpoint Telegram.
**When:** o controller o recebe.
**Then:** ele delega para um caso de uso e retorna o mesmo contrato HTTP observado antes.
### AC-2: Isolar casos de uso e comandos (FR-2, FR-3)

**Given:** um fluxo de transação ou consulta.
**When:** o caso de uso é executado.
**Then:** ele não recebe nem retorna DTO HTTP como dependência interna.
### AC-3: Isolar resolução do usuário (FR-4)

**Given:** uma operação que precisa localizar o usuário Telegram.
**When:** o caso de uso é executado.
**Then:** a dependência fica atrás de uma porta ou serviço de aplicação claramente nomeado.
### AC-4: Preservar criação de transações (FR-5, FR-6)

**Given:** uma criação de transação simples, parcelada ou já iniciada.
**When:** o fluxo é executado.
**Then:** os mesmos comandos de domínio e regras de conta/categoria continuam sendo usados.
### AC-5: Preservar consultas e perfil (FR-7, FR-8)

**Given:** uma consulta ou operação de perfil existente.
**When:** o bot a chama.
**Then:** o resultado e os erros compatíveis permanecem iguais.
### AC-6: Manter compatibilidade com o bot (FR-9, FR-10)

**Given:** o bot Telegram atual.
**When:** os endpoints são chamados após a refatoração.
**Then:** nenhuma alteração no bot é necessária.
### AC-7: Manter os testes (NFR-4)

**Given:** o conjunto de testes do backend.
**When:** a suíte é executada no CI.
**Then:** os testes existentes e os novos testes dos casos de uso passam.

## Edge Cases

- EC-1: `telegramId` inexistente continua produzindo a exceção/resposta
  atualmente definida para usuário não encontrado.
- EC-2: Conta ou categoria explícita inexistente continua sendo resolvida ou
  criada pelas regras atuais.
- EC-3: Datas, tipo de transação ou parâmetros obrigatórios inválidos devem
  manter a validação e o erro atuais.
- EC-4: Nenhum resultado de parcelas ativas continua retornando a resposta
  vazia atual.
- EC-5: Mais de um grupo de parcelas ativo continua retornando conflito.
- EC-6: Falhas de repository ou de use cases de domínio devem propagar pelo
  mesmo mecanismo global de tratamento já existente.

## API Contracts

Os contratos HTTP existentes são mantidos; a refatoração não cria endpoints.

```text
TelegramIntegrationController
  GET    /telegram/users/me
  PATCH  /telegram/users/me/monthly-base-income
  DELETE /telegram/users/me/link
  GET    /telegram/financial-analysis
  GET    /telegram/expenses/current-month
  GET    /telegram/income/current-month
  POST   /telegram/transactions
  POST   /telegram/transactions/installments
  POST   /telegram/transactions/installments/existing
  POST   /telegram/transactions/summary
  GET    /telegram/accounts/default
  POST   /telegram/installments/count
  POST   /telegram/installments/purchase-capacity
  GET    /telegram/installments/active
  GET    /telegram/installments/summary
```

Os DTOs públicos atuais permanecem nos adapters HTTP. A camada de aplicação
deve usar commands/queries internos equivalentes quando a extração exigir.

```typescript
interface TelegramApplicationPort {
  execute(command: unknown): Promise<unknown>;
}
```

## Data Models

**N/A —** esta issue não altera entidades, tabelas, migrations ou persistência;
apenas reorganiza a orquestração da integração.

| Entidade | Tipo | Restrições |
| --- | --- | --- |
| Modelo persistido | N/A | Nenhuma alteração de schema nesta issue |
| Migration | N/A | Nenhuma migration nova |

## Out of Scope

- OS-1: Alterar interpretação de linguagem natural do bot.
- OS-2: Integrar OpenAI ou qualquer outro provedor de IA.
- OS-3: Adicionar RabbitMQ, Redis, lembretes ou processamento assíncrono.
- OS-4: Criar parcelamentos ou funcionalidades financeiras novas.
- OS-5: Alterar contratos públicos sem necessidade comprovada.
- OS-6: Refatorar módulos não relacionados à integração Telegram.

## 10. Estratégia de implementação e testes

1. Criar comandos/queries e casos de uso de aplicação por fluxo.
2. Criar ports/adapters somente para dependências específicas da integração.
3. Migrar o controller e preservar uma fachada temporária se necessário.
4. Extrair testes unitários dos fluxos principais antes de remover o serviço
   concentrador.
5. Executar testes do backend e do bot, além da verificação de whitespace do CI.
