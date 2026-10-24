# Backup e restauração

Este é o runbook manual de backup PostgreSQL. Agendamento, cópia externa,
monitoramento e teste periódico ainda são acompanhados na #220. Um backup pontual
aprovado não comprova que essa rotina esteja automatizada.

## Preparação

O operador autorizado precisa de Docker, Bash e OpenSSL compatível com PBKDF2,
espaço disponível e diretório privado `0700` fora do checkout. Obter a passphrase
no gerenciador de secrets sem colocá-la em argumentos, histórico ou logs.

Definir na sessão `financebot_postgres_container`, `financebot_database_user`,
`financebot_database_name`, `financebot_backup_dir` e
`FINANCEBOT_BACKUP_PASSPHRASE`, com valores privados da instalação. A variável da
passphrase é usada somente pelo procedimento, não pela aplicação. Desativar
tracing (`set +x`); não compartilhar saídas de ambiente ou conteúdo do banco.

Preservar também, de forma cifrada, o chaveiro ativo/anterior da aplicação e os
metadados de recuperação. Manter a chave do backup separada das cópias cifradas.
As chaves não devem aparecer no manifesto nem em arquivos versionados.

## Criar uma cópia cifrada

Executar em Bash. O pipeline precisa de `pipefail`: sem ele, falha no `pg_dump`
pode ser confundida com sucesso do OpenSSL. Somente promover o arquivo parcial
após sucesso das duas etapas.

```bash
set -euo pipefail
set +x
umask 077
: "${financebot_postgres_container:?Defina o container privado}"
: "${financebot_database_user:?Defina o usuário do banco}"
: "${financebot_database_name:?Defina o banco}"
: "${financebot_backup_dir:?Defina o diretório privado existente}"
: "${FINANCEBOT_BACKUP_PASSPHRASE:?Obtenha a passphrase no gerenciador}"
test -d "$financebot_backup_dir"
export FINANCEBOT_BACKUP_PASSPHRASE
financebot_backup_bundle=$(mktemp -d "$financebot_backup_dir/financebot.XXXXXXXX")
financebot_backup_partial="$financebot_backup_bundle/postgres.dump.enc.partial"
if ! docker exec "$financebot_postgres_container" pg_dump --format=custom \
    --username="$financebot_database_user" "$financebot_database_name" \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -md sha256 \
    -pass env:FINANCEBOT_BACKUP_PASSPHRASE -out "$financebot_backup_partial"; then
  printf '%s\n' 'Backup falhou; o arquivo parcial é inválido e não deve ser restaurado.' >&2
  exit 1
fi
chmod 0600 "$financebot_backup_partial"
mv "$financebot_backup_partial" "$financebot_backup_bundle/postgres.dump.enc"
(cd "$financebot_backup_bundle" && sha256sum postgres.dump.enc > postgres.dump.enc.sha256)
cat > "$financebot_backup_bundle/encryption.json" <<'JSON'
{"format":"pg_dump-custom","cipher":"AES-256-CBC","kdf":"PBKDF2","iterations":200000,"digest":"sha256"}
JSON
unset FINANCEBOT_BACKUP_PASSPHRASE
```

O dump usa o snapshot consistente do `pg_dump`. Se comparar contagens do banco
com a cópia enquanto há escritas, exportar e compartilhar o mesmo snapshot;
contagens de momentos diferentes podem divergir sem erro no backup.

Registrar fora do Git data UTC, versões PostgreSQL/OpenSSL, SHA/tag da aplicação,
versão Flyway e parâmetros usados. Copiar o bundle e o chaveiro cifrado para um
destino fora do servidor; conferir os checksums depois da transferência. Arquivo
no mesmo disco ou volume persistente não substitui cópia externa.

Este formato mantém compatibilidade com o procedimento anterior. CBC não fornece
autenticação criptográfica do conteúdo; checksum detecta corrupção, mas não prova
autenticidade contra quem possa substituir arquivo e checksum. Aceitar somente
origem confiável e protegida. A evolução do formato integra a #220.

## Conferir e testar restauração

Usar os parâmetros do manifesto. Os backups da preparação da v1.11.0 utilizaram
`-pbkdf2 -iter 200000 -md sha256`. Cópias antigas podem ter usado o default de
10000 iterações do OpenSSL: não aplicar um parâmetro novo a um arquivo antigo por
suposição. Confirmar passphrase, versão e parâmetros antes de concluir que um
arquivo está corrompido. Não trocar a chave de dados da aplicação para resolver
um erro de descriptografia do backup.

Preparar um PostgreSQL compatível, isolado, vazio, sem clientes de produção e com
capacidade/recursos dimensionados. Definir `financebot_backup_bundle`,
`financebot_restore_container`, `financebot_restore_user`,
`financebot_restore_database`, `financebot_backup_iterations` e a passphrase na
sessão privada. O container/banco de restore deve ser exclusivo desse teste.

```bash
set -euo pipefail
set +x
: "${financebot_backup_bundle:?Defina a cópia confiável}"
: "${financebot_backup_iterations:?Use o valor registrado no manifesto}"
: "${financebot_restore_container:?Defina o container isolado de teste}"
: "${financebot_restore_user:?Defina o usuário do teste}"
: "${financebot_restore_database:?Defina o banco vazio de teste}"
: "${FINANCEBOT_BACKUP_PASSPHRASE:?Obtenha a passphrase no gerenciador}"
export FINANCEBOT_BACKUP_PASSPHRASE
(cd "$financebot_backup_bundle" && sha256sum --check postgres.dump.enc.sha256)
openssl enc -d -aes-256-cbc -pbkdf2 -iter "$financebot_backup_iterations" -md sha256 \
  -pass env:FINANCEBOT_BACKUP_PASSPHRASE -in "$financebot_backup_bundle/postgres.dump.enc" \
  | docker exec -i "$financebot_restore_container" pg_restore --list > /dev/null
openssl enc -d -aes-256-cbc -pbkdf2 -iter "$financebot_backup_iterations" -md sha256 \
  -pass env:FINANCEBOT_BACKUP_PASSPHRASE -in "$financebot_backup_bundle/postgres.dump.enc" \
  | docker exec -i "$financebot_restore_container" pg_restore --exit-on-error --no-owner \
      --no-privileges --username="$financebot_restore_user" --dbname="$financebot_restore_database"
unset FINANCEBOT_BACKUP_PASSPHRASE
```

Ler o catálogo não comprova restore. Conferir schema/Flyway, contagens contra o
snapshot quando disponível e leitura de dados cifrados com o chaveiro recuperado,
sem expor plaintext. Registrar duração e resultado, incluindo falhas. Remover
somente o ambiente temporário autorizado após preservar a evidência necessária.

## Retenção e recuperação de produção

Proposta inicial: diário 7 cópias, semanal 4 e mensal 3; teste de restauração pelo
menos mensal e antes de migrations/rotação. Definir RPO/RTO e validar se essa
política atende à instalação. Não apagar cópias antes de verificar retenção,
cópia externa, chave de recuperação e autorização.

Restore sobre produção é uma decisão separada. Preservar o estado atual, avaliar
perda de dados depois do snapshot, interromper escritores com autorização e
registrar versão/schema. Validar banco, keyring, aplicação e backlog antes de
liberar tráfego. Seguir o [playbook de incidentes](incident-playbook.md).

Parâmetros de derivação: [OpenSSL enc](https://docs.openssl.org/master/man1/openssl-enc/).
