# Desenvolvimento local

## Pré-requisitos

- Java 21;
- Docker e Docker Compose;
- Git;
- token do bot somente se for executar o módulo Telegram.

Os Maven Wrappers (`./mvnw`) são a forma preferida de executar Maven.

## Configuração da API

Crie `.env` na raiz. O arquivo é ignorado pelo Git:

```env
DB_NAME=financebot
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=5432
DB_URL=jdbc:postgresql://localhost:5432/financebot
REDIS_HOST=localhost
REDIS_PORT=6379
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=guest
JWT_SECRET=troque-por-uma-chave-local-com-mais-de-64-caracteres
JWT_EXPIRATION=86400000
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

```bash
docker compose -f compose.local.yml up -d
./mvnw spring-boot:run
```

Endpoints úteis: `http://localhost:8080/api/health` e `http://localhost:8080/swagger-ui/index.html`.

## Configuração do bot

Crie `financebot-telegram-bot/.env`:

```env
TELEGRAM_BOT_TOKEN=seu-token-local
FINANCEBOT_API_URL=http://localhost:8080
REDIS_HOST=localhost
REDIS_PORT=6379
TELEGRAM_STATE_STORE=memory
TELEGRAM_CONVERSATION_CONTEXT_TTL=30m
TELEGRAM_QUERY_CONTEXT_TTL=30m
```

Execute-o com `cd financebot-telegram-bot && ./mvnw spring-boot:run`.

## Testes e qualidade

```bash
./mvnw clean verify
(cd financebot-telegram-bot && ./mvnw clean verify)
git diff --check
```

O relatório JaCoCo da API fica em `target/site/jacoco/index.html`. Para SonarQube local, use `docker compose -f compose.sonar.yml up -d`.

## Limpeza

```bash
docker compose -f compose.local.yml down
```

Use `down -v` somente quando quiser apagar os volumes locais.
