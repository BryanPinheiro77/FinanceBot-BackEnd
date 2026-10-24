# Isolamento entre usuários

A issue [#134](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/134)
é validada pela suíte `UserIsolationIntegrationTest`. O objetivo é impedir IDOR:
usar o identificador de um recurso de outra pessoa para ler ou alterar seus dados.

## Contrato e cobertura

O proprietário vem da autenticação validada pelo backend. Um `userId` enviado na
query ou no corpo não permite escolher outro proprietário. As consultas por ID
de recursos alheios retornam `404` com `ProblemDetail`, assim como IDs inexistentes,
sem confirmar a existência do recurso a quem não tem acesso.

| Fluxo | Verificação |
|---|---|
| Contas, categorias, transações e recorrências | Consulta por ID de outro usuário retorna `404`; o proprietário consegue consultar |
| Contas, categorias, transações, recorrências e lembretes | Listagem contém somente recursos próprios, inclusive com `userId` alheio na query |
| Edição e exclusão dos cinco recursos | Tentativas sobre IDs alheios retornam `404` e preservam os dados dos dois usuários |
| Edição e exclusão próprias | Controles positivos confirmam que operações autorizadas continuam funcionando |
| Ativação e desativação de recorrências | Outro usuário não pode mudar o estado; o proprietário pode |
| Criação e edição de transações e recorrências | Contas e categorias alheias são recusadas separadamente |
| Parcelamentos novos e existentes | Contas e categorias alheias são recusadas sem criar parcelas parciais |
| Pré-checagens de transação e parcelamento | Referências alheias são recusadas |
| Criação de lembretes | Não permite associar recorrência de outro usuário |
| Filtros de transações por conta ou categoria alheia | Retornam página vazia e total zero |
| Criação com `userId` alheio no corpo | O novo recurso continua pertencendo ao usuário autenticado |
| Listagens sem JWT ou com JWT inválido | Retornam `401` |

Os ataques por ID e por referência são executados nas duas direções entre os
usuários. As tentativas de escrita são seguidas de flush, limpeza do contexto JPA
e releitura para verificar o estado persistido, incluindo exclusão lógica de categorias.
Não há endpoint de consulta individual de lembrete; a preservação desse recurso
é verificada pela listagem do proprietário.

## Execução

```bash
./mvnw --batch-mode --no-transfer-progress -Dtest=UserIsolationIntegrationTest test
./mvnw --batch-mode --no-transfer-progress -Dsurefire.runOrder=alphabetical clean verify
git diff --check
```

Validação local em 24/10/2026: `clean verify` com ordem alfabética aprovado,
com 456 testes da API, incluindo os 98 cenários desta suíte, sem falhas, erros ou
skips. A verificação de whitespace também foi aprovada.

A suíte cria duas contas de teste por `/auth/register` e utiliza os JWTs retornados
em requisições MockMvc, passando pela cadeia real do Spring Security, controllers,
services/use cases e persistência. Os recursos são criados pelos próprios endpoints,
sem mocks de autenticação, regras de negócio ou repositórios. Cada cenário tem uma
transação com rollback e um banco H2 exclusivo da suíte. Agendamentos são adiados ou
desabilitados nesse contexto para evitar trabalho concorrente durante os testes.

Esta cobertura não executa ataques contra produção. H2 com schema criado pelo
Hibernate não valida migrations Flyway nem particularidades do PostgreSQL. Os
endpoints internos do Telegram usam outro contrato de confiança, baseado no token
de serviço, e não fazem parte desta suíte. Não se trata de uma auditoria completa
de segurança, de expiração de JWT ou de rate limiting.
