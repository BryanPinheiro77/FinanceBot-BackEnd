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
Logout remove o cookie; não revoga uma cópia do JWT que já tenha sido roubada.
O JWT da API conserva sua expiração própria e a sessão web limita o cookie a no
máximo oito horas, nunca além de `exp`. Não há refresh token nesta etapa.

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
