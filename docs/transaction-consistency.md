# Consistência de transações

A issue [#182](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/182)
verifica rollback, concorrência e preservação de valores nos fluxos de
parcelamentos, edição de transações e execução de recorrências.

## Garantias e regras

- O parcelamento novo e a importação de parcelas restantes são gravados como um
  lote transacional. Se uma inserção falhar, o lote inteiro deve ser revertido.
- Uma edição inválida ou recusada pelo banco deve preservar todos os campos do
  lançamento anterior e o saldo calculado da conta.
- Na execução de uma recorrência, ocorrências geradas, próxima data, data da
  última execução e estado ativo participam da mesma transação.
- Execuções concorrentes da mesma recorrência aguardam o bloqueio da linha. Após
  a primeira concluir, a segunda considera a agenda atualizada. Veja o contrato de
  [bloqueios de linha do PostgreSQL](https://www.postgresql.org/docs/17/explicit-locking.html).
- Após uma falha confirmada com rollback, a recorrência pode ser reexecutada sem
  repetir o lote. Reexecutar um período já concluído não cria novas ocorrências.

### Centavos e parcelamentos

O total precisa ser positivo, representável em centavos e suficiente para pelo
menos R$ 0,01 por parcela do plano completo. Essas regras também se aplicam à
importação de parcelas restantes. Se a importação informar `monthlyAmount`, esse
valor também precisa ser representável em centavos antes de ser multiplicado.
Valores como `1.001` ou `0.015` são recusados com `400` e `ProblemDetail`, em vez
de serem arredondados silenciosamente na persistência. Zeros decimais adicionais,
como `1.000`, não representam fração de centavo e continuam válidos.

A distribuição mantém o arredondamento habitual, mas reserva pelo menos um
centavo para cada parcela futura antes de definir o valor atual. O restante fica
na última parcela. Exemplos:

| Total / parcelas | Resultado |
|---|---|
| R$ 1.000,00 / 3 | R$ 333,33 + R$ 333,33 + R$ 333,34 |
| R$ 0,15 / 10 | Cinco parcelas de R$ 0,02 e cinco de R$ 0,01 |
| R$ 0,03 / 5 | `400`: total insuficiente para cinco parcelas positivas |

Na importação, apenas o sufixo correspondente às parcelas restantes é gravado;
o cálculo é o mesmo do plano completo. A pré-checagem de criação também usa esse
plano, para que a projeção corresponda aos valores realmente gravados. A correção
vale para novos cálculos; avaliar dados históricos exige uma auditoria separada.

## Testes com PostgreSQL real

`TransactionConsistencyIntegrationTest` inicia PostgreSQL 17 temporário via
Testcontainers, aplica as migrations Flyway existentes até V16 e valida o schema
com Hibernate. Não há novos scripts de migration nesta mudança.

Os testes não têm uma transação externa com rollback automático. A fixture é
confirmada antes da operação, os serviços/casos de uso usam suas transações reais
e as verificações JDBC leem o estado confirmado por outras conexões.

| Cenário | Verificação |
|---|---|
| Falha na segunda inserção de um parcelamento novo ou existente | Nenhuma parcela parcial, preservação do lançamento anterior e saldo; nova tentativa após remover a falha grava o lote completo |
| Falha no commit de uma edição | Todos os campos anteriores e o saldo permanecem intactos; edição válida posterior funciona |
| Edição HTTP com valor zero ou categoria incompatível | `400` sem alterar dados confirmados |
| Falha na segunda ocorrência de uma recorrência | Lançamentos e agenda revertidos juntos; retomada cria cada ocorrência uma vez e encerra a recorrência na data final |
| Duas execuções concorrentes | Ambas observadas esperando o bloqueio em `pg_stat_activity`; após liberar, uma cria o lote e a outra cria zero registros |
| Total insuficiente, total com fração de centavo ou mensalidade fracionária | `400` sem escrita parcial |
| Pequenos valores válidos e parcelas restantes | Todas as parcelas positivas e totais exatos no banco |
| Prévia versus gravação | Projeção do próximo mês e parcelas futuras iguais à análise dos dados confirmados |

As falhas são provocadas por constraints `CHECK` temporárias no banco exclusivo
do teste. O SQLSTATE `23514` confirma a origem do erro. As constraints são removidas
ao final de cada teste; nenhum repositório ou gerenciador transacional é mockado.
O teste de concorrência mantém uma trava externa até observar dois workers
aguardando a linha, evitando um falso positivo por execuções sequenciais.

`InstallmentPlanFactoryTest` também percorre pequenos totais de 2 a 100 centavos,
com até 20 parcelas, verificando positividade, soma exata e correspondência das
parcelas restantes com o plano completo.

Com Docker ativo, execute:

```bash
./mvnw --batch-mode --no-transfer-progress -Dtest=TransactionConsistencyIntegrationTest test
./mvnw --batch-mode --no-transfer-progress -Dsurefire.runOrder=alphabetical clean verify
(cd financebot-telegram-bot && ./mvnw --batch-mode --no-transfer-progress clean verify)
git diff --check
```

Configuração de socket para Colima está em [Desenvolvimento local](development.md).

Validação local em 24/10/2026: 504 testes da API, incluindo 15 cenários desta suíte
PostgreSQL, e 189 testes do bot aprovados, sem falhas, erros ou skips. A suíte da
API foi executada com ordem alfabética. Os seis cenários HTTP iniciais de centavos
e a comparação da prévia foram reproduzidos como falhas antes das correções.
`git diff --check` também aprovado.

## Limites

Esses testes exercitam o PostgreSQL temporário e não alteram produção. Eles não
validam queda física do servidor, failover ou todos os níveis de isolamento; o
teste concorrente usa o padrão `READ COMMITTED` do PostgreSQL.

A criação REST de transações e parcelamentos passou a aceitar o header opcional
`Idempotency-Key`, em contrato próprio de [Idempotência de criação REST](transaction-idempotency.md).
Sem ele, reenviar uma criação confirmada ainda pode duplicar registros. A retomada
validada nesta suíte cobre rollback confirmado e a agenda das recorrências;
validade da chave, replay e concorrência de reenvios têm testes próprios.
