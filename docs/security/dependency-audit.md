# Verificação de dependências da candidata v1.11.0

Em 24/10/2026, foram resolvidas as dependências de runtime dos dois POMs usando
`dependency:list -DincludeScope=runtime`. A consulta enviou somente nomes públicos
e versões de pacotes Maven ao [OSV querybatch](https://google.github.io/osv.dev/post-v1-querybatch/).
Nenhum arquivo de ambiente, credencial, fonte privada ou dado financeiro foi enviado.

A revisão inicial identificou avisos em dependências gerenciadas pelas versões
anteriores. Os módulos foram alinhados ao **Spring Boot 4.0.9**. O cliente
`com.rabbitmq:amqp-client` é fixado em **5.37.0** pela propriedade
`rabbit-amqp-client.version`, pois a versão gerenciada no BOM ainda tinha avisos.
Não retirar esse override sem verificar o BOM substituto e repetir a consulta.

Resultado após as mudanças: **171 combinações pacote/versão distintas de runtime,
zero avisos retornados**, sem paginação pendente. Isso representa somente a base
consultada nesse momento; não prova ausência de vulnerabilidades desconhecidas,
não inclui pacotes do sistema/imagens e não substitui revisão de aplicabilidade.

Validação: 550 testes da API, 200 do bot e duas verificações Docker aprovados.
O teste com RabbitMQ real cobre publisher confirms, nack com reentrega e ack.

Para repetir a resolução:

```bash
./mvnw --batch-mode --no-transfer-progress dependency:list -DincludeScope=runtime
(cd financebot-telegram-bot && ./mvnw --batch-mode --no-transfer-progress dependency:list -DincludeScope=runtime)
```

Consultar os pares exatos retornados em `POST https://api.osv.dev/v1/querybatch`,
com `package.ecosystem=Maven`, `package.name=groupId:artifactId` e `version`.
Conferir paginação, registros afetados, aplicabilidade e versões corrigidas nas
fontes primárias antes de concluir o resultado. Não publicar evidências sensíveis
sobre uma instalação real; seguir [SECURITY.md](../../SECURITY.md).
