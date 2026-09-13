# Desenvolvimento local

## Pré-requisitos

- Java 21;
- Docker e Docker Compose;
- Git;
- token do bot somente se for executar o módulo Telegram.

Os Maven Wrappers (`./mvnw`) são a forma preferida de executar Maven.

### Java 21 no macOS

Em Macs com Homebrew, instale e configure o JDK antes de executar os testes:

```bash
brew install openjdk@21
echo 'export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"' >> ~/.zshrc
echo 'export PATH="$JAVA_HOME/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

O comando deve mostrar a versão 21.

## Configuração da API

Copie `.env.example` para `.env` na raiz. O arquivo real é ignorado pelo Git:

```env
DB_NAME=financebot
DB_USER=postgres
DB_PASSWORD=postgres
DB_PORT=5432
DB_URL=jdbc:postgresql://localhost:5432/financebot
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USER=default
REDIS_PASSWORD=troque-por-uma-senha-local-forte
REDIS_SSL_ENABLED=false
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_MANAGEMENT_PORT=15672
RABBITMQ_USER=financebot_local
RABBITMQ_PASSWORD=troque-por-uma-senha-local-forte
RABBITMQ_VHOST=/financebot
RABBITMQ_SSL_ENABLED=false
JWT_SECRET=troque-por-uma-chave-local-com-mais-de-64-caracteres
JWT_EXPIRATION=86400000
FINANCEBOT_DATA_ENCRYPTION_KEY=base64_de_32_bytes_gerada_com_seguranca
FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID=primary
FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID=primary
FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS=
CORS_ALLOWED_ORIGINS=http://localhost:5173
TELEGRAM_INTERNAL_TOKEN=gere-um-token-local-forte-e-nao-compartilhe
FINANCEBOT_RECURRING_SCHEDULER_CRON=0 0 0 * * *
FINANCEBOT_REMINDERS_PUBLISH_INTERVAL=60000
FINANCEBOT_REMINDERS_QUEUE_MESSAGE_TTL_MS=86400000
```

```bash
docker compose -f compose.local.yml up -d
./mvnw spring-boot:run
```

Endpoints úteis: `http://localhost:8080/api/health` e `http://localhost:8080/swagger-ui/index.html`.

## Configuração do bot

Copie `financebot-telegram-bot/.env.example` para `financebot-telegram-bot/.env`:

```env
TELEGRAM_BOT_TOKEN=seu-token-local
FINANCEBOT_API_URL=http://localhost:8080
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USER=default
REDIS_PASSWORD=use-a-mesma-senha-do-compose-local
REDIS_SSL_ENABLED=false
TELEGRAM_STATE_STORE=memory
TELEGRAM_CONVERSATION_CONTEXT_TTL=30m
TELEGRAM_QUERY_CONTEXT_TTL=30m
TELEGRAM_INTERNAL_TOKEN=use-o-mesmo-token-configurado-na-api
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=financebot_local
RABBITMQ_PASSWORD=use-a-mesma-senha-do-compose-local
RABBITMQ_VHOST=/financebot
RABBITMQ_SSL_ENABLED=false
# Opcional: enriquecimento de mensagens ambíguas por um endpoint compatível com OpenAI
FINANCEBOT_AI_ENABLED=false
FINANCEBOT_AI_ENDPOINT=https://seu-provedor.example/v1/chat/completions
FINANCEBOT_AI_API_KEY=seu-token-do-provedor
FINANCEBOT_AI_MODEL=gpt-4o-mini
FINANCEBOT_AI_TIMEOUT=10s
FINANCEBOT_AI_TRANSCRIPTION_ENDPOINT=https://api.openai.com/v1/audio/transcriptions
FINANCEBOT_AI_TRANSCRIPTION_MODEL=whisper-1
```

Quando habilitada, a IA recebe somente o texto da mensagem e retorna uma intenção
estruturada. Ela nunca persiste dados: mensagens reconhecidas pelo parser determinístico
seguem primeiro pela IA quando o recurso está habilitado; respostas inválidas ou falhas do
provedor fazem fallback para o parser existente. O token deve permanecer apenas no ambiente
local/seguro.

Execute-o com `cd financebot-telegram-bot && ./mvnw spring-boot:run`.

## RabbitMQ e lembretes

Com `compose.local.yml` em execução, a interface de gerenciamento fica em
`http://localhost:15672`. Use as credenciais locais configuradas em `RABBITMQ_USER` e
`RABBITMQ_PASSWORD`.

As portas de Redis e RabbitMQ são vinculadas a `127.0.0.1`, portanto não ficam expostas na
rede local. O Redis exige `REDIS_PASSWORD`, e o RabbitMQ usa um usuário e vhost dedicados.
Credenciais locais nunca devem ser reutilizadas em produção.

O fluxo de lembretes usa:

- exchange: `financebot.notifications`;
- fila: `financebot.notifications.telegram`;
- routing key: `notification.reminder.telegram`.

A API publica os lembretes vencidos e o bot consome a fila. Por isso, os dois módulos precisam
apontar para a mesma instância do RabbitMQ.

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
