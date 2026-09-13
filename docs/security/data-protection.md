# Proteção de dados

Este documento registra a classificação inicial dos dados do FinanceBot e a estratégia técnica da Issue [#81](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/81). Ele serve como referência de engenharia para instalações self-hosted; não é uma declaração de conformidade legal ou certificação ISO.

## Princípios

- coletar e manter somente os dados necessários para uma finalidade conhecida;
- separar hashing de criptografia: senhas e códigos de uso único não devem ser recuperáveis;
- manter chaves fora do banco, do Git e dos logs;
- proteger dados em repouso, em trânsito, nos backups e nas integrações externas;
- registrar retenção, exclusão e controle de acesso por instalação;
- não enviar dados financeiros a um provedor externo além do necessário para a funcionalidade habilitada.

## Inventário e classificação inicial

| Dado | Onde aparece | Classe | Estratégia inicial |
|---|---|---|---|
| senha | PostgreSQL | Restrito | Hash BCrypt; nunca criptografar de forma reversível |
| `monthlyBaseIncome` | PostgreSQL, análises | Restrito | Primeiro candidato à criptografia de campo |
| valor de transação | PostgreSQL, relatórios | Confidencial | Manter em claro nesta etapa para preservar agregações |
| descrição de transação/recorrência | PostgreSQL, filtros, lembretes | Restrito | Manter em claro nesta etapa; exige índice pesquisável antes de criptografar |
| nome, e-mail e `telegramId` | PostgreSQL, autenticação/vínculo | Confidencial | Não criptografar nesta etapa por causa de unicidade e buscas |
| código de vínculo Telegram | PostgreSQL | Restrito | Avaliar hash com expiração e uso único |
| JWT, OpenAI, Telegram e RabbitMQ credentials | ambiente/secrets | Restrito | Não persistir no banco; usar secret manager ou variáveis protegidas |
| contexto de conversa | Redis | Restrito | TTL mínimo, acesso autenticado e revisão específica de retenção |
| mensagens de lembrete | RabbitMQ | Restrito | TLS, autenticação, filas privadas e payload mínimo |
| logs e backups | infraestrutura | Restrito | Redação de dados, acesso mínimo, retenção e criptografia operacional |

As classes são uma convenção técnica do projeto. Cada operador deve definir responsáveis, prazo de retenção e procedimento de exclusão para sua instalação.

## Ameaças consideradas

- leitura indevida do banco ou de um backup;
- exposição de Redis, RabbitMQ ou logs;
- alteração de valores sem detecção;
- acesso entre usuários por falha de autorização;
- vazamento de secrets em configuração, mensagens ou observabilidade;
- indisponibilidade causada por falha de chave ou migração.

O modelo será ampliado conforme novos campos e integrações forem adicionados. Criptografia não substitui autorização, isolamento de rede, backups protegidos, rotação de secrets ou revisão de logs.

O plano detalhado para Redis, RabbitMQ, logs e backups está em [data-stores-protection.md](data-stores-protection.md).
O procedimento de versionamento, recriptografia e revogação está em [key-rotation.md](key-rotation.md).

## Plano de implementação

1. Introduzir um componente de criptografia de campo usando AES-256-GCM, nonce aleatório por valor e verificação de autenticação.
2. Obter a chave de uma configuração externa; não usar chave padrão nem valor embutido no código.
3. Persistir versão da chave junto ao valor cifrado para permitir rotação futura.
4. Migrar `monthlyBaseIncome` com uma nova coluna e leitura compatível durante a transição; migrations já aplicadas não devem ser editadas.
5. Adicionar testes para round-trip, nonce diferente, alteração do ciphertext, chave ausente e versão de chave inválida.
6. Revisar Redis, RabbitMQ, backups e logs antes de ampliar a proteção para descrições e demais campos pesquisáveis.

## LGPD e normas ISO

O projeto deve aplicar minimização, finalidade, controle de acesso, retenção, exclusão e resposta a incidentes desde o desenho. A responsabilidade legal depende de quem opera a instalação e do contexto do tratamento. Aderência a LGPD, ISO/IEC 27001 ou ISO/IEC 27701 exige também processos, evidências, gestão de riscos e controles organizacionais; este documento não declara certificação.
