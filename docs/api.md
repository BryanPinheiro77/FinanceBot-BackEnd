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
| Lembretes | `/reminders` |
| Dashboard | `/dashboard` |
| Análise | `/analysis` |
| Integração Telegram | `/telegram` |

Endpoints protegidos usam autenticação JWT. Consulte o Swagger e os controllers para o contrato vigente; esta página é um mapa, não substitui a especificação OpenAPI.

Lembretes pendentes são reservados em lotes pelo bot em `POST /telegram/reminders/pending/claim`
e confirmados em `PATCH /telegram/reminders/{id}/sent` usando o token interno. Em caso de falha
antes do envio, o bot libera a reserva em `PATCH /telegram/reminders/{id}/release`; reservas
abandonadas expiram após cinco minutos.
O bot também cria lembretes a partir de frases como `me lembre dia 10 de pagar o aluguel`;
quando o ano ou o mês não é informado, ele usa a próxima ocorrência válida da data.
Para recorrências ativas, frases como `me avise dois dias antes da internet vencer` vinculam
o lembrete pela descrição e calculam o aviso a partir do próximo vencimento.

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
