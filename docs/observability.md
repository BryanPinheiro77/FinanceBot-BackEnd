# Observabilidade

O FinanceBot expõe métricas Prometheus pela API e pelo bot e escreve logs estruturados em JSON.
A stack reúne Prometheus, Grafana, Loki e Grafana Alloy no arquivo `compose.observability.yml`.
Em produção, `compose.observability.prod.yml` troca os alvos de métricas para a rede Docker privada
do FinanceBot e o workflow de deploy atualiza a stack junto com a API e o bot.

## O que cada componente faz

- **Prometheus** coleta métricas das aplicações a cada 15 segundos.
- **Grafana** exibe o dashboard `FinanceBot - Visão geral` e permite consultar métricas e logs.
- **Loki** armazena os logs da API e do bot.
- **Grafana Alloy** lê os logs dos containers Docker e os envia ao Loki.

O Alloy é usado no lugar do Promtail porque o Promtail chegou ao fim de vida em março de 2026,
conforme a [documentação oficial da Grafana](https://grafana.com/docs/loki/latest/send-data/promtail/).

## Como executar localmente

Copie `.env.example` para `.env`, defina uma senha forte em `GRAFANA_ADMIN_PASSWORD` e inicie a
API e o bot nas portas padrão `8080` e `8081`. Depois execute, na raiz do repositório:

```bash
docker compose -f compose.observability.yml up -d
```

Os serviços ficam disponíveis apenas na interface local:

- Grafana: `http://localhost:3000`
- Prometheus: `http://localhost:9090`
- Loki: `http://localhost:3100/ready`

As portas usam `OBSERVABILITY_BIND_ADDRESS`, que deve ser `127.0.0.1` localmente. O Grafana exige
uma senha definida por `GRAFANA_ADMIN_PASSWORD`; não existe senha padrão de produção.

## Acesso privado na produção

No servidor, obtenha o IPv4 da Tailscale com `tailscale ip -4` e configure no `.env` protegido:

```env
OBSERVABILITY_BIND_ADDRESS=100.x.y.z
GRAFANA_ADMIN_USER=financebot_admin
GRAFANA_ADMIN_PASSWORD=uma-senha-longa-e-aleatoria
```

O deploy executa:

```bash
docker compose -f compose.observability.yml -f compose.observability.prod.yml up -d
```

Acesse os painéis somente quando o dispositivo estiver conectado à mesma tailnet:

- Grafana: `http://100.x.y.z:3000`
- Prometheus: `http://100.x.y.z:9090`
- Loki: `http://100.x.y.z:3100/ready`

Essas portas são vinculadas ao endereço da Tailscale e não ficam publicadas na interface pública.
O firewall do servidor deve continuar bloqueando acesso externo fora da interface Tailscale.

Para definir outras credenciais localmente sem versioná-las:

```bash
OBSERVABILITY_BIND_ADDRESS=127.0.0.1 \
GRAFANA_ADMIN_USER=seu-usuario GRAFANA_ADMIN_PASSWORD=sua-senha \
  docker compose -f compose.observability.yml up -d
```

## Métricas das aplicações

- API: `http://localhost:8082/actuator/prometheus`
- Bot: `http://localhost:8083/actuator/prometheus`

Actuator usa portas de gerenciamento separadas. Nos containers de produção, `8082` e `8083`
são acessíveis somente pela rede privada do Compose de produção; métricas e detalhes operacionais
não ficam expostos pela interface pública da API ou do bot.

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
