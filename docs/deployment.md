# Deploy

## Modelo atual

O deploy é acionado por push na branch `main` pelo workflow `.github/workflows/deploy.yml`. Ele executa em um runner GitHub Actions `self-hosted` instalado no notebook remoto e roda Docker Compose para a API e para o bot.

O diretório do checkout deve ser configurado como variável privada no runner self-hosted. Ele não deve ser exposto neste repositório público.

## Pré-requisitos

- Tailscale conectado;
- runner self-hosted online;
- Docker e Docker Compose funcionando para o usuário do runner;
- repositório clonado no diretório privado configurado no runner;
- rede Docker externa `backend-network` criada;
- `.env` da API na raiz e `.env` do bot em `financebot-telegram-bot/`;
- `TELEGRAM_INTERNAL_TOKEN` com o mesmo valor nos dois `.env`, mantido em segredo;
- variáveis `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USER` e `RABBITMQ_PASSWORD`
  configuradas também no `.env` do bot;
- `FINANCEBOT_DATA_ENCRYPTION_KEY` configurada no `.env` da API como uma chave Base64
  de 32 bytes; ela não deve ser versionada nem compartilhada;
- `FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID`, `FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID`
  e `FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS` coerentes com o keyring documentado em
  [security/key-rotation.md](security/key-rotation.md);
- PostgreSQL, Redis e RabbitMQ acessíveis pelos hosts definidos nos `.env`.
- Redis configurado com `REDIS_USER` e `REDIS_PASSWORD`, sem porta pública;
- RabbitMQ configurado com usuário de aplicação, `RABBITMQ_VHOST` e permissões mínimas;
- backup criptografado e restauração verificada conforme [operations/backup-restore.md](operations/backup-restore.md).
- `OBSERVABILITY_BIND_ADDRESS` configurada com o IPv4 da Tailscale do servidor;
- `GRAFANA_ADMIN_USER` e `GRAFANA_ADMIN_PASSWORD` definidos no `.env` da raiz, com senha longa
  e exclusiva;

No GitHub, configure a variável de repositório `FINANCEBOT_DEPLOY_PATH` com o caminho do checkout no runner. O valor real deve ficar nas configurações privadas do repositório, não em YAML, documentação ou código.

Para receber notificação quando o deploy ou algum health check falhar, configure o secret `DEPLOY_FAILURE_WEBHOOK_URL` com um webhook privado compatível. Se o secret não existir, o workflow apenas registra que a notificação foi ignorada.

O endpoint público de confirmação inicial do vínculo Telegram permanece em `/users/telegram/confirm-link`. Os demais endpoints de `/telegram/**` exigem o header interno enviado pelo bot.

Criação inicial da rede:

```bash
docker network create backend-network
```

## Publicação

1. Faça merge na `main` após o CI passar.
2. Acompanhe o workflow no GitHub.
3. No servidor, confirme os containers com `docker compose -f compose.prod.yml ps` em cada módulo
   e `docker compose -f compose.observability.yml -f compose.observability.prod.yml ps`.
4. O workflow valida automaticamente a API, o bot, Prometheus, Grafana e Loki, tentando por até
   60 segundos cada. As portas da observabilidade ficam acessíveis somente pelo endereço da
   Tailscale configurado em `OBSERVABILITY_BIND_ADDRESS`.

## Rollback operacional

O rollback atual é manual: identifique o commit anterior saudável, volte o checkout do servidor para esse commit e execute novamente os dois comandos `docker compose ... up -d --build`. Antes de automatizar rollback, confirme backup do PostgreSQL e compatibilidade das migrations.

## Melhorias planejadas

- substituir `git pull` no servidor por checkout imutável do SHA do workflow;
- adicionar health check pós-deploy e notificação de falha;
- separar ambientes e usar aprovação para produção;
- avaliar armazenamento seguro de secrets.
