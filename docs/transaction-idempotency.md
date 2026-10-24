# Idempotência de criação REST

A issue [#215](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/215)
protege reenvios e concorrência nos endpoints:

- `POST /transactions`;
- `POST /transactions/installments`;
- `POST /transactions/installments/existing`.

## Contrato do consumidor

Envie o header opcional `Idempotency-Key` com um identificador aleatório por
operação lógica, preferencialmente UUID. Guarde a chave, o endpoint e o conteúdo
até concluir ou reconciliar a operação. Após perder uma resposta ou receber um
erro transitório, repita com os mesmos dados e a mesma chave. Não gere outra chave
automaticamente para uma tentativa da mesma operação.

```http
POST /transactions
Authorization: Bearer <jwt-do-usuario>
Idempotency-Key: 8af1c270-6421-4239-a755-81471649236b
Content-Type: application/json
```

| Situação | Resultado |
|---|---|
| Primeira criação válida | `201` e resposta original, persistida com os lançamentos |
| Mesma chave, usuário, endpoint e conteúdo durante a validade | `201` e a resposta original, sem nova criação |
| Mesma chave/escopo com outro conteúdo | `409`, `ProblemDetail` com título `Idempotency conflict` |
| Erro de validação ou falha com rollback | A chave não fica consumida; é possível corrigir a operação e repetir |
| Resposta cifrada ilegível/corrompida ou falha transitória de reserva | `503`, `ProblemDetail` e `Retry-After: 1`; repetir com a mesma chave |
| Sem o header | Contrato anterior: cada chamada é uma nova criação |
| Chave expirada ou já removida pela retenção | A chave pode iniciar uma nova operação; o consumidor deve reconciliar antes de reenviar |

Chaves têm de 8 a 128 caracteres, começando por letra ou número; os demais podem
ser letras/números ASCII, `.`, `_`, `:` e `-`. Chaves vazias, curtas, longas,
duplicadas no header ou com outros caracteres retornam `400`. O valor é sensível
a maiúsculas e minúsculas. Não coloque dados pessoais ou secrets na chave.

O escopo é o usuário autenticado e o endpoint: duas contas podem usar a mesma
chave sem compartilhar respostas, e operações em endpoints diferentes têm
reservas independentes. O hash considera o DTO validado, com campos ordenados e
números normalizados: ordem/espaçamento do JSON e `100.00` versus `100.0000` não
mudam a identidade. Alterar strings de negócio, valores ou referências muda o
conteúdo e causa conflito após uma criação confirmada.

O replay é uma fotografia da resposta da criação, incluindo IDs e análise
financeira. Ele não recalcula a análise nem representa necessariamente o estado
atual após edições/exclusões; use as consultas normais para atualizar a tela.
O CORS permite enviar `Idempotency-Key` e já expõe `Retry-After` nas origens
configuradas. O frontend permanece em seu próprio repositório.

## Atomicidade e concorrência

A migration nova V17 cria `transaction_idempotency_requests`, com chave primária
por usuário, operação e hash da chave, FK de usuário com exclusão em cascata e
índice de expiração. Migrations anteriores não foram alteradas.

Com o header, reserva, criação, análise financeira e salvamento da resposta usam
a mesma transação e conexão PostgreSQL. Uma falha em qualquer etapa reverte todo
o conjunto. O `INSERT ... ON CONFLICT DO UPDATE ... WHERE` reserva ou renova uma
chave expirada; também mantém a trava da linha quando já existe uma resposta
válida, conforme o [contrato de INSERT do PostgreSQL](https://www.postgresql.org/docs/17/sql-insert.html).
Requisições concorrentes aguardam a transação anterior antes de criar ou repetir
a resposta. O processamento idempotente tem timeout transacional de 30 segundos.

Falhas não são armazenadas como respostas finais. Se a confirmação do commit se
perder, o reenvio com a mesma chave encontra o conjunto confirmado ou executa de
novo após um rollback: não há uma reserva confirmada separadamente do lançamento.

## Retenção e proteção de dados

| Variável opcional | Padrão | Limite |
|---|---|---|
| `FINANCEBOT_TRANSACTION_IDEMPOTENCY_TTL_SECONDS` | `86400` (24 horas) | 60 a 604800 segundos |
| `FINANCEBOT_TRANSACTION_IDEMPOTENCY_CLEANUP_BATCH_SIZE` | `1000` | 1 a 10000 registros por execução |
| `FINANCEBOT_TRANSACTION_IDEMPOTENCY_CLEANUP_INTERVAL` | `600000` | Intervalo de limpeza em milissegundos |

A validade começa na reserva e não é prorrogada pelo replay. O relógio usado é o
do PostgreSQL, compartilhado entre réplicas. Expiração permite reutilização da
reserva imediatamente, sem esperar pela limpeza. O job remove registros expirados
em lotes, usando `SKIP LOCKED` para não disputar registros em uso. Remover um marcador
não exclui o lançamento financeiro correspondente.

A remoção física depende do intervalo, tamanho dos lotes e backlog. Monitore esse
volume para dimensionar a retenção; a validade de 24 horas não promete que toda
linha expirada será apagada exatamente nesse instante. As réplicas devem usar a
mesma configuração.

Chave e conteúdo são identificados por hashes SHA-256; esses hashes são metadados
de comparação, não criptografia. A resposta financeira é cifrada com o keyring
AES-GCM já existente. Os dados autenticados associados à cifra incluem usuário,
operação, hash da chave e do conteúdo, impedindo copiar uma resposta cifrada entre
reservas sem invalidar a autenticação. Não se armazenam chave bruta ou corpo do
pedido em claro nessa tabela.

Não há novo secret. Preserve chaves antigas enquanto houver snapshots que as
utilizem: o job de rotação de renda não recriptografa essas respostas. Consulte o
passo adicional de [Rotação de chaves](security/key-rotation.md) antes de revogar
uma chave. Uma resposta ilegível não autoriza criar novamente a operação.

## Validação e limites

`TransactionIdempotencyIntegrationTest` usa PostgreSQL 17 temporário, Flyway até
V17, JWT real e requisições MockMvc sem transação de teste externa. Cobre os três
endpoints, replay, conflito, normalização, isolamento de usuários/operações,
compatibilidade sem header, falhas de escrita, retenção, CORS e integridade das
cifras. A concorrência de chaves novas e expiradas é sincronizada por uma trava
externa, com dois workers observados aguardando em `pg_stat_activity` antes da
liberação. Nenhum repositório, cifra ou gerenciador transacional é mockado.

```bash
./mvnw --batch-mode --no-transfer-progress -Dtest=TransactionIdempotencyIntegrationTest test
./mvnw --batch-mode --no-transfer-progress -Dsurefire.runOrder=alphabetical clean verify
git diff --check
```

Docker é obrigatório para essa suíte; a configuração de Colima está em
[Desenvolvimento local](development.md).

Validação local em 24/10/2026: 535 testes da API, incluindo 30 cenários de
integração desta suíte, e 189 testes do bot aprovados sem falhas, erros ou skips.
A suíte completa da API também validou a compatibilidade da V17 com os testes H2.
`git diff --check` aprovado.

A garantia depende de o consumidor adotar o header e conservar a chave. Não há
deduplicação por semelhança de texto/valor, nem garantia após a validade. Endpoints
de edição, exclusões e recorrências ficam fora desse
contrato inicial. A consistência de recorrências continua descrita em
[Consistência de transações](transaction-consistency.md). Failover com perda de
dados confirmados depende das garantias de durabilidade e backup da instalação.

## Confirmações do Telegram

Validação da #217: `./mvnw clean verify` aprovado com 548 testes da API e
200 testes do bot, sem falhas, erros ou skips (PostgreSQL e Redis via containers).

Os três endpoints internos `POST /telegram/transactions`,
`/telegram/transactions/installments` e `/telegram/transactions/installments/existing`
aceitam o mesmo header opcional `Idempotency-Key`. Continuam exigindo
`X-Internal-Service-Token` e retornando `200` sem corpo, inclusive em replay.
Sem chave, preservam o comportamento anterior. Chaves inválidas retornam `400`,
conteúdo diferente retorna `409` e indisponibilidade retorna `503`.

O usuário vinculado ao `telegramId` define o escopo da chave. As operações
Telegram têm namespaces separados dos endpoints REST. Resolução de conta e
categoria, criação e marcador participam da mesma transação; um replay válido
não resolve novamente contas/categorias e não cria lançamentos. O marcador
armazena uma confirmação booleana criptografada, sem payload financeiro.

O bot gera um UUID por pendência e persiste junto com o conteúdo e a expiração.
Com `TELEGRAM_STATE_STORE=redis`, o estado sobrevive ao reinício do processo,
dependendo da retenção/persistência do próprio Redis. O modo `memory` mantém
retry protegido durante a vida do processo, mas perde pendências no reinício.
Não há fallback automático para memória se o Redis falhar.

A pendência dura **15 minutos**, sem renovação em leituras ou retries. O marcador
Telegram dura o maior valor entre a retenção configurada da API e **uma hora**,
para permanecer válido além da janela de retry do bot. Não recoloque manualmente
pendências expiradas em circulação. Expiração, cancelamento ou um novo pedido
explícito encerram a identidade daquela operação; não se promete deduplicação
permanente de mensagens do Telegram.

Antes de chamar a API, o bot marca a tentativa atomicamente no store. A chave e o
conteúdo ficam congelados: não é possível editar ou substituir uma pendência
já tentada sem resolver a confirmação ou cancelar explicitamente. Isso também
vale para respostas de erro: confirme novamente após resolver a causa ou cancele
para iniciar outra operação. Cancelar uma tentativa não desfaz uma transação que
a API já tenha gravado; o bot orienta conferir os lançamentos antes de recriar.
Uma confirmação antiga só remove a pendência se a chave ainda for a mesma.

O Redis armazena dados pessoais e financeiros da pendência em JSON, como os
contextos de conversa existentes: mantenha-o privado, com autenticação e TLS
quando houver tráfego fora de uma rede confiável. O TTL não substitui políticas
de backups/segurança do Redis. Não registrar payloads ou chaves em logs.

Os testes usam PostgreSQL 17/Flyway e Redis 7 reais em containers para validar
replay, conflito, autenticação interna, isolamento, rollback, concorrência,
retomada após reinício, bloqueio de edição e remoção condicional.
