# Proteção de Redis, RabbitMQ, logs e backups

Este documento registra o estado atual e o plano de proteção dos dados que ficam fora do PostgreSQL. Ele serve como referência para instalações locais e para o servidor self-hosted de produção.

## Estado atual

| Superfície | Uso atual | Risco identificado |
| --- | --- | --- |
| Redis | O bot guarda contexto de conversa e consultas com TTL configurável. | O compose local publica a porta sem senha; autenticação e TLS não estão documentados para produção. |
| RabbitMQ | A API publica lembretes e o bot consome a fila privada de notificações. | Usuário, senha, vhost, TLS e permissões mínimas não estão documentados como requisitos operacionais. |
| Logs | API e bot usam logs estruturados em formato ECS. | Não há política automatizada de redação e retenção para tokens, conteúdo financeiro e identificadores. |
| Backups | O deploy depende de backup verificável do PostgreSQL antes de operações de risco. | Retenção, criptografia, teste de restauração e proteção contra acesso indevido ainda precisam ser definidos. |

## Modelo de ameaças

Os limites de confiança são: cliente para API, Telegram para bot, API e bot para a rede Docker,
containers para o host e host para o armazenamento de backups. Os ativos são contexto de conversa,
descrições financeiras, identificadores do Telegram, credenciais, mensagens e dumps do banco.

| Ameaça | STRIDE | DREAD médio | Mitigação | Responsável |
| --- | --- | ---: | --- | --- |
| Acesso ao Redis sem credencial ou por porta pública | Spoofing, Information Disclosure, Elevation of Privilege | 8,4 | autenticação/ACL, rede privada, TLS fora do host e TTL | mantenedor de infraestrutura |
| Uso indevido do RabbitMQ ou alteração de mensagens | Spoofing, Tampering, Information Disclosure | 8,0 | usuário e vhost dedicados, permissões mínimas, TLS e payload mínimo | mantenedor de infraestrutura |
| Retenção indefinida de mensagens financeiras | Information Disclosure, Denial of Service | 7,4 | expiração por mensagem e monitoramento da fila | mantenedor do backend |
| Exposição de tokens ou dados financeiros em logs | Repudiation, Information Disclosure | 7,8 | logs estruturados, SQL detalhado desativado e revisão de campos | mantenedor do backend |
| Roubo, alteração ou perda de backups | Tampering, Information Disclosure, Denial of Service | 8,6 | criptografia, checksum, retenção e restauração isolada | operador do servidor |

Todas as ameaças acima de 7 têm mitigação definida nesta issue. A evidência operacional deverá
ser registrada sem copiar credenciais, endereços privados ou dados pessoais para o GitHub.

## Controles planejados

### Redis

- manter Redis acessível somente pela rede interna entre os containers;
- exigir senha ou ACL no ambiente de produção;
- configurar TLS quando a instância estiver fora da rede local confiável;
- manter TTL mínimo para contexto de conversa e consulta;
- não armazenar tokens, senhas ou conteúdo financeiro além do necessário para concluir o fluxo;
- documentar limpeza, expiração e revogação de credenciais.

O ambiente local pode continuar simples para desenvolvimento, mas deve deixar explícito que a senha local não pode ser reutilizada em produção.

### RabbitMQ

- usar usuário de aplicação dedicado, vhost dedicado e permissões mínimas;
- impedir o uso do usuário `guest` em produção;
- manter exchanges e filas de lembretes privadas;
- revisar payloads para transportar somente o identificador e os dados mínimos da entrega;
- habilitar TLS quando o broker atravessar uma rede não confiável;
- definir retenção, mensagens rejeitadas e procedimento de reprocessamento.

### Logs

- nunca registrar tokens, senhas, chaves, conteúdo financeiro completo ou payloads de mídia;
- preferir identificadores técnicos anonimizados ou truncados;
- revisar mensagens de erro e logs de integração antes de habilitar nível `DEBUG`;
- definir retenção e acesso aos logs do host e dos containers;
- adicionar testes ou verificações para impedir regressões de redação.

### Backups

- documentar o backup do PostgreSQL, seu local e sua retenção;
- criptografar o backup em repouso e restringir o acesso ao operador responsável;
- nunca incluir `.env`, chaves ou dumps em artefatos públicos;
- testar restauração periodicamente em ambiente separado;
- registrar a data do último backup verificável antes de migrations, rotação de chaves ou rollback.

## Plano de implementação

1. Inventariar credenciais, portas, redes, TTLs, filas, logs e backups de cada ambiente.
2. Criar credenciais dedicadas e ACL/vhost para produção, sem alterar o ambiente ativo sem janela de mudança e rollback.
3. Fechar exposição de portas e documentar quando TLS é obrigatório.
4. Implementar redação e retenção de logs.
5. Formalizar backup, restauração e evidências operacionais.
6. Validar os controles com testes locais e checklist no servidor self-hosted.

## Variáveis de conexão

As duas aplicações aceitam as mesmas opções de proteção:

| Variável | Uso |
| --- | --- |
| `REDIS_USER` | Usuário ACL do Redis; `default` somente quando esse usuário estiver protegido. |
| `REDIS_PASSWORD` | Senha do Redis. Deve ser obrigatória em produção. |
| `REDIS_SSL_ENABLED` | Ativa TLS para Redis quando o tráfego sair da rede privada. |
| `RABBITMQ_VHOST` | Vhost exclusivo do FinanceBot. |
| `RABBITMQ_SSL_ENABLED` | Ativa TLS para RabbitMQ quando o tráfego sair da rede privada. |
| `FINANCEBOT_SQL_LOGGING_ENABLED` | Habilita SQL detalhado somente para diagnóstico local; o padrão é `false`. |
| `FINANCEBOT_REMINDERS_QUEUE_MESSAGE_TTL_MS` | Expiração das mensagens de lembrete; padrão de 24 horas. |

## Implantação segura

As novas propriedades são compatíveis com o ambiente atual para permitir uma migração em etapas.
Antes de tornar a senha do Redis obrigatória no servidor, configure a credencial no container,
adicione-a aos arquivos `.env` da API e do bot e valide as duas conexões. Para RabbitMQ, crie o
vhost e o usuário dedicado, conceda acesso somente a esse vhost e atualize API e bot na mesma
janela. Mantenha as credenciais anteriores até os health checks e o consumo da fila confirmarem
a migração; depois, revogue-as.

TLS é obrigatório quando Redis ou RabbitMQ forem acessados fora da rede Docker privada. Dentro
da rede privada do host, autenticação forte e ausência de portas públicas continuam obrigatórias.

## Critério de conclusão

A issue será considerada concluída quando cada superfície tiver responsável, configuração documentada para local e produção, evidência de autenticação e isolamento, política de retenção e um procedimento de rollback ou restauração testado.
