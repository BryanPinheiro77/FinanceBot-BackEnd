# Sessão web e origem autenticada do cliente

A API continua aceitando JWT exclusivamente pelo header `Authorization: Bearer`.
Não autentica por cookie e não entrega sua chave JWT ao frontend. O bot e os demais
clientes REST mantêm seus contratos. A mudança web é implementada no repositório
[FinanceBot-FrontEnd](https://github.com/BryanPinheiro77/FinanceBot-FrontEnd), usando
sua camada de servidor na mesma origem do navegador.

## Fronteira da sessão

O servidor web recebe o JWT da API e configura cookie host-only `HttpOnly`,
`Secure` em produção, `SameSite=Lax` e `Path=/`. O navegador não recebe o JWT no
JSON do login/cadastro nem o mantém em localStorage/sessionStorage. As chamadas
web passam pelo servidor do frontend, que anexa o Bearer ao falar com a API.
O backend não precisa aceitar cookies entre sites nem liberar CORS para esse tráfego.

A proteção CSRF fica na fronteira que recebe cookies: origem exata configurada,
header próprio exigido para mutações e rejeição de Fetch Metadata de outro site/origem.
Não desabilitar essas verificações para contornar problemas de configuração.
Logout exige confirmação da revogação na API antes de limpar os cookies. Todos os
access tokens e refresh tokens da mesma sessão deixam de funcionar, inclusive cópias
roubadas. Outras sessões do usuário continuam válidas. Se a API estiver indisponível,
o frontend informa a falha e preserva a credencial para tentar a revogação novamente.

## Access token, refresh e ociosidade

- Senhas continuam com hash BCrypt. JWT continua assinado com HMAC; não há chave
  JWT no frontend. Assinatura assimétrica separaria emissores de validadores, mas
  não implementaria revogação por si só.
- Access token: no máximo **15 minutos**. `JWT_EXPIRATION` é em milissegundos, com
  padrão `900000` e teto de `900000`, mesmo se o ambiente ainda configurar 24 horas.
- Cada JWT de login/cadastro/refresh contém `sid`, um identificador aleatório da
  sessão. A API verifica usuário, validade e sessão ativa em cada chamada protegida.
  JWTs antigos sem `sid` são rejeitados: será necessário entrar novamente.
- Refresh token: segredo opaco de 256 bits, persistido somente como SHA-256 em
  `auth_refresh_tokens`. Rotação em toda renovação; o histórico de tokens consumidos
  permite detectar replay. Reutilização revoga a sessão inteira e retorna `401`.
  Renovação, atividade e logout usam lock no banco para serializar alterações.
- Sessão: máximo absoluto de **8 horas**, sem extensão pelo refresh. Após **1 hora
  sem interação**, o navegador mostra “Você será desconectado” por **60 segundos**,
  com “Continuar conectado” e “Sair agora”. Ao final, solicita logout. A própria API
  rejeita a sessão após 1 hora + 60 segundos sem atividade, mesmo sem JavaScript.
- “Continuar conectado” confirma atividade no servidor. Foco da janela, consultas,
  refresh e timers não atualizam o prazo de ociosidade. A confirmação não estende o
  limite absoluto de 8 horas; nesse limite é necessário entrar novamente.
- O browser solicita refresh de 15 em 15 minutos e quando uma chamada protegida
  retorna `401`; só repete a chamada original após renovação bem sucedida. O proxy
  não consome outro refresh se já recebeu um access com mais de 30 segundos de vida.
  Web Locks serializa renovações entre abas da mesma origem; sem suporte a Web Locks,
  apenas chamadas na mesma aba são agrupadas e uma corrida pode exigir novo login.
- Cookies separados: `__Host-financebot.session` (access, nunca além de `exp`) e
  `__Host-financebot.refresh` (até o fim absoluto da sessão), ambos host-only,
  `HttpOnly`, `Secure` e `SameSite=Lax`. Desenvolvimento loopback usa os nomes sem
  `__Host-` e sem `Secure`. Nenhum dos dois aparece no JSON entregue ao browser.
- Eventos entre abas carregam apenas prazos de atividade ou aviso de logout, sem
  credenciais. Reload e retorno de suspensão consultam o prazo do servidor. GET/SSR
  não rotaciona refresh: quando necessário, o guard envia a uma página de renovação
  que faz POST protegido por CSRF no navegador.
- Migration nova: `V18__create_auth_sessions.sql`. Limpeza diária remove sessões
  além das 8 horas e seu histórico de refresh por `ON DELETE CASCADE`. Não editar
  migrations anteriores. A indisponibilidade do banco não libera acesso autenticado.

### Contrato REST (servidor web e demais clientes)

| Endpoint | Entrada | Saída / efeito |
|---|---|---|
| `POST /auth/login`, `POST /auth/register` | Credenciais existentes | Campos anteriores + `refreshToken`, `sessionExpiresAt`, `lastActivityAt` |
| `POST /auth/refresh` | JSON `{ "refreshToken": "..." }` | `token`, novo `refreshToken`, `sessionExpiresAt`, `lastActivityAt`; não altera atividade |
| `POST /auth/logout` | JSON `{ "refreshToken": "..." }` | `204`, revoga a sessão; refresh consumido também permite logout; token desconhecido válido é idempotente |
| `GET /auth/session` | Bearer válido | `lastActivityAt`, `idleWarningAt`, `idleExpiresAt`, `sessionExpiresAt` |
| `POST /auth/activity` | Bearer válido | Confirma interação e devolve os mesmos prazos; sessão encerrada retorna `401` |

O proxy web obtém o refresh exclusivamente de seu cookie, nunca do JSON ou do
Authorization enviados pelo JavaScript. Renovação e logout estão sujeitos às mesmas
verificações CSRF das demais mutações. Respostas de autenticação usam `no-store`.
O contrato de token em JSON da API é destinado a clientes REST/servidor web; o proxy
substitui esse corpo por `{ "authenticated": true }` antes de responder ao navegador.

### Publicação coordenada

Preparar API e frontend juntos. A API exige `sid` e aplica expiração curta, portanto
clientes legados precisarão entrar novamente e atualizar seu fluxo de refresh.
Aplicar V18 pela inicialização normal do Flyway, publicar a API e então o frontend
compatível em janela autorizada. Não é necessário trocar `JWT_SECRET` nem o hash das
senhas. Configurar `JWT_EXPIRATION=900000` para explicitar o limite. Nenhum deploy,
commit ou push faz parte da preparação local desta mudança.

## Preservação da cota de login/cadastro

Sem uma origem autenticada, o proxy web faria usuários diferentes compartilharem
a cota do IP de saída da hospedagem. `FINANCEBOT_WEB_PROXY_SECRET` permite que o
servidor web assine uma declaração curta sobre o IP do cliente, sem transformá-lo
em proxy de rede confiável para todos os usos.

- Chave: Base64 canônico de **32 bytes aleatórios**, exclusiva desta integração;
  não reutilizar `JWT_SECRET`, chave de dados, senha do backup ou token Telegram.
- Headers únicos: `X-FinanceBot-Client-IP`, `X-FinanceBot-Proxy-Timestamp`
  (Unix em segundos) e `X-FinanceBot-Proxy-Signature` (HMAC-SHA256 hexadecimal minúsculo).
- Mensagem UTF-8, sem newline final:
  `timestamp + "\n" + ip + "\n" + método HTTP + "\n" + caminho da API`.
- A API exige IP literal, assinatura válida em comparação constante e timestamp
  no intervalo de até 60 segundos para passado/futuro. Mantém normalização do IP
  para não criar cotas distintas por representações IPv6 equivalentes.
- Declaração ausente, inválida, repetida ou vencida segue a resolução normal do
  peer/proxies específicos já configurados; não remove a cota nem autentica usuário.
- Sem chave configurada, o recurso permanece desativado. Uma chave configurada
  inválida impede a inicialização, em vez de aceitar tráfego com proteção incompleta.

A assinatura só influencia o identificador da cota de login/cadastro. Ela não
substitui JWT, autorização por usuário ou token interno do Telegram. Repetição de
uma declaração válida na janela não muda o IP/cota assinados; não é uma credencial
de sessão. Relógios dos dois servidores precisam estar sincronizados.

O frontend hospedado na Vercel usa o IP fornecido pela plataforma, não um
`X-Forwarded-For` arbitrário do navegador. Não colocar outro proxy à frente sem
reavaliar essa cadeia. Fora da Vercel, definir uma integração de origem confiável
antes de habilitar o proxy web em produção; não confiar em headers públicos por conveniência.

## Ordem de publicação

1. Preparar/revisar os dois PRs e os testes. Esta documentação não ativa o fluxo.
2. Gerar a chave no ambiente privado e configurar a mesma variável **somente no
   servidor da API e no runtime do servidor web**, sem prefixo `VITE_` e sem expor
   valores em GitHub, logs ou chat. Configurar também origem web e URL HTTPS da API.
3. Promover a API com suporte à declaração e configuração primeiro. Verificar
   saúde e preservar a configuração anterior para decisão de recuperação autorizada.
4. Publicar o frontend somente depois de validar o caminho autenticado. Não fazer
   merge automático em uma branch que publique produção sem autorização de rollout.
5. Validar login/cadastro, recarga, logout, isolamento entre usuários, CSRF,
   `Retry-After` e cotas independentes por IP, sem forjar tráfego de usuários reais.
6. Remover configurações legadas somente após confirmar a migração. Tokens antigos
   do Web Storage são descartados; usuários precisam entrar novamente.

Preview deve usar fixtures/ambiente isolado e credenciais próprias. Não fornecer
chaves de produção a branches/código não confiáveis. A mudança não exige migration,
rotação do segredo JWT ou alteração de hashes de senha. O fallback para peer evita
confiança indevida, mas assinatura ausente/incorreta pode agregar usuários na mesma
cota; essa condição precisa ser identificada na validação da publicação.

Referências: [headers da Vercel](https://vercel.com/docs/headers/request-headers),
[CSRF OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).
