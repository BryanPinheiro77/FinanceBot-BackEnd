# Runbook de operação

Este guia cobre as verificações de rotina da instalação self-hosted. Para sintomas
inesperados, use o [playbook de incidentes](incident-playbook.md). Os procedimentos
são públicos; inventário, acessos, arquivos de ambiente, chaves e evidências reais
ficam no armazenamento privado do operador.

## Preparar a sessão

Use o alias SSH e o checkout registrados no inventário privado, conforme
[servidor remoto](remote-server.md). Os comandos abaixo são de diagnóstico e devem
ser executados no checkout correto do servidor, sem copiar valores reais para o Git.

```bash
git rev-parse HEAD
docker compose -f compose.prod.yml ps
(cd financebot-telegram-bot && docker compose -f compose.prod.yml ps)
docker compose -f compose.observability.yml -f compose.observability.prod.yml ps
df -h /
free -h
```

O SHA do checkout, sozinho, não prova qual imagem está executando: durante o build,
eles podem divergir. Confira também o SHA do workflow, o resultado do deploy e a
inicialização dos containers. Não use `docker inspect` ou `compose config` completos
como evidência pública: a saída pode conter variáveis de ambiente sensíveis.

## Verificar saúde e acesso

```bash
curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8080/api/health
curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8082/actuator/health
curl --fail --silent --show-error --max-time 5 http://127.0.0.1:8083/actuator/health
```

Esperado: HTTP `200`, containers em execução e sem reinícios recorrentes. Um
container `running` não comprova saúde. Confira ainda:

- API acessível pela origem HTTPS pública, com certificado válido.
- Rota protegida retornando `401` sem credencial; não usar dados reais de login em probes.
- Preflight CORS aceitando somente as origens configuradas do frontend.
- API/bot publicados no host somente em loopback; métricas na rede Docker privada
  e em loopback, conforme [builds e imagens](../security/container-builds.md).
- Observabilidade acessível pelo endereço privado definido pelo operador; não
  assumir `localhost` para portas vinculadas à Tailscale em produção.

No Prometheus, `up{job=~"financebot-.+"}` deve indicar coleta das duas aplicações.
Os testes de HTTPS/CORS não substituem uma validação funcional autenticada; não
criar lançamentos financeiros reais apenas para testar disponibilidade.

## Rotina sugerida

| Frequência | Verificação | Evidência de sucesso |
|---|---|---|
| Diária | Saúde, erros recentes, reinícios, disco e fila/outbox | Sem falha persistente; capacidade suficiente; backlog acompanhado |
| Diária | Último backup e cópia fora do host | Job concluído, arquivo verificável e destino externo confirmado |
| Após mudança | SHA, CI/deploy, migrations, saúde e acesso | Versão esperada e contratos funcionando |
| Periódica | Restauração isolada, patches e permissões | Resultado registrado e pendências atribuídas |

As frequências são uma proposta, não uma declaração de que jobs e alertas já
existem. RPO (quanto dado se aceita perder) e RTO (tempo de recuperação aceitável)
devem ser definidos pelo mantenedor e conferidos com medições.

## Publicação e manutenção

Seguir a [governança de releases](../releases.md) e o [deploy](../deployment.md),
com CI/revisão e autorização de produção. Antes de alterações com risco de perda,
validar backup, chaveiro, restauração isolada e compatibilidade das migrations.
Publicar API antes do bot quando o contrato interno mudar.

Registrar início/fim e resultado da manutenção. Atualização de pacotes, reboot,
reinício de serviços, alteração de firewall, limpeza de artefatos e rollback
exigem autorização operacional; este runbook não os executa automaticamente.

## Dados e recuperação

O [procedimento de backup/restore](backup-restore.md) define formato, parâmetros,
proteção e teste. Preservar o chaveiro da aplicação e a chave de recuperação do
backup: um dump de dados cifrados sem as chaves correspondentes não basta.
Manter segredos separados das cópias cifradas e fora do checkout.

Para rotação, usar [rotação de chaves](../security/key-rotation.md). Não apagar
chaves antigas enquanto houver campos, snapshots idempotentes ou backups que
precisem delas. Não usar `FLUSHALL`, purge de filas ou remoção de volumes como
procedimento de manutenção.

## Registro e encerramento

Guardar em privado data UTC, responsável, SHA/tag, checks executados, resultado,
checksum/identificação do backup e pendências. Evidência pública deve ser sanitizada,
sem dados pessoais, valores financeiros, tokens, endereços ou paths da instalação.

A rotina está concluída quando os checks esperados passaram ou cada falha tem
responsável e próximo passo registrado. Health check verde não encerra perda de
dados, backlog ou incidente de segurança. Continuidade: #219 (isolamento), #220
(backup/restore automático) e #221 (manutenção e privilégios).
