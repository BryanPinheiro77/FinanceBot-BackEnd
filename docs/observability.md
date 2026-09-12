# Observabilidade

O FinanceBot expõe métricas Prometheus pela API e pelo bot e escreve logs estruturados em JSON.
A stack local reúne Prometheus, Grafana, Loki e Grafana Alloy no arquivo
`compose.observability.yml`.

## O que cada componente faz

- **Prometheus** coleta métricas das aplicações a cada 15 segundos.
- **Grafana** exibe o dashboard `FinanceBot - Visão geral` e permite consultar métricas e logs.
- **Loki** armazena os logs da API e do bot.
- **Grafana Alloy** lê os logs dos containers Docker e os envia ao Loki.

O Alloy é usado no lugar do Promtail porque o Promtail chegou ao fim de vida em março de 2026,
conforme a [documentação oficial da Grafana](https://grafana.com/docs/loki/latest/send-data/promtail/).

## Como executar localmente

Inicie a API e o bot nas portas padrão `8080` e `8081`. Depois execute, na raiz do repositório:

```bash
docker compose -f compose.observability.yml up -d
```

Os serviços ficam disponíveis apenas na interface local:

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100/ready`

O acesso inicial do Grafana usa `admin`/`admin` somente para desenvolvimento local. Para definir
outras credenciais sem versioná-las:

```bash
GRAFANA_ADMIN_USER=seu-usuario GRAFANA_ADMIN_PASSWORD=sua-senha \
  docker compose -f compose.observability.yml up -d
```

## Métricas das aplicações

- API: `http://localhost:8082/actuator/prometheus`
- Bot: `http://localhost:8083/actuator/prometheus`

Actuator usa portas de gerenciamento separadas. Nos containers de produção, `8082` e `8083`
são publicadas somente em `127.0.0.1`; métricas e detalhes operacionais não ficam expostos pela
interface pública da API ou do bot.

Além das métricas automáticas de HTTP, JVM, banco e RabbitMQ, o projeto registra:

- publicações e falhas de lembretes;
- entregas, descartes e liberações de lembretes;
- transações geradas por recorrências;
- mensagens do Telegram processadas ou com falha;
- resultado e duração das interpretações pela OpenAI.

As labels têm conjuntos pequenos e conhecidos. IDs de usuário, descrições, valores financeiros,
tokens e conteúdo das mensagens não são usados como labels nem gravados pelos novos logs.

## Correlação de logs

Cada requisição da API recebe o header `X-Correlation-Id`. O bot cria esse identificador para
cada mensagem recebida e o encaminha nas chamadas HTTP para a API. O valor aparece no campo
`correlationId` dos logs JSON, permitindo pesquisar o mesmo fluxo nos dois serviços.

Para mensagens de lembrete, o consumidor usa `reminder-<id>` como identificador de correlação.
O ID técnico ajuda a seguir publicação, consumo e confirmação sem registrar o texto do lembrete.

## Diagnóstico rápido

```bash
docker compose -f compose.observability.yml ps
curl --fail http://localhost:9090/-/ready
curl --fail http://localhost:3100/ready
```

No Prometheus, consulte `up{job=~"financebot-.+"}`. O valor `1` indica que o endpoint da
aplicação foi coletado; `0` indica falha de acesso ou aplicação indisponível.

Para remover os containers preservando os dados coletados:

```bash
docker compose -f compose.observability.yml down
```
