# Changelog

## [Unreleased]

### Added

- Adicionado suporte para registro de parcelamentos existentes, gerando apenas parcelas restantes a partir da parcela atual.
- Adicionado endpoint autenticado para criação de parcelamentos existentes no módulo de transações.
- Adicionado endpoint de integração Telegram para criação de parcelamentos existentes via bot.
- Adicionado suporte no bot Telegram para interpretar parcelamentos em andamento com parcelas pagas ou parcela atual.
- Adicionada edição de progresso de parcelamento existente no preview do Telegram antes da confirmação.
- Adicionado suporte a novos aliases de categorias e contas no vocabulário de linguagem natural do bot Telegram.
- Adicionado suporte a novas expressões de período nas consultas do Telegram, incluindo semana atual e últimos 30 dias.
- Adicionado suporte a novas variações de perguntas sobre término de parcelamentos.

### Changed

- Ajustado o fluxo de criação de parcelamentos para reutilizar a geração de plano de parcelas com suporte a parcelas restantes.
- Melhorada a interpretação de frases como `ja paguei 5`, `estou pagando a 6` e `parcelamento de 10x` no bot Telegram.
- Melhorada a detecção de intenções de despesa e receita em mensagens naturais do Telegram.
- Melhorada a limpeza da descrição de transações interpretadas pelo Telegram, evitando sobras como preposições após remoção de valores.
- Centralizados termos e padrões de limpeza do parser em constantes para facilitar manutenção.

### Fixed

- Corrigida a autorização da rota de integração Telegram para salvar parcelamentos existentes.
- Corrigido o roteamento de `CREATE_EXISTING_INSTALLMENT_EXPENSE` para o preview do bot Telegram.
- Corrigido o parser do Telegram para não usar quantidade de parcelas pagas como valor da compra.

### Tests

- Adicionada cobertura para criação de parcelamentos existentes no domínio, use case, controller e integração Telegram.
- Adicionada cobertura para interpretação, preview, edição e confirmação de parcelamentos existentes no bot Telegram.

## v1.4.0

### Added

- Adicionado suporte a Redis para contexto conversacional temporário do bot Telegram.
- Adicionados stores configuráveis em memória e Redis para contexto ativo de conversa e contexto passivo de consultas.
- Adicionado fluxo multi-etapas para criação de parcelamentos no Telegram, perguntando o dia de vencimento antes do preview.

### Changed

- Migrado o contexto de consultas do Telegram para ports/adapters com TTL configurável.
- Extraído o armazenamento de contexto conversacional do bot para contratos neutros, mantendo Redis restrito aos adapters de infraestrutura.
- Ajustado o cancelamento de operações pendentes para limpar também o contexto conversacional ativo.

### Tests

- Adicionada cobertura para stores em memória e Redis de contexto conversacional e contexto de consultas.
- Adicionada cobertura para continuação de conversa, resolução de vencimento de parcelamento e limpeza de contexto no cancelamento.

## v1.3.0

### Changed

- Alinhado o fluxo de preview, confirmação e persistência de transações via Telegram usando `PendingTelegramTransaction`, garantindo que os dados exibidos ao usuário sejam os mesmos usados na confirmação.
- Refatorado o fluxo de comandos do Telegram, mantendo `TelegramCommandService` como fachada fina.
- Adicionado `TelegramCommandRouter` para centralizar o roteamento de mensagens e delegar os fluxos para handlers especializados.
- Extraídos handlers específicos para comandos básicos, operações pendentes, preview de transações, confirmação de transações, edição de pendências, consultas financeiras, consultas pendentes e mensagens em linguagem natural.
- Extraídos componentes auxiliares para identificação de comandos, normalização de texto, parsing de edição pendente, resolução de conta padrão no preview e mapeamento de erros do bot.

### Tests

- Adicionada cobertura para preview e confirmação de transações comuns e parceladas no bot Telegram.
- Adicionado teste garantindo que a conta padrão resolvida no preview seja preservada no pending quando encontrada.
- Adicionada cobertura unitária para `TelegramCommandRouter`, `TelegramPendingEditParser`, `TelegramPreviewAccountResolver` e `TelegramPendingQueryHandler`.
- Ajustados testes do fluxo de comandos do Telegram após a separação em router, handlers e componentes auxiliares.

## v1.2.1

### Changed
- Refatorado o módulo de transações para uma estrutura mais próxima da arquitetura hexagonal.
- Extraídos casos de uso para criação, listagem, busca, atualização, remoção e parcelamento de transações.
- Removido o `TransactionService`, substituindo o fluxo por use cases e ports/adapters.
- Movido `TransactionController` para `adapter/in/web`.
- Isolado o acesso a `TransactionRepository` e `TransactionSpecification` no adapter de persistência.
- Separados DTOs de request HTTP dos commands internos nos fluxos de criação, criação parcelada e atualização de transações.
- Ajustado o fluxo de parcelamento para `CreateInstallmentTransactionUseCase` receber `CreateInstallmentTransactionCommand`.
- Ajustado o fluxo de atualização para `UpdateTransactionUseCase` receber `UpdateTransactionCommand`.
- Ajustado o fluxo do Telegram para criar transações comuns e parceladas usando commands internos em vez de DTOs HTTP.
- Removida a dependência direta de `Page` e `Pageable` da camada application de transações.
- Ajustado `ListTransactionsUseCase` e `FindTransactionPort` para usar tipos neutros de paginação.
- Adaptado `TransactionPersistenceAdapter` para converter paginação neutra para Spring Data.

### Added
- Adicionados ports de saída para persistência de transações.
- Adicionado `TransactionPersistenceAdapter`.
- Adicionados `PageQuery`, `PageResult`, `PageSort` e `SortDirection` como tipos neutros de paginação.
- Adicionado mapper de paginação entre Spring Data e tipos internos no adapter web.

### Tests
- Adicionados testes unitários para use cases, controller e adapter de persistência de transações.
- Atualizados testes de use cases, controller e integração Telegram para os novos commands internos.
- Adicionada cobertura para criação parcelada via Telegram delegando para `CreateInstallmentTransactionCommand`.
- Adicionados testes para o mapper de paginação.
- Atualizados testes de listagem, controller e adapter de persistência de transações para os tipos neutros de paginação.

## v1.2.0

### Added

- Adicionado suporte para desativar e reativar categorias do usuário sem apagar referências históricas.
- Adicionados os campos `active` e `defaultCategory` em categorias.
- Adicionada migration para incluir as colunas `active` e `default_category` na tabela `categories`.

### Changed

- A listagem de categorias passa a retornar apenas categorias ativas por padrão.
- A remoção de categorias passa a realizar soft delete com `active = false`.
- `GET /categories/{id}` passa a retornar apenas categorias ativas.
- `PUT /categories/{id}` passa a permitir alteração apenas de categorias ativas.
- O delete de categorias passa a ser idempotente quando a categoria já está inativa.
- A resolução de categorias em transações, recorrências e Telegram passa a tratar categorias inativas de forma segura.
- Ajustado o perfil de testes para configurar JWT, Redis e RabbitMQ no ambiente `test`.

### Fixed

- Corrigido risco de categorias desativadas continuarem sendo usadas em novas transações por envio direto do `categoryId`.
- Corrigido risco de categorias inativas serem resolvidas automaticamente pelo fluxo do Telegram.
- Corrigida inconsistência entre categorias ocultas na listagem e categorias ainda acessíveis por busca direta de ID.

### Tests

- Atualizados testes unitários de `CategoryService`.
- Atualizados testes unitários de `UserResourceResolver`.
- Atualizados testes unitários de `TelegramCategoryResolverService`.
- Adicionada cobertura para soft delete de categorias.
- Adicionada cobertura para reativação de categorias inativas.
- Adicionada cobertura para impedir uso de categorias inativas em novos fluxos.
- Adicionada cobertura para comportamento idempotente ao remover categoria já inativa.

## v1.1.4

### Changed
- Centralizada a resolução do usuário autenticado com `AuthenticatedUserResolver`.
- Centralizada a resolução de recursos pertencentes ao usuário com `UserResourceResolver`.
- Centralizada a validação entre categoria e tipo de transação com `TransactionCategoryValidator`.
- Extraída a lógica de parcelamento do `TransactionService` para o domínio de transações.
- Adicionado modelo de domínio para plano de parcelamento com `InstallmentPlan`, `InstallmentPlanItem` e `InstallmentPlanFactory`.
- Reduzida a responsabilidade do `TransactionService`, mantendo-o mais focado na orquestração do caso de uso.
- Melhorada a separação entre regras de domínio e lógica de aplicação.
- Reduzido o acoplamento entre services.

### Tests
- Adicionados testes para `AuthenticatedUserResolver`.
- Adicionados testes para `UserResourceResolver`.
- Adicionados testes para `TransactionCategoryValidator`.
- Adicionados testes para `InstallmentPlanFactory`.
- Atualizados testes de `TransactionService`, `RecurringTransactionService` e `FinancialAnalysisService` após extrações de responsabilidades.
- Mantida a suíte de testes automatizados passando após as refatorações.

## v1.1.3
### Tests
- Adicionados testes unitários para `UserService`, cobrindo autenticação, atualização de renda base e vínculo/desvínculo do Telegram.
- Adicionados testes unitários para `RecurringTransactionService`, cobrindo criação, consulta, atualização, ativação, desativação, exclusão e validações de regra de negócio.
- Aumentada a cobertura geral do projeto para acima de 50%.

## v1.1.2
- Configuração de CORS com origem permitida por propriedade
- Configuração local do SonarQube via Docker
- Configuração do JaCoCo para geração de relatório de cobertura de testes
- Perfil de testes com banco H2 em memória
- API e bot padronizados para execução com Java 21
- Suporte da API alinhado ao fluxo web da v1.2.0
- Tratamento de erros de autenticação ajustado para respostas 401 consistentes
- Login inválido agora retorna 401 em vez de erro genérico
- E-mail já cadastrado agora retorna 400 em vez de erro 500
- Respostas de validação HTTP padronizadas com retorno 400 para requisições inválidas
- Interpretação de mensagens do Telegram refinada para exigir valor e quantidade de parcelas explícitos na análise de compra parcelada
- Normalização de descrições de parcelas ajustada para evitar remoções incorretas e regex com risco de backtracking
- Ajustada lógica de cálculo de datas de parcelas para evitar alerta de overflow em operações numéricas
- Refatoração de constantes e trechos repetidos em services, segurança e tratamento de erros
- Novos testes unitários para tratamento global de erros, services principais da API e fluxo de análise parcelada no bot
- Ajustes de confiabilidade, cobertura, Maintainability e Security Hotspots identificados no SonarQube
- Quality Gate aprovado com cobertura mínima de New Code

## v1.1.1
- Reorganização dos DTOs do Telegram em pacotes de `request` e `response`
- Ampliação da cobertura automatizada para a análise de compra parcelada
- Testes para parser, fluxo do bot e service
- Validação de cenários felizes, entradas inválidas e regras de classificação

## v1.1.0
- Análise de capacidade para nova compra parcelada no bot do Telegram
- Interpretação de valor total e quantidade de parcelas explícitos na mensagem
- Simulação determinística baseada na análise financeira atual do usuário
- Resposta com valor total, parcelas, valor estimado por parcela, resultado e observação curta
- Novos estados de decisão: `VIAVEL`, `ALERTA` e `DESFAVORAVEL`
- Endpoint Telegram dedicado para consulta de viabilidade de compra parcelada
- Cobertura inicial com testes unitários do parser e da regra de análise

## v1.0.0
- Criação de transações por linguagem natural no Telegram
- Preview da operação com confirmação ou cancelamento
- Edição da operação pendente antes da confirmação
- Resolução de conta padrão no preview
- Respostas do bot formatadas em HTML
- Consultas por período
- Consultas por conta e categoria
- Criação de despesas parceladas pelo bot
- Consultas de parcelamento
- Contexto conversacional curto para continuidade de consultas
- Vocabulário centralizado com aliases iniciais
- Limpeza inicial da descrição interpretada
