#!/bin/bash

set -e

BASE_BRANCH=${1:-main}

mkdir -p .review

echo "Atualizando referência remota..."
git fetch origin

echo "Gerando diff do PR contra origin/$BASE_BRANCH..."
git diff origin/$BASE_BRANCH...HEAD > .review/pr.diff

echo "Gerando status atual do repositório..."
git status --short > .review/status.txt

echo "Gerando lista de commits da branch atual..."
git log --oneline origin/$BASE_BRANCH..HEAD > .review/commits.txt

echo "Gerando prompt final para o Codex..."
cat > .review/codex-review-prompt.md << 'EOF'
Você é um revisor técnico de Pull Requests do projeto FinanceBot.

Leia os arquivos abaixo:

- `.github/ai-review-instructions.md`
- `.github/prompts/pr-review-prompt.md`
- `.review/pr.diff`
- `.review/status.txt`
- `.review/commits.txt`

Quando necessário, consulte os arquivos reais do projeto para entender melhor o contexto.

Não altere código.
Não crie commits.
Não faça merge.
Não aprove oficialmente o PR.
Não diga que executou testes se não executou.
Não invente problemas.
Não sugira mudanças fora do escopo do PR.

Gere um review técnico em Markdown seguindo exatamente o formato definido no prompt.
EOF

echo ""
echo "Arquivos de contexto gerados com sucesso em .review/"
echo ""
echo "Arquivos criados:"
echo "- .review/pr.diff"
echo "- .review/status.txt"
echo "- .review/commits.txt"
echo "- .review/codex-review-prompt.md"
echo ""
echo "Agora abra o Codex no projeto e peça:"
echo ""
echo "Use o arquivo .review/codex-review-prompt.md para revisar este PR."
echo ""