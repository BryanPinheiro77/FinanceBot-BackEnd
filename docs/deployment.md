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
- PostgreSQL, Redis e RabbitMQ acessíveis pelos hosts definidos nos `.env`.

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
3. No servidor, confirme os containers com `docker compose -f compose.prod.yml ps` em cada módulo.
4. O workflow valida automaticamente `http://localhost:8080/api/health` e `http://localhost:8081/actuator/health`, tentando por até 60 segundos cada.

## Rollback operacional

O rollback atual é manual: identifique o commit anterior saudável, volte o checkout do servidor para esse commit e execute novamente os dois comandos `docker compose ... up -d --build`. Antes de automatizar rollback, confirme backup do PostgreSQL e compatibilidade das migrations.

## Melhorias planejadas

- substituir `git pull` no servidor por checkout imutável do SHA do workflow;
- adicionar health check pós-deploy e notificação de falha;
- documentar backup/restauração do PostgreSQL;
- separar ambientes e usar aprovação para produção;
- avaliar armazenamento seguro de secrets.
