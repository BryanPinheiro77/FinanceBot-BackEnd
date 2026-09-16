# Finance Bot

Assistente financeiro conversacional com API REST e integração via Telegram para registro, consulta e análise de finanças pessoais em linguagem natural.

> **Estado do projeto:** o FinanceBot está em desenvolvimento inicial. O repositório é open source e prioriza uma instalação self-hosted; um eventual serviço hospedado terá termos, suporte e políticas próprios.

![Badge](https://img.shields.io/badge/Java-21-red)
![Badge](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![Badge](https://img.shields.io/badge/PostgreSQL-17-blue)
![Badge](https://img.shields.io/badge/Telegram-Bot%20Integration-2AABEE)
![Badge](https://img.shields.io/badge/Tests-JUnit%20%7C%20Mockito-orange)
![Badge](https://img.shields.io/badge/Quality-SonarQube%20%7C%20JaCoCo-success)

---

## Sobre o projeto

O Finance Bot nasceu para reduzir o atrito do controle financeiro no dia a dia.

Em vez de depender apenas de formulários tradicionais, o projeto permite registrar despesas, receitas e consultas financeiras por linguagem natural, com foco em uma experiência mais prática, rápida e conversacional.

O repositório é dividido em dois módulos principais:

- **financebot-backend**: backend REST com regras de negócio, autenticação, persistência e endpoints da aplicação
- **financebot-telegram-bot**: módulo responsável pela interação com o usuário no Telegram e consumo da API

---

## Componentes do sistema

### API REST
- autenticação com login e registro
- CRUD de contas, categorias, transações e recorrências
- análise financeira e pré-checagens
- suporte ao fluxo web
- endpoints dedicados para integração com o bot

### Bot do Telegram
- interpretação de mensagens naturais
- preview antes da confirmação
- criação de transações por conversa
- consultas financeiras e resumos
- suporte a parcelamentos e fluxo contextual
- consumo assíncrono de lembretes pelo RabbitMQ

### Infraestrutura local
- PostgreSQL
- Redis
- RabbitMQ
- SonarQube via Docker Compose

---

## Principais funcionalidades

### Registro financeiro
- criação de despesas e receitas por linguagem natural
- criação de transações parceladas
- suporte a transações recorrentes
- criação e entrega de lembretes financeiros
- resolução automática de conta padrão
- resolução automática de categoria

### Consultas e análise
- total gasto no mês
- total recebido no mês
- consultas por categoria
- consulta de conta padrão
- consulta de parcelas ativas
- análise financeira mensal
- análise de viabilidade para compra parcelada

### Integração com Telegram
- vínculo de conta com código temporário
- consulta de perfil
- atualização de renda base mensal
- respostas formatadas para o bot
- confirmação ou cancelamento de operações

---

## Arquitetura

A arquitetura atual segue uma organização em camadas com evolução gradual para uma abordagem mais próxima de Clean Architecture.

### Estrutura atual
- **Controllers**: endpoints REST e entrada HTTP
- **Services**: regras de negócio e orquestração
- **Repositories**: acesso a dados com Spring Data JPA
- **DTOs**: contratos de entrada e saída
- **Entities**: persistência com JPA
- **Security**: autenticação/autorização com JWT

### Direção arquitetural
O projeto está evoluindo para reduzir acoplamento entre domínio, aplicação, infraestrutura e interfaces, sem forçar refatorações amplas fora do escopo de cada mudança.

---

## Tecnologias utilizadas

### Backend / API
- Java 21
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- Spring Actuator
- Redis
- RabbitMQ
- Lombok

### Bot
- Java 21
- Spring Boot 4
- Telegram Bot API
- RestClient
- Spring AMQP / RabbitMQ

### Testes e qualidade
- JUnit 5
- Mockito
- H2
- JaCoCo
- SonarQube

---

## Documentação

- [Índice da documentação](docs/README.md)
- [Como contribuir](CONTRIBUTING.md)
- [Código de conduta](CODE_OF_CONDUCT.md)
- [Política de segurança](SECURITY.md)
- [Privacidade](PRIVACY.md)
- [Suporte](SUPPORT.md)
- [Roadmap](docs/roadmap.md)
- [Governança de releases](docs/releases.md)
- [Desenvolvimento local](docs/development.md)
- [Arquitetura](docs/architecture.md)
- [API](docs/api.md)
- [Bot Telegram](docs/telegram-bot.md)
- [Contribuição](docs/contributing.md)
- [Deploy](docs/deployment.md)
- [Servidor remoto](docs/operations/remote-server.md)
- [Observabilidade](docs/observability.md)

## Como executar localmente

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker / Docker Compose
- token de bot do Telegram

### Variáveis de ambiente da API
Copie `.env.example` para `.env` na raiz do projeto e substitua os valores locais. O arquivo `.env`
é ignorado pelo Git e nunca deve ser versionado:

```env
DB_URL=jdbc:postgresql://localhost:5432/financebot
DB_USER=postgres
DB_PASSWORD=sua-senha

REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USER=default
REDIS_PASSWORD=sua-senha
REDIS_SSL_ENABLED=false

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=financebot_local
RABBITMQ_PASSWORD=sua-senha
RABBITMQ_VHOST=/financebot
RABBITMQ_SSL_ENABLED=false

JWT_SECRET=sua_chave_secreta
JWT_EXPIRATION=86400000
FINANCEBOT_DATA_ENCRYPTION_KEY=base64_de_32_bytes_gerada_com_seguranca
FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID=primary
FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID=primary
FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS=

CORS_ALLOWED_ORIGINS=http://localhost:5173

# Opcional: scheduler de recorrências (padrão: todos os dias à meia-noite)
FINANCEBOT_RECURRING_SCHEDULER_CRON=0 0 0 * * *

# Opcional: publicação de lembretes vencidos (padrão: 60 segundos)
FINANCEBOT_REMINDERS_PUBLISH_INTERVAL=60000
FINANCEBOT_REMINDERS_QUEUE_MESSAGE_TTL_MS=86400000

# Opcional: Actuator local da API (padrões: porta 8082 e endereço 127.0.0.1)
FINANCEBOT_MANAGEMENT_PORT=8082
FINANCEBOT_MANAGEMENT_ADDRESS=127.0.0.1
```

### Subindo a API com Docker
```bash
docker compose -f compose.local.yml up -d --build
```

Esse compose sobe a aplicação `financebot-backend` na porta `8080`.

> O PostgreSQL, Redis e RabbitMQ precisam estar disponíveis no ambiente local ou em containers próprios apontados pelas variáveis acima.

### Subindo a API sem Docker
Com PostgreSQL, Redis e RabbitMQ já disponíveis:

```bash
./mvnw spring-boot:run
```

### Variáveis de ambiente do bot
Copie `financebot-telegram-bot/.env.example` para `financebot-telegram-bot/.env` e substitua os
valores locais. O arquivo `.env` é ignorado pelo Git e nunca deve ser versionado:

```env
TELEGRAM_BOT_TOKEN=seu_token_aqui
FINANCEBOT_API_URL=http://localhost:8080
TELEGRAM_INTERNAL_TOKEN=use-o-mesmo-token-configurado-na-api
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USER=default
REDIS_PASSWORD=sua-senha
REDIS_SSL_ENABLED=false
TELEGRAM_STATE_STORE=redis
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=financebot_local
RABBITMQ_PASSWORD=sua-senha
RABBITMQ_VHOST=/financebot
RABBITMQ_SSL_ENABLED=false

# Opcional: Actuator local do bot (padrões: porta 8083 e endereço 127.0.0.1)
FINANCEBOT_MANAGEMENT_PORT=8083
FINANCEBOT_MANAGEMENT_ADDRESS=127.0.0.1
```

### Subindo o bot com Docker
```bash
cd financebot-telegram-bot
docker compose -f compose.local.yml up -d --build
```

O bot fica disponível na porta `8081` e consome a API configurada em `FINANCEBOT_API_URL`.

### Subindo o bot sem Docker

```bash
cd financebot-telegram-bot
./mvnw spring-boot:run
```

### Serviços úteis

- API: `http://localhost:8080`
- Health check da API: `http://localhost:8080/api/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Bot: `http://localhost:8081`
- Métricas da API: `http://localhost:8082/actuator/prometheus`
- Métricas do bot: `http://localhost:8083/actuator/prometheus`

---

## Deploy

O deploy automatizado está configurado via GitHub Actions em `.github/workflows/deploy.yml`.

O fluxo atual executa em push para a branch `main`, usando um runner `self-hosted`:

```bash
docker compose -f compose.prod.yml up -d --build
```

O backend e o bot são implantados separadamente:

- backend: raiz do repositório, usando `compose.prod.yml`
- bot: diretório `financebot-telegram-bot`, usando `compose.prod.yml`

### Requisitos do ambiente de produção

- runner self-hosted com acesso ao repositório
- Docker e Docker Compose instalados
- rede Docker externa `backend-network` criada previamente
- arquivos `.env` configurados na raiz do projeto e em `financebot-telegram-bot/`
- PostgreSQL, Redis e RabbitMQ acessíveis pelas variáveis de ambiente

Exemplo de criação da rede externa:

```bash
docker network create backend-network
```

---

## Testes e qualidade

### Rodando testes
```bash
./mvnw test
```

### Gerando cobertura
```bash
./mvnw verify
```

Relatório do JaCoCo:
```text
target/site/jacoco/index.html
```

### SonarQube local
```bash
docker compose -f compose.sonar.yml up -d
```

Acesse:
```text
http://localhost:9000
```

---

## Fluxo básico de uso

1. Suba a infraestrutura local.
2. Inicie a API.
3. Inicie o bot do Telegram.
4. Gere o código de vínculo pela aplicação.
5. Confirme o vínculo no Telegram.
6. Envie mensagens naturais para registrar ou consultar informações financeiras.

---

## Automação de review

O repositório possui suporte a review técnico assistido por IA para análise de Pull Requests.

Arquivos relacionados:
- `.github/ai-review-instructions.md`
- `.github/prompts/pr-review-prompt.md`
- `.review/codex-review-prompt.md`
- `scripts/review-pr-with-codex.sh`

Esse fluxo é voltado para revisão técnica, identificação de riscos e melhoria contínua do código. Ele não substitui validação manual, testes e revisão humana final.

### Execução local
```bash
bash scripts/review-pr-with-codex.sh
```
ou
```bash
.\scripts\review-pr-with-codex.ps1
```


---

## Evolução do projeto

Algumas frentes em andamento:
- melhoria da experiência conversacional
- consultas mais inteligentes
- expansão da camada de dashboard
- evolução do uso de mensageria e cache
- aprofundamento do uso de IA para interpretação de mensagens

---

## Changelog

As mudanças de versão estão documentadas em [CHANGELOG.md](CHANGELOG.md).

---

## Open source

A ideia é manter o projeto aberto para estudo, aprendizado e colaboração.

Se quiser acompanhar a evolução, sugerir melhorias ou contribuir, fique à vontade para abrir uma issue ou pull request.

---

## Licença

Este projeto está licenciado sob a licença [MIT](LICENSE).

---

## Autores

- **Bryan Pinheiro** — [@BryanPinheiro77](https://github.com/BryanPinheiro77)
- **Luiz Fernando** — [@LuizFernandoReisFranca](https://github.com/luizfernandoreisfranca)
