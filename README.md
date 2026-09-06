# Finance Bot

Assistente financeiro conversacional com API REST e integração via Telegram para registro, consulta e análise de finanças pessoais em linguagem natural.

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

### Testes e qualidade
- JUnit 5
- Mockito
- H2
- JaCoCo
- SonarQube

---

## Como executar localmente

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker / Docker Compose
- token de bot do Telegram

### Variáveis de ambiente da API
Crie um arquivo `.env` na raiz do projeto ou exporte as variáveis no terminal:

```env
DB_URL=jdbc:postgresql://localhost:5432/financebot
DB_USER=postgres
DB_PASSWORD=sua-senha

REDIS_HOST=localhost
REDIS_PORT=6379

RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASSWORD=sua-senha

JWT_SECRET=sua_chave_secreta
JWT_EXPIRATION=86400000

CORS_ALLOWED_ORIGINS=http://localhost:5173

# Opcional: scheduler de recorrências (padrão: todos os dias à meia-noite)
FINANCEBOT_RECURRING_SCHEDULER_CRON=0 0 0 * * *
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
Crie um arquivo `.env` em `financebot-telegram-bot/` ou exporte as variáveis no terminal:

```env
TELEGRAM_BOT_TOKEN=seu_token_aqui
FINANCEBOT_API_URL=http://localhost:8080
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

