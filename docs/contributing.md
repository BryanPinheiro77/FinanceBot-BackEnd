# Guia de contribuição

## Antes de começar

1. Confira uma issue ou descreva claramente o objetivo.
2. Crie uma branch: `feature/...`, `fix/...`, `refactor/...`, `docs/...` ou `chore/...`.
3. Leia `AGENTS.md` e o documento relacionado à área alterada.

## Durante o desenvolvimento

- Prefira mudanças pequenas e reversíveis.
- Adicione testes para regras novas e cenários de erro.
- Para mudanças de API, atualize exemplos/documentação e considere o bot.
- Para mudanças de banco, crie uma migration Flyway nova.
- Nunca inclua secrets, `.env`, dumps ou dados pessoais reais.

## Validação local

```bash
./mvnw clean verify
(cd financebot-telegram-bot && ./mvnw clean verify)
git diff --check
```

## Pull request

O PR deve explicar objetivo, alterações, testes e impactos operacionais. Use `.github/pull_request_template.md`. Atualize `CHANGELOG.md` para mudanças relevantes e a documentação quando comandos, contratos ou operação forem alterados.

O CI executa os builds/testes dos dois módulos. O deploy de produção só ocorre a partir de `main`, conforme [Deploy](deployment.md).

## Commits

Use mensagens curtas e descritivas, preferencialmente no formato `tipo: descrição`, por exemplo `fix: valida valor de parcela`. Um commit deve representar uma mudança coerente.
