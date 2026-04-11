# Changelog

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
