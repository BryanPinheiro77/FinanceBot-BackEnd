# Contrato web de transações — issue #167

A interface pertence ao repositório FinanceBot-FrontEnd. A #167 usa os endpoints
existentes; não adiciona endpoints, migrations, regras financeiras ou configuração
à API. O usuário proprietário é sempre resolvido pelo JWT autenticado.

## Operações

| Operação | Endpoint | Resposta |
|---|---|---|
| Consultar | `GET /transactions` | Página Spring com `content`, `number`, `size`, `totalElements`, `totalPages`, `first`, `last` |
| Abrir para editar | `GET /transactions/{id}` | `TransactionResponse` |
| Criar lançamento unitário | `POST /transactions` | `201`, objeto com `transaction` e `analysis` |
| Editar lançamento | `PUT /transactions/{id}` | `TransactionResponse` |
| Excluir lançamento | `DELETE /transactions/{id}` | `204`, sem corpo |

O proxy web expõe as mesmas operações em `/api/financebot/transactions`, com IDs
inteiros positivos canônicos, sem permitir caminhos arbitrários ou endpoints de
parcelamento. Cookie HttpOnly e proteção de origem/header continuam no servidor
web; a API recebe seu Bearer existente. Todas as respostas do proxy usam no-store.

O formulário envia `amount`, `description`, `date`, `type`, `sourceType`,
`accountId` e `categoryId`. `amount` continua sendo um **número JSON**, e a API usa
BigDecimal. O cliente preserva seu literal decimal como texto na leitura e na
serialização, evitando conversão intermediária em ponto flutuante. Aceita valores
positivos com até duas casas decimais, sem arredondar frações de centavo. A
formatação em reais não calcula totais, saldos ou regras financeiras.

Criação usa `sourceType=WEB`. Edição preserva a origem retornada pela consulta:
`WEB`, `BOT_TEXT`, `BOT_AUDIO`, `BOT_IMAGE` ou `BOT_PDF`. A categoria precisa ser do
mesmo tipo (`INCOME`/`EXPENSE`) e estar disponível para o usuário; validação final
continua na API. Contas e categorias vêm dos GETs existentes de `/accounts` e
`/categories`; seus CRUDs completos não integram a #167.

## Filtros e paginação

O cliente suporta `startDate` e `endDate` inclusivos (`AAAA-MM-DD`), `type`,
`accountId` e `categoryId`. Sem datas, consulta todos os períodos, sempre com
paginação. A listagem usa `page` iniciando em zero, `size` 10, 20 ou 50 e duas
ordenações: `sort=date,desc&sort=id,desc`. A API continua responsável pelo filtro,
ordenamento e contagem; não se somam páginas no navegador.

Filtros e página ficam na URL da interface para recarga/voltar, sem valores
monetários, descrição ou tokens na URL. Ao aplicar filtros ou trocar o tamanho, a
página volta a zero. Se uma exclusão ou alteração concorrente remover a última
página, a interface consulta a última página válida. Contagens vazias representam
somente uma consulta bem-sucedida; falhas recebem estado próprio.

## Escritas e resultados incertos

Criação gera um UUID em `Idempotency-Key` por tentativa lógica. Enquanto o pedido
está em andamento, os controles ficam indisponíveis. Em falha de rede/5xx ou
resposta não confirmada, os mesmos dados e chave permanecem em memória, com o
formulário congelado. Não se dispara uma nova criação automaticamente.

O reenvio explícito com mesma chave/corpo é limitado a 45 segundos a partir da
primeira tentativa, abaixo da menor retenção configurável da API (60 segundos).
Após esse prazo, conflito 409 ou saída/recarregamento da página, conferir os
lançamentos antes de cadastrar novamente. Não há persistência da chave/dados em
Web Storage nem garantia de retomada após fechar a aba. Respostas 400/403/404
permitem corrigir dados; 401 aciona o fluxo existente de expiração de sessão.
[Contrato completo de idempotência](transaction-idempotency.md).

PUT/DELETE não recebem garantia de replay de criação. Uma resposta incerta exige
conferir a lista antes de repetir a ação. A interface não implementa bloqueio
otimista/ETag: a última escrita aceita pela API prevalece. A consulta individual
é refeita ao abrir a edição, mas não elimina concorrência depois dessa consulta.

Para registros parcelados, PUT e DELETE afetam **apenas o lançamento selecionado**;
não editam/excluem o grupo nem recalculam as demais parcelas. A interface explica
essa semântica antes de salvar/excluir. Criar parcelamentos fica fora desta issue.

## Atualização e isolamento

Após escrita confirmada, a lista é consultada novamente usando os filtros atuais;
um lançamento pode deixar de aparecer se a alteração o tirar do período/tipo.
O dashboard busca novamente seu resumo ao navegar até ele. Não se altera seu
resultado localmente a partir dos valores do formulário.

Trocar filtros, página ou identidade cancela/ignora consultas antigas e remove os
resultados anteriores da tela enquanto a próxima consulta carrega. O backend
mantém o isolamento real; o cliente não trata IDs ou filtros como autorização.
Os testes web usam API fictícia, com identidades isoladas, sem credenciais/dados de
produção. Evidências e limitações ficam em `FinanceBot-FrontEnd/docs/transactions.md`.
