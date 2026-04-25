param(
    [string]$BaseBranch = "main"
)

$ErrorActionPreference = "Stop"

[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$OutputEncoding = [System.Text.Encoding]::UTF8

$Utf8NoBom = [System.Text.UTF8Encoding]::new($false)

function Write-Utf8File {
    param(
        [string]$Path,
        [string[]]$Content
    )

    if ($null -eq $Content) {
        $Content = @()
    }

    [System.IO.File]::WriteAllLines($Path, $Content, $Utf8NoBom)
}

New-Item -ItemType Directory -Force -Path ".review" | Out-Null

Write-Host "Atualizando referencia remota..."
git fetch origin

Write-Host "Gerando diff do PR contra origin/$BaseBranch..."
$DiffContent = git diff "origin/$BaseBranch...HEAD"
Write-Utf8File -Path ".review/pr.diff" -Content $DiffContent

Write-Host "Gerando status atual do repositorio..."
$StatusContent = git status --short
Write-Utf8File -Path ".review/status.txt" -Content $StatusContent

Write-Host "Gerando lista de commits da branch atual..."
$CommitsContent = git log --oneline "origin/$BaseBranch..HEAD"
Write-Utf8File -Path ".review/commits.txt" -Content $CommitsContent

Write-Host "Gerando prompt final para o Codex..."

$Prompt = @'
Voce e um revisor tecnico de Pull Requests do projeto FinanceBot.

Leia os arquivos abaixo:

- `.github/ai-review-instructions.md`
- `.github/prompts/pr-review-prompt.md`
- `.review/pr.diff`
- `.review/status.txt`
- `.review/commits.txt`

Quando necessario, consulte os arquivos reais do projeto para entender melhor o contexto.

Nao altere codigo.
Nao crie commits.
Nao faca merge.
Nao aprove oficialmente o PR.
Nao diga que executou testes se nao executou.
Nao invente problemas.
Nao sugira mudancas fora do escopo do PR.

O review final deve ser escrito em portugues brasileiro.

Gere um review tecnico em Markdown seguindo exatamente o formato definido no prompt.
'@

[System.IO.File]::WriteAllText(
    ".review/codex-review-prompt.md",
    $Prompt,
    $Utf8NoBom
)

Write-Host ""
Write-Host "Arquivos de contexto gerados com sucesso em .review/"
Write-Host ""
Write-Host "Arquivos criados:"
Write-Host "- .review/pr.diff"
Write-Host "- .review/status.txt"
Write-Host "- .review/commits.txt"
Write-Host "- .review/codex-review-prompt.md"
Write-Host ""
Write-Host "Agora abra o Codex no projeto e solicite:"
Write-Host ""
Write-Host "Use o arquivo .review/codex-review-prompt.md para revisar este PR."
Write-Host ""