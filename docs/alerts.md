# Alertas financeiros e resumos

A API calcula as regras e grava cada notificação em uma outbox PostgreSQL antes
de publicá-la no RabbitMQ. A fila dedicada contém somente o ID opaco da
notificação; o conteúdo financeiro fica no banco e é obtido pelo bot por uma
rota interna autenticada. O Redis não participa da deduplicação desses alertas.

## Regras e frequência

- Gasto fora do padrão: último mês fechado comparado à média dos três meses
  anteriores, com aumento mínimo de 50% e R$ 100.
- Excesso de parcelas: cinco ou mais grupos de parcelas ativos.
- Orçamento apertado: comprometimento projetado de pelo menos 60% ou saldo
  projetado negativo.
- Resumo semanal: semana anterior completa, de segunda a domingo.
- Resumo mensal: mês fechado anterior.

O scheduler calcula diariamente às 9h no fuso do processo. Sempre considera a
última semana e o último mês fechados, inclusive quando uma execução de
segunda-feira ou do dia 1 foi perdida. Não gera resumos de períodos terminados
antes do cadastro do usuário. A recuperação é limitada ao último período
fechado; não produz retrospectivas de todos os períodos de uma indisponibilidade
longa. Execuções repetidas geram o mesmo ID por usuário, regra e período e não
criam outro item. Cada alerta de risco é limitado a um por mês; cada categoria
atípica tem seu próprio item mensal.

## Preferências

No Telegram:

- `/alertas`: consultar as preferências individuais.
- `/alertas ligar` ou `/alertas desligar`: ativar ou desativar todos os tipos.
- `/alertas semanal ligar|desligar`: controlar somente o resumo semanal.
- `/alertas mensal ligar|desligar`: controlar somente o resumo mensal.

As preferências existentes começam ativadas. A geração respeita cada opção;
a reserva de entrega verifica novamente preferências, vínculo Telegram,
expiração e configuração global. Uma alteração não cancela uma chamada ao
Telegram que já começou. Desativação ou desvinculação antes da reserva cancela
os itens enfileirados. Reativar não recria itens já cancelados no mesmo período.
`FINANCEBOT_ALERTS_ENABLED=false` desativa globalmente a geração e novas reservas.

## Entrega e falhas

Estados persistidos: `PENDING`, `PUBLISHED`, `SENDING`, `SENT`, `UNKNOWN` e
`CANCELLED`. A publicação roda a cada 60 segundos em lotes de 100 e conserva o
item pendente diante de falha do broker. Itens publicados sem reserva são
republicados após cinco minutos. Duplicatas RabbitMQ precisam adquirir a mesma
reserva transacional; somente uma execução pode começar a entrega.

A reserva gera um token e dura dois minutos. O bot confirma o resultado com o
mesmo token; repetir essa confirmação é idempotente. Rejeições explícitas do
Telegram voltam a `PENDING` após cinco minutos, até cinco tentativas. Erros de
rede/timeout ficam `UNKNOWN`. Falhas no ACK repetem somente o ACK, até três
vezes, sem reenviar a mensagem.

**Limite de entrega:** Telegram não oferece uma chave de idempotência para
`sendMessage`. Se o envio ocorre e a confirmação se perde, não há como garantir
exactly-once. Uma reserva expirada ou resposta ambígua fica `UNKNOWN` para
reconciliação operacional e não é reenviada automaticamente. Isso evita
transformar uma falha de confirmação em mensagem duplicada. Não descrevemos
`UNKNOWN` como entrega concluída nem garantimos ausência absoluta de perda.

Para acompanhar pendências no banco, consultar somente metadados:

```sql
SELECT id, kind, status, attempts, created_at, claimed_at, expires_at
FROM financial_notifications
WHERE status IN ('UNKNOWN', 'PENDING', 'PUBLISHED', 'SENDING')
ORDER BY created_at;
```

Estados incertos devem ser conferidos com o destinatário antes de qualquer
reenvio manual. Não existe rotina automática que reenvie itens `UNKNOWN`.

## Contratos e configuração

Todas as rotas abaixo exigem `X-Internal-Service-Token`; não são contratos do
frontend:

- `POST /telegram/financial-notifications/{id}/claim`: retorna token, chat,
  título e corpo; `204` quando não há entrega elegível.
- `PATCH /telegram/financial-notifications/{id}/delivery`: `{token, outcome}`;
  resultados permitidos `SENT`, `PENDING` (rejeição explícita) e `UNKNOWN`.
- `GET /telegram/financial-notifications/preferences?telegramId=...`.
- `PATCH /telegram/financial-notifications/preferences?telegramId=...`:
  `{alerts, weeklySummary, monthlySummary}`, todos booleanos obrigatórios.

Configuração: `FINANCEBOT_ALERTS_ENABLED`, `FINANCEBOT_ALERTS_SCHEDULER_CRON`,
`FINANCEBOT_ALERTS_PUBLISH_INTERVAL` e `FINANCEBOT_ALERTS_QUEUE_MESSAGE_TTL_MS`.
A migration V15 adiciona preferências e a outbox; nenhuma migration anterior
foi alterada. Não há nova credencial obrigatória de ambiente.

Conteúdo financeiro da outbox é confidencial, mantido em claro nesta etapa,
como os valores usados nas agregações. Acesso ao banco e aos backups deve ser
restrito. Alertas expiram na virada de período; resumos semanais em 14 dias e
mensais em 45 dias após o fechamento. Itens `SENT`/`CANCELLED` são removidos sete
dias após expirar; conteúdo `UNKNOWN` é redigido nesse mesmo prazo, preservando
metadados para reconciliação. IDs expirados não são recriados pela geração.
