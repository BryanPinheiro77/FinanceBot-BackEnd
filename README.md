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
- desvinculação da conta

### Registro de transações
- criação de despesas e receitas por linguagem natural
- preview da operação antes de salvar
- confirmação ou cancelamento da operação
- persistência real da transação no backend
- resolução automática de conta padrão
- resolução automática de categoria

### Consultas naturais
- total gasto no mês
- total recebido no mês
- consultas com filtro por categoria
- estrutura inicial para consultas com filtro por conta

## Arquitetura

O projeto está dividido em dois módulos principais:

- **financebot-api**: backend com regras de negócio, autenticação, persistência e endpoints REST
- **financebot-telegram-bot**: bot do Telegram responsável pela interação conversacional e integração com a API

### Responsabilidades da API
- autenticação e segurança
- vinculação de usuário com Telegram
- persistência de contas, categorias e transações
- regras de negócio financeiras
- análise financeira
- resolução automática de conta padrão e categoria

### Responsabilidades do bot
- receber mensagens do Telegram
- interpretar comandos e mensagens naturais
- gerar preview e pedir confirmação
- consultar a API
- enviar respostas conversacionais para o usuário

## Tecnologias utilizadas

### Backend / API
- Java
- Spring Boot
- Spring Security
- JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- Lombok

### Bot
- Java
- Spring Boot
- Telegram Bot API
- RestClient

### Infraestrutura / ambiente
- Docker
- Docker Compose

## Funcionalidades em evolução

O projeto continua evoluindo com foco em experiência conversacional e arquitetura. Algumas das próximas melhorias planejadas são:

- edição da operação pendente antes da confirmação
- respostas mais naturais e contextuais
- consultas mais inteligentes com filtros adicionais
- dashboard para visão mais ampla e visual da vida financeira
- uso de Redis e RabbitMQ em evoluções futuras
- integração com IA para enriquecer a interpretação das mensagens

## Objetivo futuro com IA

Uma das evoluções planejadas é integrar IA ao fluxo de interpretação para tornar o bot ainda mais natural e preciso ao entender:

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

> Ajuste esta seção conforme a estrutura exata do seu repositório.

### Pré-requisitos
- Java 21+ (ou a versão usada no projeto)
- Maven
- PostgreSQL
- Docker / Docker Compose
- Bot do Telegram configurado
- variáveis de ambiente ou `application.yml` configurados

### Passos gerais
1. Clonar o repositório
2. Configurar banco e variáveis de ambiente
3. Rodar as migrations com Flyway
4. Subir a API
5. Subir o bot do Telegram
6. Gerar código de vínculo no sistema
7. Conectar a conta no Telegram

## Open source

A ideia é manter o projeto aberto para estudo, aprendizado e colaboração.

Se quiser acompanhar a evolução, sugerir melhorias ou contribuir, fique à vontade para abrir uma issue ou pull request.

## Licença

Este projeto está licenciado sob a licença **MIT**.

Consulte o arquivo `LICENSE` para mais detalhes.

## Autor

Desenvolvido por Bryan.
