# Skills para agentes

O projeto versiona uma seleção de skills do AIIAHub em `.agents/skills/`. O
objetivo é oferecer o mesmo contexto operacional para colaboradores e agentes
que trabalham no repositório.

## Skills incluídas

### Desenvolvimento e arquitetura

- `senior-backend`: APIs Spring Boot, integrações, persistência e segurança de backend.
- `senior-architect`: decisões de arquitetura e análise de dependências.
- `codebase-onboarding`: entendimento inicial e documentação do código.
- `spec-driven-workflow`: especificações, critérios de aceite e validação.

### API, banco e migrações

- `api-design-reviewer`
- `api-test-suite-builder`
- `sql-database-assistant`
- `database-schema-designer`
- `migration-architect`

### Qualidade e revisão

- `senior-qa`
- `tdd-guide`
- `code-reviewer`
- `pr-review-expert`
- `adversarial-reviewer`
- `ship-gate`

### Operação, segurança e performance

- `senior-devops`
- `ci-cd-pipeline-builder`
- `senior-security`
- `env-secrets-manager`
- `observability-designer`
- `performance-profiler`
- `dependency-auditor`

### IA

- `senior-prompt-engineer`
- `rag-architect`
- `agent-designer`

## Skills intencionalmente fora do projeto

Jira não é utilizado neste repositório. Também ficaram fora as skills de
frontend/UX, automação de navegador, Scrum e MCP, pois não fazem parte do
escopo atual do backend e do bot.

## Segurança e manutenção

As skills foram copiadas do AIIAHub e submetidas a uma análise estática antes
de serem adicionadas. Os scripts são auxiliares e não devem ser executados
automaticamente em CI sem revisão. Nunca forneça tokens, arquivos `.env`,
chaves SSH, dumps ou dados do servidor remoto a uma skill.

Ao atualizar uma skill, revise o diff completo e execute novamente o auditor de
skills antes de abrir um PR.
