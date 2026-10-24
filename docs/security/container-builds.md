# Builds e imagens dos serviços Java

A API e o bot têm contextos Docker separados. Cada `.dockerignore` permite somente
`Dockerfile`, wrapper Maven, `pom.xml` e fontes do respectivo módulo. Arquivos de
ambiente, chaves privadas e formatos de backup são excluídos também dos
subdiretórios permitidos. Migrations SQL permanecem incluídas.

O build usa dois estágios com Java 21:

- `build`: JDK e wrapper Maven compilam o módulo; o cache Maven pertence ao builder.
- `runtime`: JRE, usuário `financebot` com UID/GID `10001:10001` e `/app/app.jar`.
  O JAR e o diretório da aplicação são legíveis, mas não graváveis pelo usuário do
  serviço. Arquivos temporários podem ser criados em `/tmp`.

O runtime não precisa do checkout, do JDK, do wrapper ou do cache de compilação.
O Dockerfile pula os testes durante o empacotamento; o CI executa as suítes Maven
completas em jobs separados antes da aprovação do PR.

## Configuração e compatibilidade

O Compose continua lendo o `.env` protegido do operador e fornecendo variáveis
ao processo. Não copiar configurações reais para `src/`, argumentos de build ou
imagens. TLS externo e outros arquivos privados devem ser fornecidos por um
mecanismo de runtime apropriado; não remover exclusões para embuti-los no JAR.

Builds e comandos Compose existentes continuam usando os mesmos contextos e
números de portas, publicados somente em `127.0.0.1`. O proxy HTTPS do host acessa
a API por loopback; o bot usa o DNS interno da rede Docker. Consumidores que
usavam diretamente um IP do host nas portas 8080/8081 devem usar o proxy HTTPS
ou um túnel administrativo autorizado. O comando padrão passa a ser
`java -jar /app/app.jar`. Mounts adicionais
que precisem de escrita devem ter permissões compatíveis com UID/GID 10001;
não contornar isso executando a aplicação como root.

O frontend tem repositório e publicação próprios; não faz parte desses contextos.

## Verificação repetível

Na raiz do repositório, com Python 3, Docker e BuildKit disponíveis:

```bash
python3 scripts/verify-container-images.py --module api
python3 scripts/verify-container-images.py --module telegram-bot
```

O script prepara um contexto temporário apenas com os arquivos do módulo, sem
copiar o `.env` do operador. Injeta configurações, chaves e backups **fictícios**,
e usa o próprio Docker para verificar que o contexto os exclui. Confere também
wrapper, recursos e preservação das migrations.

O teste também renderiza o Compose com ambiente fictício e sem interpolação,
verificando que as portas HTTP e de management ficam publicadas só em loopback.
Requer Docker Compose com `--no-env-resolution` disponível.

Depois constrói a imagem real, executa Java sem rede como usuário sem root,
verifica permissões e inspeciona o filesystem/JAR final. A limpeza remove
somente o container temporário e a tag exclusiva do teste; não executa `prune`
nem altera volumes ou serviços externos. Os dois módulos têm jobs próprios no CI.

O teste verifica a política de empacotamento e não substitui revisão de código,
varredura de dependências ou health checks após um deploy autorizado.

## Publicação e ciclo de vida

Alterar o Dockerfile não modifica imagens já construídas. A aplicação desta
política ao servidor exige reconstrução, publicação e validação autorizadas.
Inventariar imagens, caches e quem tem acesso a eles antes de remover artefatos
ou planejar rotação de credenciais. Não revogar chaves de dados sem considerar
compatibilidade, backups e o [procedimento de rotação](key-rotation.md).

Não registrar detalhes da instalação, caminhos privados ou achados sensíveis em
issues públicas; seguir a [política de segurança](../../SECURITY.md).
