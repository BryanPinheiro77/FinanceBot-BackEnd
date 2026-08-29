# Back-end do bot Telegram

## Responsabilidade

O módulo `financebot-telegram-bot/` é um serviço Java 21 separado. Ele recebe mensagens do Telegram, interpreta intenções, mantém contexto de conversa e chama a API FinanceBot. As regras financeiras devem permanecer na API.

## Execução

```bash
cd financebot-telegram-bot
./mvnw spring-boot:run
```

O serviço usa a porta `8081` e precisa de `TELEGRAM_BOT_TOKEN` e `FINANCEBOT_API_URL`. O armazenamento de contexto pode ser `memory` ou Redis, conforme `TELEGRAM_STATE_STORE`.

O endpoint técnico `GET /actuator/health` é usado pelo deploy para confirmar que o processo iniciou. Ele não expõe detalhes das dependências (`show-details=never`).

## Interpretação de mensagens

`TelegramIntentService` funciona como fachada e normaliza a mensagem antes de delegar para parsers especializados:

- `TelegramQueryParser`: consultas financeiras, análises e parcelamentos;
- `TelegramTransactionParser`: despesas, receitas e parcelamentos.

Os parsers preservam a ordem das regras existentes. A extração de valores, datas, contas e categorias continua centralizada no serviço durante esta etapa incremental; a issue #96 poderá aprofundar essa separação.

## Fluxo principal

```text
Mensagem Telegram
        ↓
Bot / handlers / interpretação
        ↓
Cliente HTTP da API
        ↓
Regra financeira e persistência na API
        ↓
Resposta formatada para o Telegram
```

## Cuidados

- Não duplicar cálculo financeiro no bot.
- Não colocar token do Telegram no código ou em logs.
- Mudanças nos endpoints da API devem ser refletidas em `FinanceBotApiClient` e seus testes.
- Alterações no formato das mensagens devem ter testes dos handlers/formatters.
- O bot pode usar Redis para preservar contexto entre reinícios; em `memory`, esse contexto é perdido ao reiniciar.
