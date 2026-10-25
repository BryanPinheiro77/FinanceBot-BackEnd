# Backup automatizado com Restic

Implementação opt-in da #220. O código e os templates não instalam nem ativam nada
no servidor. A issue permanece aberta até configurar destino externo e alerta,
comprovar execução agendada e validar a aplicação com o chaveiro recuperado.
O [procedimento manual](backup-restore.md) continua válido para cópias antigas.

## O que a rotina faz

1. Exige configuração e senha do repositório em arquivos privados do operador,
   destino remoto SFTP/S3 HTTPS/REST HTTPS e webhook de falha configurado.
2. Usa um lock por instalação; exige pelo menos 5 GiB livres na partição raiz e
   staging em tmpfs com espaço para duas vezes o limite do dump mais 64 MiB.
3. Obtém somente o chaveiro de dados da API em execução. Exporta PostgreSQL em
   formato custom, limitando a 256 MiB por padrão. Falha/saída vazia/excesso de
   tamanho abortam o fluxo antes do upload. Não copia o `.env` inteiro.
4. Recusa mudança do container/chaveiro da API durante a exportação. Não agendar
   durante deploy, migration ou rotação: o dump tem snapshot consistente, mas o
   processo não coordena essas operações concorrentes.
5. Envia dump, chaveiro e manifesto com checksums ao repositório cifrado Restic.
6. Recupera os três arquivos pelo ID completo daquele snapshot, verifica hashes e
   formato do chaveiro. Restaura em container PostgreSQL 17 exclusivo, sem rede,
   portas ou volumes persistentes, com limites de CPU/memória/tmpfs.
7. Exige restore completo, tabelas essenciais acessíveis e ausência de registros
   Flyway malsucedidos. Só então marca o snapshot como verificado e avalia retenção.
8. Remove temporários ao terminar, registra UTC do último sucesso em arquivo
   privado e retorna erro em falhas, tentando também enviar alerta sem dados privados.

O staging é plaintext em tmpfs protegido, não um dump persistido no filesystem.
Isso não exclui a possibilidade de swap/core dumps: revisar swap, core dumps e
permissões do host antes da ativação. A conta operacional com socket Docker tem
poder equivalente a root; restrições systemd não eliminam esse privilégio.
Não executar durante manutenção. SIGKILL, falta de energia ou falha do daemon
podem deixar temporários em memória/container de teste: investigar em privado;
não usar limpeza global. Remover somente artefatos da execução identificada.

## Criptografia e recuperação

O formato novo usa a cifragem autenticada e a derivação de senha do Restic; não
reimplementa AES em scripts nem muda chaves JWT/senhas da aplicação. Todos os
arquivos, incluindo manifesto e chaveiro, entram no repositório cifrado. Conservar
senha/chaves de recuperação separadamente, em gerenciador e cópia independente.
Perder a senha de acesso e todas as cópias de recuperação pode tornar o backup
irrecuperável. Não guardar a senha do Restic dentro do próprio repositório.

Backups antigos OpenSSL não são convertidos nem apagados. Continuam dependendo da
passphrase e dos parâmetros originais. O novo chaveiro não substitui chaves antigas
necessárias a backups históricos. O host comprometido com credenciais de exclusão
pode apagar cópias remotas: avaliar destino com isolamento, versionamento e política
contra exclusão, além de testar a compatibilidade dessa política com prune/locks.

## Retenção e capacidade

`apply_retention: false` é o padrão: a política é simulada e nada é apagado.
Após revisar o plano e autorizar a ativação, `true` permite `forget --prune`, sempre
depois do restore da cópia recém-criada. A seleção é limitada ao host lógico desta
instalação e à tag de snapshots verificados; o repositório deve ser dedicado.
A política preserva a última cópia, 7 diárias, 4 semanais e 3 mensais. As seleções
se sobrepõem; não são necessariamente 14 arquivos. Os períodos são calendários
com backups existentes, não um prazo máximo de 90 dias: cópias antigas podem ficar
por mais tempo quando há lacunas. Snapshots sem verificação não são apagados por
essa política e precisam de investigação; não servem como evidência de recuperação.

Sem espaço local, sem quota remota ou sem conectividade: falhar e alertar, sem
apagar cópias anteriores para tentar abrir espaço. A quota remota depende do
provedor e deve ter monitoramento próprio; o preflight local não a comprova.
`prune` também consome espaço/tráfego/tempo remoto. `restic check` é executado
após prune. Deduplicação/compressão reduzem uso, mas não dão um limite máximo.

O limite inicial de dump e o tmpfs de restore (512 MiB) atendem somente bancos
pequenos; estimar crescimento, memória, tamanho restaurado e duração antes de
ampliar. Dump comprimido não representa o espaço do banco restaurado. Falta de
capacidade no teste impede retenção e mantém cópias anteriores.

## Preparar e ativar

Pré-requisitos: Linux com Python >= 3.11, Restic >= 0.16, Docker, findmnt e systemd;
PostgreSQL de origem compatível com restore em 17. Validar versões no ambiente.
Preparar `postgres:17` previamente, por referência/digest revisado; a rotina usa
`--pull=never`. O template usa tag 17 e precisa acompanhar a política de atualização.
Restic/SSH/S3 precisam ter acesso não interativo, com verificação de identidade TLS
ou host SSH. Não desabilitar verificação de certificados/host keys.

1. Escolher destino **fora do notebook**, com quota, custo, retenção e credenciais
   dedicadas. Um diretório local não é aceito pela configuração operacional.
2. Gerar senha forte do repositório e guardar recuperação separadamente. Criar o
   repositório dedicado com `restic init` no ambiente privado, conferindo destino e
   permissões. O script não inicializa repositórios automaticamente.
3. Instalar cópia revisada de `scripts/backup/financebot_backup.py` em diretório
   operacional fora do checkout. Adaptar `infra/backup/config.example.json` para
   arquivo privado `0600`; a senha também deve ser arquivo `0600/0400` do operador.
4. Criar arquivo privado de credenciais de runtime para o serviço. Configurar as
   credenciais do backend Restic e `FINANCEBOT_BACKUP_FAILURE_WEBHOOK_URL` (webhook
   HTTPS compatível com JSON `content`). Não usar contas/tokens pessoais de bot como
   substituto. Proteger também diretórios, chaves SSH e credenciais do provedor.
5. Executar manualmente `backup`, com retenção desativada, conferir snapshot remoto,
   restore isolado e alerta em falha controlada. Não validar falha apagando cópias.
6. Revisar `retention-plan` no ambiente privado. O script oculta saídas das
   ferramentas para não vazar caminhos: inspecionar a seleção com `restic forget`
   **sempre com `--dry-run`**, os mesmos filtros/política e credenciais privadas.
   Autorizar separadamente exclusão pela política e habilitar `apply_retention`.
7. Instalar/revisar as units em `infra/backup/`. O timer sugere diariamente às
   **03:00 UTC**, com atraso aleatório até 15 min e recuperação de execução perdida.
   Ajustar ao horário de baixo uso e evitar deploy. Ativar apenas depois de autorização
   do operador e primeira recuperação comprovada; isso altera a operação de produção.
8. Conferir `systemctl status/list-timers`, estado `last-success.json` e recebimento
   dos alertas. Monitor externo deve alertar também ausência de execução/sucesso por
   mais de 26 horas: o script não consegue notificar se o servidor estiver desligado.

```bash
python3 scripts/backup/financebot_backup.py --config /caminho/privado/config.json backup
python3 scripts/backup/financebot_backup.py --config /caminho/privado/config.json retention-plan
python3 scripts/backup/financebot_backup.py --config /caminho/privado/config.json restore-check --snapshot <id-completo>
```

Esses comandos exigem ambiente privado preparado; não colar configurações/saídas
brutas em issues. O serviço tem timeout de 45 minutos, limite de memória de 768 MiB
para o processo e execução sem concorrência. O container de teste criado pelo daemon
Docker tem limite separado de 512 MiB. Medir impacto na carga real e ajustar antes
que os limites deixem de atender à instalação.

## Limites da validação

O restore verifica PostgreSQL/schema e formato do chaveiro; ainda não inicia uma
API isolada nem comprova leitura dos campos cifrados pela aplicação. Não compara
contagens entre instantes diferentes como se fossem do mesmo snapshot. RPO sugerido
pela frequência é de até 24h15min **somente com execuções bem-sucedidas**; RTO exige
ensaio completo e medição. Cópia remota verificada não substitui monitor externo,
rotina de recuperação completa ou política aprovada de recuperação de chaves.

Restore em produção segue decisão e autorização próprias no [playbook](incident-playbook.md).

Referências: [formato criptográfico Restic](https://restic.readthedocs.io/en/stable/100_references.html),
[retenção](https://restic.readthedocs.io/en/stable/060_forget.html).
