# Alertas financeiros e resumos

As regras da issue #119 são calculadas pela API e publicadas em uma fila
RabbitMQ dedicada. O bot Telegram consome a fila e entrega a mensagem ao
usuário. O Redis do bot guarda uma chave de deduplicação por notificação por
31 dias; se o RabbitMQ reenviar a mesma mensagem, ela não é entregue duas
vezes.

As regras atuais são:

- gasto fora do padrão: último mês fechado comparado à média dos três meses
  anteriores, com aumento mínimo de 50% e R$ 100;
- excesso de parcelas: cinco ou mais grupos de parcelas ativos;
- orçamento apertado: comprometimento projetado a partir de 60% ou saldo
  projetado negativo;
- resumo semanal: semana anterior completa, de segunda a domingo;
- resumo mensal: mês fechado anterior.

O scheduler roda diariamente às 9h por padrão. A análise de resumos é emitida
somente na segunda-feira e no primeiro dia do mês, respectivamente. O recurso
pode ser desativado globalmente com `FINANCEBOT_ALERTS_ENABLED=false`.

Esta etapa não altera os endpoints existentes e não envia dados sensíveis para
logs. A entrega depende do RabbitMQ e do Redis configurados para o bot.
