# FinanceBot — contexto para agentes

## Visão geral

FinanceBot é um sistema de controle financeiro pessoal composto por uma API REST em Java 21 + Spring Boot (`src/`), um bot do Telegram (`financebot-telegram-bot/`) e PostgreSQL, Redis e RabbitMQ executados via Docker Compose. O frontend não está neste repositório; contratos de integração estão em `docs/frontend-integration.md`.

Leia `README.md` e `docs/README.md` antes de fazer alterações de arquitetura ou infraestrutura.
As skills compartilhadas do projeto estão catalogadas em `docs/ai-skills.md` e armazenadas em `.agents/skills/`.

## Regras de trabalho

- Responda e documente em português brasileiro.
- Preserve o escopo do pedido e evite refatorações amplas sem necessidade.
- Não comite secrets, tokens, senhas, arquivos `.env`, dumps ou dados pessoais.
- Não faça commit ou push sem confirmação explícita do mantenedor.
- Não execute deploy em produção, rollback ou comandos destrutivos no servidor sem autorização explícita.
- Não invente detalhes do servidor remoto; marque informações desconhecidas como pendências.
- Não documente ou altere o frontend como se ele estivesse neste repositório; mantenha a integração limitada aos contratos da API.
- Nunca use `docker compose down -v` sem confirmar que os dados locais podem ser apagados.
- Não altere ou apague migrations Flyway já aplicadas; crie uma migration nova.
- Use `BigDecimal` para valores monetários.
- Controllers/adapters recebem e validam entrada; regras de negócio ficam em services/use cases.
- Não faça o domínio depender de Spring, JPA, Telegram ou infraestrutura quando a mudança tocar a arquitetura.
- Toda mudança de comportamento deve ter testes; mudanças de contrato ou operação devem atualizar a documentação.
- Não diga que um teste foi executado sem executá-lo e registrar o resultado.
- Antes de ações externas ou irreversíveis, explique o impacto e peça confirmação.

## Comandos principais

```bash
./mvnw clean verify
./mvnw spring-boot:run
(cd financebot-telegram-bot && ./mvnw clean verify)
docker compose -f compose.local.yml up -d
```

## Estrutura relevante

- `src/main/java/com/financebot`: API, domínio, casos de uso, adapters e integrações.
- `src/main/resources/db/migration`: migrations Flyway; nunca edite uma migration já aplicada.
- `financebot-telegram-bot/src`: aplicação e handlers do Telegram.
- `compose.local.yml`: dependências locais da API.
- `compose.prod.yml`: container da API em produção.
- `financebot-telegram-bot/compose.prod.yml`: container do bot em produção.
- `.github/workflows/ci.yml`: validação de pull requests e branches.
- `.github/workflows/deploy.yml`: deploy no runner self-hosted do notebook.
- `docs/`: documentação operacional e de contribuição.

## Fluxo recomendado

1. Criar uma branch a partir de `main`.
2. Alterar o menor número de módulos possível.
3. Adicionar ou ajustar testes.
4. Rodar os testes do módulo afetado e `git diff --check`.
5. Revisar secrets/configuração e atualizar `CHANGELOG.md` ou `docs/` quando aplicável.
6. Abrir PR usando o template existente.

## Deploy

O deploy ocorre após push na `main`, em um GitHub Actions runner self-hosted instalado no notebook remoto. O procedimento está em `docs/deployment.md` e `docs/operations/remote-server.md`.
