# Changelog

As versões do produto seguem as [regras de release](docs/releases.md). O histórico abaixo foi reconciliado com as releases publicadas; as tags antigas foram preservadas. Datas usam UTC, conforme o GitHub.

## [Unreleased]

### Changed

- Reconciliados changelog e roadmap com as releases publicadas e o estado das issues.
- Formalizados versionamento, promoção, notas de release e atualização contínua da documentação.

## [v1.10.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.10.0) — 2026-09-16

### Added

- alertas explicáveis de gastos fora do padrão por categoria, excesso de parcelas e orçamento apertado;
- resumos financeiros da última semana e do último mês completos, enviados pelo Telegram;
- comando `/alertas` para consultar preferências e habilitar ou desabilitar alertas, resumos semanais e mensais separadamente.

### Changed

- geração de notificações e publicação no RabbitMQ separadas, com recuperação de trabalho pendente;
- recuperação do último período fechado quando uma execução agendada é perdida;
- processamento de usuários e notificações em lotes, com configurações documentadas no `.env.example`;
- documentação das regras, contratos internos e procedimentos operacionais em `docs/alerts.md`.

### Fixed

- indisponibilidade do broker mantém notificações pendentes na outbox PostgreSQL;
- reservas transacionais e confirmação idempotente evitam envio simultâneo por consumidores duplicados;
- falhas na confirmação da API não provocam novo envio ao Telegram;
- preferências, vínculo Telegram e expiração são verificados antes da entrega.

### Security

- RabbitMQ recebe apenas o ID opaco da notificação; destino e conteúdo são obtidos pela API interna autenticada;
- migration V15 adiciona a outbox e as preferências individuais, habilitadas por padrão;
- configurações novas têm valores padrão e não exigem novos secrets;
- o Telegram não oferece idempotência em `sendMessage`: entregas incertas ficam `UNKNOWN`, sem reenvio automático, e exigem acompanhamento operacional;
- recuperação de resumos limitada ao último período fechado.

### Tests

- 358 testes da API e 188 do bot aprovados no código final da implementação;
- happy path e cenários de erro cobrem detectores, períodos, publicação, duplicatas, reservas expiradas, confirmação perdida e preferências;
- CI do PR de promoção aprovado; verificação final de CI e deploy da main registrada abaixo.

### Referências

- issue #119 e subtarefas #140–#145;
- PRs #200–#205 — implementação;
- PR #206 — promoção para `main`;
- comparação: https://github.com/BryanPinheiro77/FinanceBot-BackEnd/compare/v1.9.1...v1.10.0

CI da `main` aprovado: https://github.com/BryanPinheiro77/FinanceBot-BackEnd/actions/runs/35044646112

Deploy concluído com health checks da API, bot e observabilidade aprovados: https://github.com/BryanPinheiro77/FinanceBot-BackEnd/actions/runs/35044646082

## [v1.9.1](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.9.1) — 2026-09-13

### Added

- adicionada publicação automática da stack de observabilidade no deploy;
- adicionada configuração de produção para Prometheus coletar API e bot pela `backend-network`;
- adicionado acesso privado de Grafana, Prometheus e Loki pelo endereço da Tailscale.

### Changed

- Grafana agora exige `GRAFANA_ADMIN_PASSWORD` configurada no ambiente;
- removido o fallback de senha `admin/admin` para a configuração de produção;
- workflow passou a validar API, bot, Prometheus, Grafana e Loki após o deploy;
- adicionados exemplos de ambiente para API, bot e observabilidade.

### Security

- portas da observabilidade ficam vinculadas somente ao IPv4 da Tailscale do servidor;
- métricas da API e do bot trafegam pela rede Docker privada;
- nenhum token, senha ou endereço privado foi versionado;
- a rotação de chaves continua desabilitada por padrão.

### Tests

- CI da `main` aprovado;
- configuração dos Compose local e de produção validada;
- deploy de produção concluído com health checks da API, bot e observabilidade;
- Prometheus confirmou API e bot com `up=1`.

### Referências

- PR #198 — implementação da observabilidade;
- PR #199 — promoção para `main`.

## [v1.9.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.9.0) — 2026-09-13

### Added

- adicionado chaveiro versionado para criptografia de campos sensíveis;
- adicionada escrita com a chave ativa e leitura compatível com chaves anteriores;
- adicionado job opt-in para recriptografar dados em lotes, com transações e atualização otimista;
- adicionados health check e interrupção segura quando a rotação falha.

### Changed

- novos envelopes usam o formato `v2.<key-id>.<payload>`;
- valores legados `v1` podem ser migrados sem indisponibilidade da API;
- configuração permite manter chaves anteriores somente para leitura e revogá-las após a migração;
- documentação de desenvolvimento, deploy e segurança foi atualizada com o procedimento de rotação.

### Fixed

- a troca direta da chave deixou de tornar os valores existentes ilegíveis durante a transição;
- alterações concorrentes durante a migração são detectadas e interrompem o lote afetado.

### Tests

- suíte completa da API validada com 315 testes, sem falhas ou erros;
- suíte completa do bot Telegram validada com 170 testes, sem falhas ou erros;
- testes de criptografia, chaveiro, seleção SQL, migração em lotes, concorrência, conclusão e falha aprovados;
- CI dos PRs #195 e #196 aprovado.

### Security

- a rotação permanece desabilitada por padrão;
- a execução exige `FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED=true`;
- as chaves não são registradas no log nem versionadas;
- a documentação inclui backup restaurável, rollback, validação e revogação da chave antiga;
- nenhuma alteração de chave foi ativada automaticamente no deploy desta release.

### Referências

- Issue #187 — rotação de chaves de criptografia;
- PR #195 — implementação em `develop`;
- PR #196 — promoção para `main`.

## [v1.8.4](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.4) — 2026-09-13

### Added

- adicionada autenticação configurável para Redis nos módulos da API e do bot Telegram;
- adicionado vhost exclusivo `/financebot` e suporte configurável a TLS para RabbitMQ;
- adicionada documentação de backup criptografado, restauração isolada e resposta a falhas;
- adicionado modelo de ameaças STRIDE/DREAD para os armazenamentos de dados.

### Changed

- mensagens de lembrete no RabbitMQ agora expiram após 24 horas por padrão;
- logs SQL detalhados ficam desativados por padrão em produção;
- portas administrativas de Redis e RabbitMQ foram limitadas ao localhost no servidor;
- Redis passou a utilizar autenticação e volume persistente;
- credenciais de infraestrutura foram isoladas nos arquivos de ambiente protegidos.

### Fixed

- removido o usuário padrão `guest` do RabbitMQ;
- removidos o usuário legado e as permissões temporárias do vhost raiz após a migração;
- eliminada a exposição pública das portas `6379`, `5672` e `15672` no servidor.

### Tests

- suíte completa da API validada com `./mvnw clean verify`;
- suíte completa do bot Telegram validada com `./mvnw clean verify`;
- expiração das mensagens RabbitMQ coberta por teste unitário;
- CI e health checks de produção aprovados após o deploy.

### Security

- API e bot utilizam o usuário dedicado `financebot_app` no vhost `/financebot`;
- Redis rejeita conexões sem autenticação;
- API e bot permaneceram com health `UP` após a rotação das configurações;
- procedimentos de backup, verificação de integridade e restauração estão documentados.

### Referências

- Issue #188 — proteger Redis, RabbitMQ, logs e backups;
- PR #193 — implementação e documentação em `develop`;
- PR #194 — promoção para `main`.

## [v1.8.3](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.3) — 2026-09-13

### Added

- adicionada cobertura automatizada para os principais fluxos financeiros, Telegram, lembretes e segurança;
- adicionados testes dos mappers principais e dos controllers de integração;
- adicionados testes para o scheduler de transações recorrentes.

### Changed

- aumentada a cobertura geral do backend para 90,1% de linhas e 73,6% de branches;
- ampliada a validação dos cenários de análise financeira e projeções recorrentes;
- ampliada a cobertura dos fluxos de cadastro, login, JWT e autenticação de requisições;
- ampliada a cobertura de consultas e operações financeiras do bot Telegram.

- nenhum código de produção foi alterado nesta entrega;
- branches de trabalho continuam sendo removidas automaticamente após o merge;
- `develop` permanece preservada no fluxo de promoção para `main`.

### Tests

- cobertos caminhos de erro para credenciais inválidas, tokens expirados ou malformados e usuários Telegram inexistentes;
- cobertos cenários de validação de parcelamentos, categorias duplicadas, lembretes e exceções globais;
- cobertos cenários de recorrências ativas, inativas, vencidas e reagendamento de lembretes.

- suíte completa validada com `./mvnw clean verify`;
- testes unitários adicionados para análise financeira, autenticação, Telegram, lembretes, mappers, controllers e scheduler;
- caminhos de sucesso, erro e limite incluídos na cobertura.

### Referências

- Issue #42 — aumentar cobertura geral de testes para 60%;
- PR #191 — implementação dos testes em `develop`;
- PR #192 — promoção para `main`.

## [v1.8.2](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.2) — 2026-09-13

### Security

- adiciona criptografia AES-256-GCM para a renda mensal;
- adiciona configuração externa de chave de criptografia;
- documenta classificação e proteção de dados sensíveis.

### Added

- adiciona processamento de documentos PDF;
- adiciona OCR opcional para imagens via endpoint compatível com OpenAI;
- adiciona transcrição opcional de áudio;
- encaminha conteúdo extraído ao fluxo existente de interpretação, preview e confirmação;
- adiciona limites de tamanho e tratamento de erros para mídias.

### Changed

- atualiza Compose e documentação para configuração de IA e transcrição;
- deploy de produção validado com API e bot saudáveis.

## [v1.8.1](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.1) — 2026-09-12

### Added

- Observabilidade inicial da API e do bot Telegram.
- Métricas Prometheus para requisições, lembretes, recorrências e IA.
- Correlation IDs e logs estruturados em ECS.
- Stack local com Prometheus, Grafana, Loki e Grafana Alloy.
- Documentação operacional atualizada.

### Tests

- API: 225 testes aprovados.
- Bot: 161 testes aprovados.
- CI da branch `develop` aprovado.

Inclui as alterações do PR #115, promovidas no PR #116.

## [v1.8.0]

### Added

- Adicionado envio assíncrono de lembretes pelo RabbitMQ, com producer na API, consumer no bot Telegram e topologia durável para notificações.

### Changed

- Substituído o polling de lembretes realizado pelo bot pela publicação agendada de eventos na API.

### Tests

- Adicionada cobertura para publicação, roteamento, consumo, mensagens obsoletas e tratamento de falha das notificações de lembrete.

## [v1.7.0]

### Added

- Adicionada execução automática de transações recorrentes vencidas, com avanço da próxima data, encerramento após a data final e proteção transacional contra duplicidade.
- Adicionado cadastro de lembretes avulsos ou vinculados a recorrências, com entrega pelo bot Telegram e confirmação para evitar reenvio.

## [v1.6.0]

### Changed

- Refatorada a classificação de intenções e extraídos formatters dedicados para transações e consultas do módulo Telegram, preservando a fachada e os contratos existentes.
- Separada a interpretação de mensagens do bot em parsers dedicados para consultas e transações, preservando o comportamento existente.
- Refatorada a integração Telegram da API para casos de uso de aplicação separados por fluxo, com commands internos e fachada de compatibilidade.
- Isolada a responsabilidade da integração Telegram para manter controllers e regras de aplicação preparados para evolução independente.
- Mantida a compatibilidade dos contratos HTTP existentes usados pelo bot Telegram.
- Integrada a OpenAI como estratégia principal de interpretação de mensagens não estruturadas, com o parser determinístico como fallback.

### Added

- Adicionada porta `AiInterpretationPort` e adapter compatível com a API de Chat Completions.
- Adicionado contrato interno estruturado para intenções financeiras, com validação de valores, tipos e parcelamentos antes do preview.
- Adicionada configuração opcional da IA por ambiente, desativada por padrão, incluindo endpoint, modelo e timeout.
- Adicionado `AGENTS.md` com contexto, limites operacionais e regras de contribuição para agentes.
- Adicionada documentação de desenvolvimento, arquitetura, API, bot Telegram, integração com frontend, contribuição, deploy e servidor remoto.
- Adicionado workflow de CI para validar os dois módulos Maven com Java 21.
- Adicionado health check técnico ao bot Telegram e validação pós-deploy da API e do bot.
- Adicionada notificação opcional de falha de deploy via webhook privado.

### Fixed

- Corrigida a inicialização do bot quando o adapter de IA está presente, mantendo o carregamento do contexto Spring sem API key configurada.

### Tests

- Adicionada cobertura para resposta válida da IA, prioridade sobre o parser determinístico, resposta inválida e fallback em caso de indisponibilidade.
- Mantida a suíte completa do bot Telegram e da API passando após as refatorações.

## [v1.5.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/1.5.0) — 2026-06-20

### Added

- Adicionado suporte para registro de parcelamentos existentes, gerando apenas parcelas restantes a partir da parcela atual.
- Adicionado endpoint autenticado para criação de parcelamentos existentes no módulo de transações.
- Adicionado endpoint de integração Telegram para criação de parcelamentos existentes via bot.
- Adicionado suporte no bot Telegram para interpretar parcelamentos em andamento com parcelas pagas ou parcela atual.
- Adicionada edição de progresso de parcelamento existente no preview do Telegram antes da confirmação.
- Adicionado suporte a novos aliases de categorias e contas no vocabulário de linguagem natural do bot Telegram.
- Adicionado `mc` `mac` para a categoria Alimentação no vocabulário de linguagem natural do bot Telegram.
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
