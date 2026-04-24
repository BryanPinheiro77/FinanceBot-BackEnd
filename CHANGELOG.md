# Changelog

## [Unreleased]

## v1.1.2
- Configuração de CORS com origem permitida por propriedade
- Configuração local do SonarQube via Docker
- Configuração do JaCoCo para geração de relatório de cobertura de testes
- Perfil de testes com banco H2 em memória
- API principal ajustada para execução com Java 21
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
