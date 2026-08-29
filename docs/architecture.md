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
API ──> RabbitMQ
Bot ──> Redis (quando habilitado)
```

## Organização da API

O código combina organização em camadas e partes já migradas para uma abordagem hexagonal/Clean Architecture:

- controllers/adapters de entrada: HTTP e DTOs;
- services/application: orquestração e regras de aplicação;
- domain: entidades, value objects, validações e regras puras;
- repositories/adapters de saída: persistência via Spring Data/JPA;
- config, security e integrações: detalhes de framework e serviços externos.

A migração é incremental. O objetivo é manter o `domain` independente, expor ports na aplicação e deixar adapters implementarem detalhes externos.

## Persistência

O schema é controlado pelo Flyway em `src/main/resources/db/migration`. O Hibernate usa `ddl-auto=validate`; alterações de schema devem ser migrations novas, numeradas e revisadas.

## Regras arquiteturais

- Controllers não contêm regra financeira.
- O bot não duplica regras de negócio da API.
- DTOs ficam nas bordas e não devem vazar para o domínio.
- Repositórios Spring Data não devem ser dependência de regras de domínio.
- Dados monetários usam `BigDecimal`.
- Mudanças que cruzam API e bot devem atualizar ambos os contratos e seus testes.
