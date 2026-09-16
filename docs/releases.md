# Governança de releases

## Versão e fontes de verdade

A versão pública do produto é a tag Git `vMAJOR.MINOR.PATCH`, compartilhada pela API e pelo bot. O changelog registra essa mesma versão e a release do GitHub usa a mesma tag. Não crie versões independentes para os módulos.

- `PATCH`: correções compatíveis, documentação ou ajustes operacionais.
- `MINOR`: recursos novos compatíveis com os contratos existentes.
- `MAJOR`: mudanças incompatíveis que exigem migração dos consumidores.

Números previstos em issues são intenção de planejamento. A versão final é definida pelo conjunto efetivamente promovido; atualize a issue se a previsão mudar. Nunca reutilize ou mova uma tag publicada. Tags históricas sem `v` permanecem válidas; use o prefixo nas próximas releases.

Os POMs atualmente usam `0.0.1-SNAPSHOT` como versão técnica dos artefatos Maven. Ela não identifica a versão pública instalada: consulte a tag e o SHA do deploy. Não publique esses snapshots como se fossem artefatos numerados da release. Uma futura publicação de artefatos deverá alinhar os dois POMs à versão do produto antes de criar a tag.

## Atualização durante o desenvolvimento

O responsável pelo PR atualiza os registros na mesma mudança:

1. Adicionar a alteração em `CHANGELOG.md`, em `Unreleased`, com impacto para usuário ou operação. Usar Added, Changed, Fixed, Security ou Tests.
2. Atualizar `docs/roadmap.md` quando uma frente começar, mudar de escopo ou for concluída. Distinguir trabalho entregue de melhorias futuras.
3. Atualizar os documentos dos contratos, configuração e operação afetados.
4. Vincular issues e subtarefas no PR; registrar explicitamente o que ficou pendente.

A revisão verifica esses itens. Não basta atualizar somente as notas no GitHub. PRs de documentação também entram no changelog quando alteram o fluxo do projeto.

## Promoção de develop para main

1. Verificar que os PRs do conjunto estão mesclados em `develop` e que os checks passaram.
2. Comparar `origin/main..origin/develop` e `origin/develop..origin/main`. Se houver commits exclusivos de main, integrá-los em develop por PR antes de promover.
3. Definir a versão pelo impacto do conjunto. No PR de preparação para develop, mover apenas as alterações ainda não publicadas de Unreleased para a seção da versão escolhida e deixar Unreleased disponível para o próximo trabalho. Usar a data UTC planejada, corrigindo-a se a publicação mudar de dia.
4. Reconciliar roadmap, issues, changelog e configuração necessária no servidor. Não versionar valores reais de ambiente.
5. Criar uma branch temporária `promote/<versão>` a partir do SHA atualizado de develop e abrir PR para main usando o [template](../.github/pull_request_template.md). A branch deve conter exatamente o conjunto aprovado.
6. Descrever a versão prevista, migrations, variáveis novas, compatibilidade, riscos e evidências. Usar `Closes #...` apenas para issues cujo aceite foi atendido integralmente.
7. Executar `bash scripts/review-pr-with-codex.sh main` na branch de promoção e realizar a revisão com o prompt gerado. O script prepara contexto; não emite aprovação automática.
8. Aguardar o CI específico do PR e a autorização do mantenedor para merge. Push em main aciona o deploy de produção.

## Publicação e verificação

Depois do merge, o responsável pela release:

- acompanha CI e deploy do SHA promovido; confirma os health checks conforme [Deploy](deployment.md);
- se o deploy falhar, registra o problema e trata a recuperação antes de apresentar a versão como saudável;
- cria a release autorizada usando o SHA exato do merge, não um nome de branch que pode avançar;
- confere versão, tag, changelog, links e fechamento das issues;
- se a data real divergir da seção preparada, corrige o registro por um novo PR;
- leva eventuais ajustes exclusivos de main para develop por PR antes da próxima promoção.

Exemplo de publicação, somente após autorização e verificações:

```bash
gh release create vX.Y.Z --target <sha-validado> \
  --title 'vX.Y.Z - Descrição da entrega' --notes-file <arquivo-de-notas>
```

As releases já publicadas são histórico: corrigir uma informação documental é permitido, mas não apagar ou substituir tags e commits. Não alegar testes, análises ou validações de produção que não foram feitos.

## Padrão das notas

Usar português brasileiro e descrever comportamento e impacto. Incluir somente seções relevantes:

- **Novidades**: recursos que o usuário pode utilizar.
- **Melhorias**: mudanças de experiência, manutenção ou operação.
- **Correções**: problemas concretos resolvidos; testes novos não são correções de comportamento.
- **Segurança e operação**: migrations, configuração, ativação opcional, limitações e ações necessárias.
- **Testes**: comandos/resultados realmente executados e links de CI/deploy; indicar validações não realizadas quando relevantes.
- **Referências**: issues, PRs e comparação com a tag anterior real, inclusive quando ela não tem prefixo `v`.

O changelog é o registro versionado; as notas da release apresentam o mesmo escopo com evidências da publicação. Notas geradas automaticamente podem auxiliar, mas devem ser revisadas e não substituir o changelog.

## Limpeza de branches

Manter habilitada a exclusão automática de branches temporárias após merge. Promover por branch `promote/...` permite excluir a origem do PR sem remover `develop`.

Nunca excluir `main` ou `develop`. Conferir a branch de origem antes de usar `--delete-branch`. Branches antigas só devem ser removidas após confirmar que não contêm trabalho pendente nem PR aberto.
