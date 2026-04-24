# Finance Bot

Assistente financeiro conversacional via Telegram, integrado a uma API em **Java + Spring Boot**, criado para tornar o controle financeiro mais simples, rápido e natural.

## Sobre o projeto

O Finance Bot nasceu de um problema real: a dificuldade de controlar gastos no dia a dia de forma prática e consistente.

Muitas vezes, registrar uma despesa ou receita em aplicativos tradicionais exige um fluxo engessado:
- abrir o app
- preencher formulário
- selecionar categoria
- escolher conta
- salvar manualmente

Na prática, isso gera atrito.  
Muita gente acaba anotando em bloco de notas, mensagens soltas ou simplesmente deixa de registrar os gastos.

A proposta deste projeto é transformar esse processo em uma experiência mais natural: permitir que o usuário registre e consulte informações financeiras **conversando com um bot no Telegram**, quase como se estivesse mandando mensagem para um amigo.

## Exemplos de uso

O usuário pode escrever mensagens como:

- `gastei 200 reais no mercado ontem`
- `recebi 1500 de salário`
- `quanto gastei esse mês?`
- `quanto gastei com mercado esse mês?`
- `me dá a análise desse mês`

## O que o projeto já faz

### Integração com Telegram
- conexão da conta do usuário com o Telegram por código temporário
- consulta de perfil
- consulta de resumo/status
- consulta de análise financeira
- atualização de renda base mensal
- desvinculação da conta

### Registro de transações
- criação de despesas e receitas por linguagem natural
- criação de transações parceladas
- suporte a transações recorrentes
- preview da operação antes de salvar
- confirmação ou cancelamento da operação
- persistência real da transação no backend
- resolução automática de conta padrão
- resolução automática de categoria

### Consultas naturais
- total gasto no mês
- total recebido no mês
- consultas com filtro por categoria
- consulta de conta padrão
- consulta de parcelas ativas e resumo de parcelamentos

### Recursos da API
- autenticação com login e registro
- CRUD de contas, categorias, transações e recorrências
- análise financeira e pré-checagens
- endpoints dedicados para consumo do bot
- estrutura de dashboard no backend

## Arquitetura

O repositório está organizado em duas aplicações Spring Boot que trabalham em conjunto:

- **financebot-api**: aplicação backend na raiz do repositório, com regras de negócio, autenticação, persistência e endpoints REST
- **financebot-telegram-bot**: aplicação separada dentro da pasta `financebot-telegram-bot`, responsável pela interação conversacional e integração com a API

### Responsabilidades da API
- autenticação e segurança
- vinculação de usuário com Telegram
- persistência de contas, categorias e transações
- persistência de transações recorrentes e parceladas
- regras de negócio financeiras
- análise financeira
- endpoints para dashboard
- resolução automática de conta padrão e categoria

### Responsabilidades do bot
- receber mensagens do Telegram
- interpretar comandos e mensagens naturais
- gerar preview e pedir confirmação
- consultar a API
- enviar respostas conversacionais para o usuário

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

### Infraestrutura / ambiente
- Docker
- Docker Compose
- PostgreSQL 17
- Redis
- RabbitMQ

## Funcionalidades em evolução

O projeto continua evoluindo com foco em experiência conversacional e arquitetura. Algumas das próximas melhorias planejadas são:

- edição da operação pendente antes da confirmação
- respostas mais naturais e contextuais
- consultas mais inteligentes com filtros adicionais
- expansão da camada de dashboard
- evolução do uso de mensageria e cache na arquitetura
- integração com IA para enriquecer a interpretação das mensagens e consultas

## Objetivo futuro com IA

Uma das evoluções planejadas é aprofundar o uso de IA no fluxo de interpretação para tornar o bot ainda mais natural e preciso ao entender:

- intenções
- contexto
- categorias
- contas
- consultas mais complexas

A ideia não é substituir as regras de negócio do backend, mas enriquecer a camada de entendimento da linguagem.

## Fluxo de criação de transação

1. O usuário envia uma mensagem natural no Telegram
2. O bot interpreta a intenção
3. O bot apresenta um preview da operação
4. O usuário confirma ou cancela
5. A API resolve conta padrão e categoria
6. A transação é salva no banco de dados

## Como executar localmente

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker / Docker Compose
- token de bot do Telegram

### Serviços de infraestrutura
Suba os serviços locais com:

```bash
docker compose up -d
```

O `compose.yaml` disponibiliza:

- PostgreSQL em `localhost:5432`
- RabbitMQ em `localhost:5672` e painel em `localhost:15672`
- Redis em `localhost:6379`

### Configuração da API
A API principal roda na porta `8080` e lê, por padrão:

- banco PostgreSQL em `jdbc:postgresql://localhost:5432/financebot`
- Redis em `localhost:6379`
- RabbitMQ em `localhost:5672`

Se precisar sobrescrever segredos locais, crie o arquivo:

```properties
src/main/resources/application-secret.properties
```

Para liberar o frontend web local, a API aceita por padrão a origem:

```properties
app.cors.allowed-origins=http://localhost:5173
```

Se precisar, sobrescreva essa propriedade no `application-secret.properties` com uma ou mais origens separadas por vírgula.

### Subindo a API
Na raiz do projeto:

```bash
./mvnw spring-boot:run
```

As migrations do Flyway são executadas automaticamente na inicialização.

### Configuração do bot do Telegram
O bot roda na porta `8081` e depende da API em `http://localhost:8080`.

Defina a variável de ambiente:

```bash
export TELEGRAM_BOT_TOKEN=seu_token_aqui
```

Se necessário, ajuste também o arquivo:

```properties
financebot-telegram-bot/src/main/resources/application.properties
```

Principalmente a propriedade:

```properties
financebot.api.base-url=http://localhost:8080
```

## Testes e qualidade

Os testes da API usam o perfil `test` com banco H2 em memória. Para executar:

```bash
./mvnw test
```

Para gerar também o relatório de cobertura do JaCoCo:

```bash
./mvnw verify
```

O relatório é gerado em:

```text
target/site/jacoco/index.html
```

## SonarQube local

Para subir uma instância local do SonarQube:

```bash
docker compose -f compose.sonar.yml up -d
```

Depois, acesse:

```text
http://localhost:9000
```

## Subindo o bot
Dentro da pasta do bot:

```bash
cd financebot-telegram-bot
../mvnw spring-boot:run
```

## Fluxo básico de uso
1. Suba a infraestrutura com Docker Compose.
2. Inicie a API.
3. Inicie o bot do Telegram.
4. Gere o código de vínculo pela API/autenticação da aplicação.
5. Confirme o vínculo no Telegram.
6. Envie mensagens naturais para registrar ou consultar informações financeiras.

## Open source

A ideia é manter o projeto aberto para estudo, aprendizado e colaboração.

Se quiser acompanhar a evolução, sugerir melhorias ou contribuir, fique à vontade para abrir uma issue ou pull request.

## Licença

Este projeto está licenciado sob a licença [MIT](LICENSE).

## Autores

- **Bryan Pinheiro** — [@BryanPinheiro77](https://github.com/BryanPinheiro77)
- **Luiz Fernando** — [@LuizFernandoReisFranca](https://github.com/luizfernandoreisfranca)

