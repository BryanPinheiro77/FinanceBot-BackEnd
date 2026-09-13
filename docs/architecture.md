# Arquitetura

## Visão geral

O repositório contém dois módulos independentes:

1. **API**: autenticação, usuários, contas, categorias, transações, recorrências, dashboard, análise financeira e endpoints de integração.
2. **Bot Telegram**: recebe mensagens, interpreta intenções, mantém contexto e chama a API.

Em produção, os dois módulos são containers separados conectados à rede Docker externa `backend-network`.

```text
Cliente web ──HTTP/JWT──> API ──> PostgreSQL
Telegram ──> Bot ──HTTP──> API
API ──> Redis
API ──evento──> RabbitMQ ──mensagem──> Bot ──> Telegram
Bot ──> Redis (quando habilitado)
```

## Mapa do repositório

- `src/main/java/com/financebot`: API REST, domínio, casos de uso, adapters e integrações;
- `src/main/resources/db/migration`: migrations Flyway da API;
- `financebot-telegram-bot/src`: aplicação, handlers e adapters do bot;
- `compose.local.yml`: dependências para desenvolvimento local;
- `compose.prod.yml` e `financebot-telegram-bot/compose.prod.yml`: execução dos serviços em produção;
- `observability/` e `compose.observability.yml`: stack local de métricas, logs e dashboards;
- `docs/`: contratos, operação e decisões úteis para colaboradores.

O frontend não faz parte deste repositório. Seus contratos de integração estão em
[frontend-integration.md](frontend-integration.md).

## Organização da API

O código combina organização em camadas e partes já migradas para uma abordagem hexagonal/Clean Architecture:

- controllers/adapters de entrada: HTTP e DTOs;
- services/application: orquestração e regras de aplicação;
- domain: entidades, value objects, validações e regras puras;
- repositories/adapters de saída: persistência via Spring Data/JPA;
- config, security e integrações: detalhes de framework e serviços externos.

A migração é incremental. O objetivo é manter o `domain` independente, expor ports na aplicação e deixar adapters implementarem detalhes externos.

## Notificações assíncronas

A API reserva lembretes vencidos e publica eventos por uma porta de saída. O adapter RabbitMQ
converte o evento de aplicação para a mensagem de infraestrutura e a envia para a exchange
`financebot.notifications` com a routing key `notification.reminder.telegram`.

O bot consome a fila durável `financebot.notifications.telegram`, envia a mensagem ao Telegram
e confirma o lembrete pela API. Antes do envio, valida o identificador e a data na API para
ignorar cópias antigas que possam permanecer na fila. Se o envio falhar, libera a reserva para
uma publicação futura. Producer e consumer conhecem RabbitMQ; os casos de uso dependem apenas
de ports.

## Persistência

O schema é controlado pelo Flyway em `src/main/resources/db/migration`. O Hibernate usa `ddl-auto=validate`; alterações de schema devem ser migrations novas, numeradas e revisadas.

## Regras arquiteturais

- Controllers não contêm regra financeira.
- O bot não duplica regras de negócio da API.
- DTOs ficam nas bordas e não devem vazar para o domínio.
- Repositórios Spring Data não devem ser dependência de regras de domínio.
- Dados monetários usam `BigDecimal`.
- Mudanças que cruzam API e bot devem atualizar ambos os contratos e seus testes.

## Como evoluir a arquitetura

Ao tocar em uma área existente, preserve o padrão local e migre de forma incremental. Uma
mudança nova deve manter as regras de negócio longe de Spring, JPA, Telegram e RabbitMQ sempre
que a separação já existir no módulo. Quando uma mudança precisar introduzir um adapter ou uma
porta, documente a direção da dependência e cubra o fluxo com testes do caso de uso e da borda
afetada.
