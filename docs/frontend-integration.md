# Frontend e integração

O frontend não faz parte deste repositório. Esta página registra a fronteira conhecida entre o frontend e a API para evitar que a documentação fique implícita.

## Integração atual

- URL base local da API: `http://localhost:8080`;
- autenticação: JWT;
- documentação interativa: `/swagger-ui/index.html`;
- health check: `/api/health`;
- CORS: configurado por `CORS_ALLOWED_ORIGINS`.

Login e cadastro podem retornar `429` com um `ProblemDetail` e `Retry-After` em
segundos; o CORS expõe esse header às origens permitidas. O consumidor deve aguardar
esse prazo antes de repetir. Se o contador de autenticação estiver indisponível,
a resposta será `503`, também com `Retry-After`. Detalhes em
[Limites de autenticação](security/auth-rate-limiting.md).

Na criação ou importação de parcelamentos, valores com fração de centavo e totais
insuficientes para manter cada parcela em pelo menos R$ 0,01 retornam `400`.
`monthlyAmount` também deve ser representável em centavos. A prévia usa o plano
da criação. Para reenviar uma criação após perder a resposta, preserve um
`Idempotency-Key` gerado por operação lógica, junto do endpoint e conteúdo.
Repetições devolvem o `201` e a resposta original; conteúdo diferente com a mesma
chave retorna `409`. A validade padrão é de 24 horas. Sem chave ou após expirar,
reconciliar os lançamentos antes de reenviar. Consulte
[Idempotência de criação REST](transaction-idempotency.md) e
[Consistência de transações](transaction-consistency.md).

## Quando o frontend for adicionado

O repositório do frontend deve ter seu próprio `AGENTS.md`, README e documentação de setup. Nesse projeto, devemos registrar apenas contratos compartilhados: URL da API, autenticação, endpoints utilizados, variáveis públicas e compatibilidade de versões.

Pendência: identificar o repositório oficial do frontend e documentar seus comandos, stack, ambiente local e processo de deploy sem misturar responsabilidades com este backend.
