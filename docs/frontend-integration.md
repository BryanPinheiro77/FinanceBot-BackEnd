# Frontend e integração

O frontend não faz parte deste repositório. Esta página registra a fronteira conhecida entre o frontend e a API para evitar que a documentação fique implícita.

## Integração atual

- URL base local da API: `http://localhost:8080`;
- autenticação da API: JWT por Bearer; sessão web publicada via servidor do frontend e cookie protegido em [web-session.md](security/web-session.md);
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

## Dashboard mensal

A #164 adiciona `GET /dashboard/monthly-summary?month=AAAA-MM`: totais, resultado,
contagem de lançamentos, série diária completa e despesas por categoria, sempre
limitados ao usuário autenticado. Regras e exemplos em [Resumo mensal](dashboard.md).
A API deve ser publicada antes do frontend consumidor; falhas não representam
totais zerados. O navegador usa o proxy de sessão web e não recalcula regras
financeiras nem soma páginas de transações para construir o resumo.

## Repositório do frontend

O frontend é mantido em [FinanceBot-FrontEnd](https://github.com/BryanPinheiro77/FinanceBot-FrontEnd).
Setup, stack e processo de publicação pertencem ao README/documentação daquele
repositório. Aqui ficam os contratos da API e a [ordem de compatibilidade da sessão
web](security/web-session.md), incluindo a assinatura opcional da origem usada
nas cotas de login/cadastro. A publicação coordenada foi validada na [v1.11.1](releases/v1.11.1.md). Os guias de configuração continuam exigindo validação própria para outras instalações.

## Sessões revogáveis

O contrato de autenticação agora inclui access de até 15 minutos, refresh opaco com
rotação, revogação por sessão no logout e inatividade de 1 hora com aviso de 60
segundos. Consultar endpoints, cookies, concorrência entre abas e publicação
coordenada em [sessão web](security/web-session.md#access-token-refresh-e-ociosidade).
A interface é implementada exclusivamente no repositório FinanceBot-FrontEnd.
