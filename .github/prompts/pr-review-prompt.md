# Prompt de Review de Pull Request

Você vai revisar um Pull Request do projeto FinanceBot.

Use como base:

- `.github/ai-review-instructions.md`
- `.review/pr.diff`
- `.review/status.txt`
- `.review/commits.txt`

Quando necessário, leia os arquivos reais do projeto para entender o contexto.

## Objetivo

Gerar um review técnico útil, didático e objetivo para aprendizado e melhoria contínua do projeto.

O review deve ajudar a identificar:

- possíveis bugs;
- problemas de segurança;
- problemas de arquitetura;
- problemas de manutenibilidade;
- problemas de performance;
- necessidade de testes;
- necessidade de atualização do changelog.

## O que analisar

1. Se a alteração faz sentido com o objetivo aparente do PR.
2. Se existe risco de bug.
3. Se existe risco de segurança.
4. Se existe risco de lentidão, consumo excessivo de memória ou consulta ruim.
5. Se a arquitetura atual está sendo respeitada.
6. Se a migração para Clean Architecture está sendo feita corretamente quando o PR tocar nesse tema.
7. Se os testes são suficientes.
8. Se o `CHANGELOG.md` deveria ser atualizado.
9. Se há oportunidade real de melhorar legibilidade ou manutenção.

## Instruções de análise

- Analise primeiro o diff.
- Depois consulte os arquivos reais do projeto apenas quando precisar de mais contexto.
- Não altere arquivos.
- Não faça commits.
- Não faça merge.
- Não aprove oficialmente o PR.
- Não diga que rodou testes se não rodou.
- Não invente problemas.
- Não sugira mudanças fora do escopo.
- Não exagere em otimizações prematuras.
- Seja direto, mas explique o motivo das recomendações.

## Formato obrigatório

## Review técnico do PR

### Resumo das alterações

### Pontos positivos

### Riscos ou problemas encontrados

### Sugestões de melhoria

### Testes recomendados

### Performance

### Changelog

### Veredito técnico