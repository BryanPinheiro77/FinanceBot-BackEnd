# Backup e restauração

Este procedimento protege backups do PostgreSQL em instalações self-hosted. Os comandos devem
ser adaptados ao nome real do container e executados somente por um operador autorizado.

## Requisitos

- diretório de backup fora do checkout do Git, com permissão `0700`;
- passphrase exclusiva em um gerenciador de secrets;
- `pg_dump`, `pg_restore` e OpenSSL disponíveis no host;
- espaço livre suficiente e política de retenção definida.

Nunca grave a passphrase, o dump aberto ou valores do `.env` no repositório ou nos logs.

## Criar backup criptografado

Exporte `FINANCEBOT_BACKUP_PASSPHRASE` apenas na sessão segura do operador. Gere o dump em formato
custom e envie a saída diretamente para criptografia, sem criar uma cópia aberta em disco:

```bash
docker exec <postgres-container> pg_dump --format=custom --username=<db-user> <db-name> \
  | openssl enc -aes-256-cbc -salt -pbkdf2 -pass env:FINANCEBOT_BACKUP_PASSPHRASE \
  -out <diretorio-privado>/financebot-AAAA-MM-DD.dump.enc
```

Depois, restrinja o arquivo para `0600`, registre apenas data, tamanho e checksum e remova backups
além da retenção aprovada. Não registre nomes de usuários, senhas ou conteúdo do dump.

## Verificar integridade

Uma criação de arquivo não comprova que o backup pode ser restaurado. Descriptografe diretamente
para `pg_restore --list` e confirme que o catálogo pode ser lido:

```bash
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:FINANCEBOT_BACKUP_PASSPHRASE \
  -in <arquivo.dump.enc> \
  | pg_restore --list
```

## Testar restauração

Use um PostgreSQL isolado, vazio e sem acesso de clientes. Nunca restaure sobre produção para
testar o backup.

```bash
createdb <database-temporario>
openssl enc -d -aes-256-cbc -pbkdf2 -pass env:FINANCEBOT_BACKUP_PASSPHRASE \
  -in <arquivo.dump.enc> \
  | pg_restore --exit-on-error --no-owner --dbname=<database-temporario>
```

Após validar migrations, tabelas e uma amostra sem expor dados pessoais, apague o banco temporário
de acordo com a política do ambiente. Registre a data, o resultado, o responsável e a versão da
aplicação usada no teste.

## Retenção sugerida

- diário: 7 cópias;
- semanal: 4 cópias;
- mensal: 3 cópias;
- teste de restauração: pelo menos mensal e antes de migrations ou rotação de chaves.

A retenção final deve considerar necessidade operacional e os princípios de necessidade e
eliminação da LGPD.

## Recuperação

Antes de restaurar, confirme o incidente, preserve o banco atual, valide o checksum, registre a
versão do schema e interrompa escritores. Após a restauração, execute health checks, valide o
Flyway e só então libere tráfego. O rollback deve apontar para a cópia preservada antes da operação.
