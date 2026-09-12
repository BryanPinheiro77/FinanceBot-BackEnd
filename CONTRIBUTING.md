# Contribuindo com o FinanceBot

Obrigado pelo interesse em contribuir. O FinanceBot está em desenvolvimento inicial e evolui por pequenas mudanças revisadas em pull requests.

## Antes de começar

1. Leia o [README](README.md), a [arquitetura](docs/architecture.md) e o [guia detalhado de contribuição](docs/contributing.md).
2. Procure uma issue existente antes de iniciar uma implementação.
3. Para dúvidas, use as [discussões do repositório](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/discussions) quando estiverem habilitadas.
4. Nunca publique tokens, senhas, arquivos `.env`, dumps ou dados financeiros reais.

## Ambiente local

É necessário Java 21, Docker e Git. Os comandos completos estão em [Desenvolvimento local](docs/development.md).

```bash
./mvnw clean verify
(cd financebot-telegram-bot && ./mvnw clean verify)
git diff --check
```

## Branches e commits

Crie a branch a partir de `main` usando um prefixo que descreva a mudança:

- `feature/` para funcionalidade;
- `fix/` para correção;
- `docs/` para documentação;
- `refactor/` para reorganização interna;
- `chore/` para manutenção.

Mantenha cada commit coerente e use mensagens curtas no formato `tipo: descrição`, por exemplo `docs: atualizar guia de instalação`.

## Pull requests

O pull request deve:

- explicar o problema e o comportamento resultante;
- referenciar a issue relacionada;
- listar testes executados e seus resultados;
- atualizar documentação e `CHANGELOG.md` quando necessário;
- informar impactos operacionais, de configuração ou de contrato;
- seguir o [template de pull request](.github/pull_request_template.md).

Toda mudança de comportamento deve ter testes. Alterações de banco exigem uma nova migration Flyway; migrations já aplicadas não devem ser editadas.

## Limites do projeto

O frontend não está neste repositório. Alterações de frontend devem respeitar os contratos documentados em [Integração com o frontend](docs/frontend-integration.md). Regras financeiras pertencem à API, e o bot Telegram deve chamar os casos de uso por seus adapters.

Consulte o [Código de conduta](CODE_OF_CONDUCT.md) e a [Política de segurança](SECURITY.md) antes de contribuir.
