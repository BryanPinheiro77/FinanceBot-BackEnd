# Resumo mensal do dashboard

Contrato adicionado para a [issue #164](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/164).
A interface é implementada no repositório FinanceBot-FrontEnd.

## Consulta

`GET /dashboard/monthly-summary?month=2026-11` exige JWT Bearer da API.
O parâmetro `month` é obrigatório, no formato `AAAA-MM`, entre `0001-01` e
`9999-12`. Ausência ou formato inválido retorna `400`; falta de autenticação ou
token inválido retorna `401`. O proprietário sempre vem da autenticação; não há
filtro por usuário fornecido pelo consumidor.

```json
{
  "month": "2026-11",
  "startDate": "2026-11-01",
  "endDate": "2026-11-30",
  "totalIncome": 1000.01,
  "totalExpense": 50.30,
  "balance": 949.71,
  "transactionCount": 3,
  "daily": [
    { "date": "2026-11-01", "totalIncome": 1000.01, "totalExpense": 0.00 },
    { "date": "2026-11-02", "totalIncome": 0.00, "totalExpense": 20.30 }
  ],
  "expensesByCategory": [
    { "categoryId": 8, "categoryName": "Transporte", "totalExpense": 30.00 },
    { "categoryId": 9, "categoryName": "Alimentação", "totalExpense": 20.30 }
  ]
}
```

O exemplo abrevia `daily`: a resposta real contém **todos os dias** do mês em
ordem crescente, inclusive dias sem movimentação e 29 de fevereiro nos anos
bissextos. `expensesByCategory` contém somente categorias com despesas no período,
ordenadas por valor decrescente, com desempate por ID.

## Semântica financeira

- A seleção usa a data do lançamento, com início e fim do mês inclusivos, em todas
  as contas do usuário autenticado; não usa a data de criação nem conversão UTC.
- Receitas e despesas são somadas no backend com `BigDecimal`. `balance` é a
  diferença entre receitas e despesas do período e pode ser negativa.
- A renda mensal cadastrada e o saldo inicial das contas **não** são somados às
  receitas. O resultado não representa saldo bancário disponível nem projeção.
- Parcelas já persistidas entram no mês da data de cada parcela, mesmo que futura.
  Recorrências ainda não materializadas em lançamentos não entram no resumo.
- Categorias desativadas continuam no histórico. O agrupamento usa ID, não apenas
  nome, e retorna o nome atual da categoria.
- Meses vazios retornam totais e contagem zerados, série diária zerada e categorias
  vazias. Falhas nunca devem ser apresentadas como mês sem lançamentos.

Uma consulta agregada por data, tipo e categoria alimenta totais, série diária e
categorias, evitando inconsistência entre consultas e carregamento das descrições
ou entidades de cada lançamento. Não há cache compartilhado no serviço.

## Integração web e ordem de publicação

No frontend, o navegador consulta
`GET /api/financebot/dashboard/monthly-summary?month=AAAA-MM`; o proxy de sessão
encaminha somente esse GET adicional, com o Bearer obtido do cookie HttpOnly e
headers `private, no-store`/`Vary: Cookie`. Nenhum JWT é entregue ao JavaScript.

Publicar a API com esse contrato antes do frontend consumidor. Uma versão antiga
da API não fornece esse endpoint; a interface deve mostrar erro com nova tentativa,
sem usar dados fictícios ou calcular um resumo parcial a partir de páginas de
transações. O frontend mantém apenas formatação e escala visual dos gráficos;
totais, agrupamentos e resultado vêm desta resposta.

## Validação

`DashboardIntegrationTest` cobre JWT real, isolamento entre usuários, parâmetros
inválidos, meses vazios, centavos, limites inclusivos, ano bissexto, parcelas,
categorias desativadas e resultado negativo, usando H2 em modo PostgreSQL. O teste
não substitui validação da instalação PostgreSQL em ambiente isolado.

As evidências de navegador e testes da interface ficam no repositório do frontend.
Não executar publicação de produção como parte desses testes.

### Resultado local — 05/11/2026

`./mvnw clean verify` aprovado: **573 testes, zero falhas, erros ou testes
ignorados**, incluindo os 13 cenários de `DashboardIntegrationTest`. A execução
usou o socket do contexto Colima ativo com `DOCKER_HOST` e
`TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=/var/run/docker.sock`, conforme
[Desenvolvimento local](development.md); PostgreSQL, Redis e RabbitMQ dos testes
foram containers isolados. O novo resumo foi exercitado com H2; os testes
PostgreSQL existentes cobriram a compatibilidade geral da aplicação.

No frontend, 22 testes Vitest e 18 testes de dashboard/4 de sessão Playwright
aprovados, além de typecheck, build e lint dos arquivos alterados. Capturas e
limitações estão em `FinanceBot-FrontEnd/docs/dashboard.md`. A validação não executou publicação ou alteração operacional no servidor.
