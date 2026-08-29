# Back-end da API

## Responsabilidade

A API é a fonte das regras de negócio financeiras. Ela expõe autenticação, usuários, contas, categorias, transações, recorrências, dashboard, análise financeira e endpoints específicos para o bot.

## Execução

```bash
./mvnw spring-boot:run
```

Por padrão, a API escuta na porta `8080`. O health check público é `GET /api/health` e a documentação OpenAPI fica em `/swagger-ui/index.html`.

## Principais grupos de endpoints

| Grupo | Prefixo |
|---|---|
| Autenticação | `/auth` |
| Usuário autenticado | `/users` |
| Contas | `/accounts` |
| Categorias | `/categories` |
| Transações | `/transactions` |
| Recorrências | `/recurring-transactions` |
| Dashboard | `/dashboard` |
| Análise | `/analysis` |
| Integração Telegram | `/telegram` |

Endpoints protegidos usam autenticação JWT. Consulte o Swagger e os controllers para o contrato vigente; esta página é um mapa, não substitui a especificação OpenAPI.

## Dependências de runtime

- PostgreSQL: dados da aplicação e migrations Flyway;
- Redis: cache e estado de integrações conforme configuração;
- RabbitMQ: mensageria da aplicação.

## Alterações de contrato

Ao modificar request, response, autenticação ou regras consumidas pelo bot:

1. atualize testes da API;
2. atualize o cliente/DTO correspondente no bot;
3. verifique o impacto no frontend;
4. atualize Swagger e esta documentação quando necessário.
