# Guia de contribuição

Este guia resume o fluxo técnico. As regras públicas de participação estão em [CONTRIBUTING.md](../CONTRIBUTING.md), o comportamento esperado está em [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) e relatos de segurança devem seguir [SECURITY.md](../SECURITY.md).

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
- Nunca use dados reais de usuários para reproduzir um teste; anonimize o exemplo ou crie um fixture.

## Validação local

```bash
./mvnw clean verify
(cd financebot-telegram-bot && ./mvnw clean verify)
git diff --check
```

## Pull request

O PR deve explicar objetivo, alterações, testes e impactos operacionais. Use `.github/pull_request_template.md`. Atualize `CHANGELOG.md` para mudanças relevantes e a documentação quando comandos, contratos ou operação forem alterados.

O CI executa os builds/testes dos dois módulos. O deploy de produção só ocorre a partir de `main`, conforme [Deploy](deployment.md).

Pull requests de documentação também devem explicar o que foi atualizado e validar links, comandos e caminhos mencionados.

## Commits

Use mensagens curtas e descritivas, preferencialmente no formato `tipo: descrição`, por exemplo `fix: valida valor de parcela`. Um commit deve representar uma mudança coerente.
