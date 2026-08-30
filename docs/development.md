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
TELEGRAM_INTERNAL_TOKEN=gere-um-token-local-forte-e-nao-compartilhe
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
TELEGRAM_INTERNAL_TOKEN=use-o-mesmo-token-configurado-na-api
# Opcional: enriquecimento de mensagens ambíguas por um endpoint compatível com OpenAI
FINANCEBOT_AI_ENABLED=false
FINANCEBOT_AI_ENDPOINT=https://seu-provedor.example/v1/chat/completions
FINANCEBOT_AI_API_KEY=seu-token-do-provedor
FINANCEBOT_AI_MODEL=gpt-4o-mini
FINANCEBOT_AI_TIMEOUT=10s
```

Quando habilitada, a IA recebe somente o texto da mensagem e retorna uma intenção
estruturada. Ela nunca persiste dados: mensagens reconhecidas pelo parser determinístico
seguem primeiro pela IA quando o recurso está habilitado; respostas inválidas ou falhas do
provedor fazem fallback para o parser existente. O token deve permanecer apenas no ambiente
local/seguro.

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
