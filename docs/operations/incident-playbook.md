# Playbook de incidentes

Use este guia quando houver falha ou resultado incerto. A rotina preventiva fica
no [runbook](runbook.md). O mantenedor decide sobre mudanças em produção; responsável,
canal de contato e inventário da instalação devem estar registrados em privado.

## Triagem inicial

1. Registrar início UTC, sintoma, abrangência e último SHA/deploy conhecido.
2. Verificar saúde da API, bot e dependências usando o runbook. Diferenciar falha
   pública do proxy de falha local e de problema restrito a um usuário.
3. Inspecionar logs recentes em sessão privada, pelo intervalo do incidente e
   `correlationId`, sem publicar mensagens, credenciais ou dados financeiros.
4. Priorizar indisponibilidade geral, perda/corrupção de dados ou suspeita de
   vazamento; falhas restritas devem ter impacto e responsável definidos também.
5. Preservar evidências e obter autorização antes de reiniciar, alterar configuração,
   limpar artefatos ou executar rollback/restore. Não repetir escritas de resultado incerto.

## Escolher o procedimento

| Sintoma | Diagnóstico inicial | Encaminhamento |
|---|---|---|
| API local falha | Startup, Flyway, banco, recursos | API e migrations |
| API local funciona, acesso público falha | Proxy, TLS, destino e bind | HTTPS/CORS |
| Login/cadastro `503` ou `429` | Redis, cota e origem do cliente | Autenticação |
| Confirmação sem resposta, `409` ou `503` | Chave/payload e resultado persistido | Transação incerta |
| Bot silencioso ou fila acumulando | Saúde, vínculo, token interno, broker/outbox | Telegram e mensageria |
| Disco/memória esgotando | Crescimento de logs, backlog e consumo | Capacidade |
| Suspeita de segredo exposto | Escopo de acesso e distribuição | Segurança |

## API e migrations

Conferir startup, conectividade do banco e resultado do Flyway, sem expor a URL de
conexão. Identificar a versão aplicada e o SQLState quando disponível. Não editar
migrations aplicadas, alterar manualmente o histórico ou executar `repair` para
esconder uma falha.

Se a aplicação não iniciou depois de uma migration, avaliar compatibilidade antes
de voltar a imagem. Isso não desfaz o schema. Um rollback precisa coordenar API e
bot; uma correção por nova migration pode ser mais adequada. Restore de dados
exige decisão específica, preservação do estado atual e interrupção autorizada dos
escritores. Seguir [backup/restore](backup-restore.md) e [deploy](../deployment.md).

Encerrar após startup/migrations coerentes, saúde recuperada e validação do fluxo
afetado, sem perda de dados pendente.

## HTTPS e CORS

Comparar o health local com o público. Conferir certificado, destino do proxy,
portas em loopback e origem usada pelo frontend. O bot deve usar o DNS interno
Docker, não depender da porta pública do host.

Verificar CORS com preflight da origem esperada. Não liberar `*`, publicar portas
em todas as interfaces ou desligar TLS para contornar a falha. Seguir os contratos
em [integração frontend](../frontend-integration.md) e [rate limiting](../security/auth-rate-limiting.md).
Encerrar quando acesso HTTPS, CORS e proteção das rotas estiverem corretos.

## Autenticação: Redis, `503` e `429`

- `503` no limitador significa que o orçamento não pôde ser verificado. Conferir
  Redis, credenciais, conectividade, timeouts e permissões de EVAL e comandos do
  contador. Não desabilitar o limitador nem limpar o Redis inteiro.
- `429` exige respeitar `Retry-After`. Se usuários diferentes compartilham cota,
  conferir o salto imediato do proxy e a cadeia `X-Forwarded-For`.
- Confiar somente nos proxies controlados; não usar `0.0.0.0/0` ou `::/0`. Não
  habilitar reescrita antecipada do IP com `native`/`framework` nesse contrato.

Não testar o limite por brute force em produção. Encerrar após recuperar a
conectividade/configuração e verificar o comportamento esperado sem consumir
indevidamente o orçamento de usuários.

## Transação incerta ou conflito idempotente

Conservar a mesma chave e o mesmo conteúdo ao repetir uma confirmação cuja
resposta se perdeu. Não gerar outra chave apenas para fazer a solicitação passar.

`409` indica conteúdo diferente com a mesma chave. `503` pode indicar falha no
processamento/recuperação do resultado. Verificar banco, chaveiro e logs técnicos;
não remover o marcador de idempotência para forçar nova criação. Usar consultas
normais para reconciliar o resultado antes de cancelar/recriar uma operação.

No Telegram, a pendência tentada fica congelada. Cancelar não desfaz um lançamento
já gravado. Consultar [idempotência](../transaction-idempotency.md) para retenção e
limites; não restaurar manualmente pendências expiradas para reenvio.
Encerrar quando o resultado estiver identificado, sem duplicação ou ação incerta pendente.

## Telegram e mensageria

Conferir saúde do bot e da API; em `401` interno, comparar presença/coerência dos
tokens sem exibir valores. Verificar acesso à rede Docker e Redis; não substituir
uma pendência persistida por uma chave nova durante uma indisponibilidade.

Para notificações, identificar backlog e estado da outbox sem publicar destinatários
ou conteúdo. RabbitMQ recuperado não prova que a entrega ocorreu. Não fazer purge,
recriar mensagens em massa ou reenviar automaticamente resultados `UNKNOWN`:
Telegram não garante idempotência de envio. Seguir [alertas e entrega](../alerts.md).
Encerrar após consumidores/fluxo recuperados e resultados incertos acompanhados.

## Capacidade

Identificar o crescimento antes de limpar: uso de disco/memória, logs, arquivos,
imagens e filas. Não apagar volumes, backups, imagens de rollback ou executar
`docker system prune` como primeira resposta. Qualquer remoção exige escopo,
retenção e autorização; a saúde do banco e os backups têm prioridade.
Encerrar após recuperar capacidade e tratar a causa ou atribuir a correção de retenção/limites.

## Suspeita de vazamento ou acesso indevido

Preservar evidências e limitar o acesso com autorização. Registrar em privado
quais artefatos/configurações foram acessíveis e quem podia obtê-los; não publicar
secrets nem detalhes exploráveis em issues públicas.

Rotacionar credenciais conforme o escopo confirmado, considerando o impacto nos
serviços. Chaves de dados exigem processo próprio: não revogar a antiga enquanto
leituras, snapshots ou backups ainda dependerem dela. Seguir [SECURITY.md](../../SECURITY.md)
e [rotação](../security/key-rotation.md). Ausência de um arquivo na imagem nova não
comprova remoção de cópias antigas ou ausência de acesso anterior.

## Encerramento

Registrar em privado linha do tempo, impacto, causa confirmada ou hipóteses ainda
abertas, ações/autorização, SHA validado, dados reconciliados e resultado dos checks.
Conferir que jobs/filas e acessos esperados foram recuperados; não encerrar somente
porque um endpoint voltou a responder. Criar tarefas preventivas com evidências
sanitizadas e atualizar o runbook quando o procedimento tiver mudado.
