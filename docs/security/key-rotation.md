# Rotação de chaves de criptografia

Este procedimento implementa a Issue [#187](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/187).
Ele permite trocar a chave de criptografia de campos sem interromper leituras nem manter chaves
antigas indefinidamente.

## Modelo adotado

O envelope novo tem o formato `v2.<key-id>.<payload>`. O prefixo `v2` identifica o formato e o
`key-id` seleciona uma chave no keyring. O ID não é secreto e aceita apenas letras, números,
`_` e `-`. O payload continua protegido por AES-256-GCM com nonce aleatório e tag de autenticação.

Valores `v1.<payload>` criados antes desta mudança continuam legíveis pela chave indicada em
`FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID`. Novas escritas sempre usam
`FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID` e `FINANCEBOT_DATA_ENCRYPTION_KEY`.

| Variável | Função |
|---|---|
| `FINANCEBOT_DATA_ENCRYPTION_KEY` | Chave ativa em Base64, com 32 bytes após decodificação. |
| `FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID` | ID gravado nas novas cifras. Padrão inicial: `primary`. |
| `FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS` | Pares `id=chave-base64`, separados por vírgula, aceitos somente para leitura. |
| `FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID` | ID usado para ler envelopes `v1`; pode ficar vazio após migrar todos eles. |
| `FINANCEBOT_DATA_ENCRYPTION_ROTATION_ENABLED` | Habilita o job; padrão `false`. |
| `FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED` | Deve ser `true` durante a execução do job. |
| `FINANCEBOT_DATA_ENCRYPTION_ROTATION_BATCH_SIZE` | Registros por transação, entre 1 e 1000; padrão 100. |
| `FINANCEBOT_DATA_ENCRYPTION_ROTATION_INTERVAL_MS` | Intervalo entre lotes; padrão 1000 ms. |

Nunca registre ou versione os valores das chaves. O keyring deve permanecer somente no gerenciador
de secrets e no arquivo de ambiente protegido da instalação.

## Ameaças e controles

O fluxo cruza três limites de confiança: operador para arquivo de ambiente, aplicação para keyring
em memória e aplicação para PostgreSQL. Os ativos são as chaves AES, os ciphertexts, a renda base
e os backups. O operador obtém as chaves no gerenciador de secrets; a configuração monta o
keyring; conversores atendem leituras e escritas; o job recriptografa lotes no PostgreSQL.

| Ameaça | STRIDE | DREAD | Controle | Responsável |
|---|---|---:|---|---|
| operador não autorizado altera o keyring | Spoofing/Elevation | 8,2 | SSH por chave, acesso mínimo e arquivo `0600` | operador da instalação |
| troca da chave antes de configurar leitura legada | Tampering/DoS | 8,0 | implantação em fases e teste de leitura antes do job | backend e operador |
| revogação com registros ainda antigos | DoS | 8,4 | consulta de conclusão e revogação somente após resultado zero | operador da instalação |
| execução acidental da recriptografia | Tampering | 7,6 | job desabilitado e confirmação explícita de backup | backend |
| duas instâncias processarem o mesmo registro | Tampering | 7,0 | `FOR UPDATE SKIP LOCKED` e atualização otimista | backend |
| vazamento de plaintext ou chave durante a operação | Information Disclosure | 9,0 | logs somente com contagens; secrets fora de argumentos e do Git | backend e operador |
| operação sem evidência verificável | Repudiation | 7,2 | registro de data, checksum, versão e contagens sem dados pessoais | operador da instalação |
| corrupção silenciosa do ciphertext | Tampering | 8,2 | autenticação AES-GCM e rollback transacional do lote | backend |

## Procedimento seguro

### 1. Preparar e validar o backup

Crie um backup criptografado e teste sua restauração em banco isolado conforme
[backup-restore.md](../operations/backup-restore.md). Registre data, checksum e resultado do teste,
sem registrar dados pessoais ou a passphrase.

Não habilite o job apenas porque o arquivo de backup existe. A confirmação significa que a
restauração foi realmente testada.

### 2. Montar o keyring de transição

Preserve a chave atual como `primary`, gere uma chave nova de 32 bytes e escolha um ID que não seja
reutilizado, por exemplo `key_2026_09`. Configure:

```env
FINANCEBOT_DATA_ENCRYPTION_ACTIVE_KEY_ID=key_2026_09
FINANCEBOT_DATA_ENCRYPTION_KEY=<nova-chave-base64>
FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID=primary
FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS=primary=<chave-anterior-base64>
FINANCEBOT_DATA_ENCRYPTION_ROTATION_ENABLED=false
FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED=false
```

Faça o deploy e valide leituras existentes e uma nova escrita. Nesse estágio, valores antigos são
lidos pela chave `primary`, enquanto novos valores já usam `key_2026_09`.

### 3. Executar a recriptografia

Depois dos health checks e da validação funcional, habilite temporariamente:

```env
FINANCEBOT_DATA_ENCRYPTION_ROTATION_ENABLED=true
FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED=true
FINANCEBOT_DATA_ENCRYPTION_ROTATION_BATCH_SIZE=100
FINANCEBOT_DATA_ENCRYPTION_ROTATION_INTERVAL_MS=1000
```

O job processa cada lote em uma transação, converte valores `v1`, valores ligados a chaves
anteriores e a coluna legada em claro, e registra somente contagens. Chave ausente, autenticação
GCM inválida ou alteração concorrente interrompe e reverte o lote afetado. Enquanto o job está
habilitado, seu estado também participa do health check: uma falha deixa a saúde da aplicação
como `DOWN`, sem expor ciphertext ou chaves.

Confirme a mensagem `Rotação de dados concluída` e valide que não restam valores antigos:

```sql
SELECT COUNT(*)
FROM users
WHERE monthly_base_income IS NOT NULL
   OR (monthly_base_income_encrypted IS NOT NULL
       AND SUBSTRING(monthly_base_income_encrypted, 1, LENGTH('v2.key_2026_09.'))
           <> 'v2.key_2026_09.');
```

O resultado deve ser zero para usuários que possuem renda base. Usuários sem renda podem manter
as duas colunas nulas e devem ser desconsiderados na validação operacional.

### 4. Desabilitar o job e revogar a chave antiga

Primeiro volte `FINANCEBOT_DATA_ENCRYPTION_ROTATION_ENABLED` e
`FINANCEBOT_DATA_ENCRYPTION_ROTATION_BACKUP_CONFIRMED` para `false`. Após nova validação, remova
`primary` de `FINANCEBOT_DATA_ENCRYPTION_PREVIOUS_KEYS` e deixe
`FINANCEBOT_DATA_ENCRYPTION_LEGACY_KEY_ID` vazio. Reinicie a API e confirme os health checks e as
leituras antes de revogar a chave antiga no gerenciador de secrets.

## Rollback

Enquanto a chave anterior permanecer no keyring, restaure a configuração anterior e reinicie a
API. Valores já recriptografados com a chave nova exigirão que ela também seja mantida como chave
anterior durante o rollback. Não restaure um banco antigo sem alinhar o keyring à data do backup.

Se um lote falhar, preserve as duas chaves, desabilite o job, registre apenas o tipo do erro e
investigue antes de repetir. Use a restauração do banco somente quando a transação e o rollback
normal não forem suficientes.
