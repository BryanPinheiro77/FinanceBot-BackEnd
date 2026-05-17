# Changelog

## [1.1.4]

### Refactored
- Centralizada a resolução do usuário autenticado com `AuthenticatedUserResolver`.
- Centralizada a resolução de recursos pertencentes ao usuário com `UserResourceResolver`.
- Centralizada a validação entre categoria e tipo de transação com `TransactionCategoryValidator`.
- Extraída a lógica de parcelamento do `TransactionService` para o domínio de transações.
- Adicionado modelo de domínio para plano de parcelamento com `InstallmentPlan`, `InstallmentPlanItem` e `InstallmentPlanFactory`.
- Reduzida a responsabilidade do `TransactionService`, mantendo-o mais focado na orquestração do caso de uso.

### Tests
- Adicionados testes para `AuthenticatedUserResolver`.
- Adicionados testes para `UserResourceResolver`.
- Adicionados testes para `TransactionCategoryValidator`.
- Adicionados testes para `InstallmentPlanFactory`.
- Atualizados testes de `TransactionService`, `RecurringTransactionService` e `FinancialAnalysisService` após extrações de responsabilidades.

### Quality
- Mantida a suíte de testes automatizados passando após as refatorações.
- Melhorada a separação entre regras de domínio e lógica de aplicação.

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
