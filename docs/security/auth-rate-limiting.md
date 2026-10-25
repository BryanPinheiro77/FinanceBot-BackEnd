# Limites de tentativas de autenticação

A issue [#132](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/132)
adiciona limites independentes para `POST /auth/login` e `POST /auth/register`.
O orçamento é por IP e operação, compartilhado entre réplicas da API pelo Redis.

## Comportamento

- Login: até 10 requisições em 60 segundos por padrão.
- Cadastro: até 5 requisições em 3600 segundos por padrão.
- A primeira tentativa inicia a janela. Sucessos, erros de credenciais e payloads
  inválidos consomem a mesma cota; o corpo não é lido pelo limitador.
- Ao exceder a cota, a API retorna `429`, `Content-Type: application/problem+json`,
  `Cache-Control: no-store` e `Retry-After` com os segundos restantes, arredondados
  para cima. Rejeições não renovam a janela e não chegam ao controller.
- `OPTIONS` e as demais rotas não consomem essa cota.
- Em falha do contador Redis, a autenticação retorna `503` com `Retry-After: 5`.
  Não há liberação automática nem contador local alternativo durante a falha.

Exemplo de resposta limitada:

```json
{
  "type": "about:blank",
  "title": "Too many requests",
  "status": 429,
  "detail": "Limite de tentativas de autenticação excedido. Aguarde antes de tentar novamente.",
  "instance": "/auth/login"
}
```

O frontend pode ler `Retry-After` nas origens permitidas pelo CORS e deve aguardar
antes de repetir. A implementação do frontend pertence ao repositório próprio.

## Configuração

As variáveis são opcionais, com os padrões abaixo. Não há novos secrets ou migrations.

| Variável | Padrão | Uso |
|---|---|---|
| `FINANCEBOT_AUTH_RATE_LIMIT_ENABLED` | `true` | Habilita o limitador |
| `FINANCEBOT_AUTH_LOGIN_MAX_REQUESTS` | `10` | Cota de login |
| `FINANCEBOT_AUTH_LOGIN_WINDOW_SECONDS` | `60` | Janela de login |
| `FINANCEBOT_AUTH_REGISTER_MAX_REQUESTS` | `5` | Cota de cadastro |
| `FINANCEBOT_AUTH_REGISTER_WINDOW_SECONDS` | `3600` | Janela de cadastro |
| `FINANCEBOT_AUTH_TRUSTED_PROXIES` | vazio | IPs/CIDRs confiáveis, separados por vírgula |
| `REDIS_CONNECT_TIMEOUT` | `2s` | Prazo para conectar ao Redis na API |
| `REDIS_TIMEOUT` | `2s` | Prazo dos comandos Redis na API |

As cotas aceitam 1 a 100000 requisições e as janelas, 1 a 86400 segundos.
Valores fora desses intervalos impedem a inicialização. As réplicas devem usar a
mesma configuração. Ao mudar limites, contadores existentes conservam o TTL
anterior até expirar. Reiniciar somente a API não limpa o orçamento; perder ou
remover os dados Redis reinicia as janelas.

## IP do cliente e proxies

Por padrão, o limitador usa o IP da conexão TCP. `X-Forwarded-For` de uma conexão
não confiável e o header `Forwarded` são ignorados. Não confie no primeiro IP
informado livremente pelo cliente.

Se houver proxy reverso, confirme o IP/sub-rede da conexão imediata e configure
somente os proxies controlados pelo operador em `FINANCEBOT_AUTH_TRUSTED_PROXIES`.
O proxy deve remover headers de encaminhamento do cliente ou acrescentar o IP real
da conexão à direita de `X-Forwarded-For`. O backend percorre a cadeia da direita
para a esquerda e para no primeiro salto não confiável. Múltiplas linhas são
combinadas na ordem recebida. Cadeias acima de 2048 caracteres ou 20 entradas,
ou um endereço inválido necessário à resolução, usam a cota do IP da conexão.
Prefixos anteriores ao primeiro salto não confiável não influenciam o resultado.

O projeto define `server.forward-headers-strategy=none`. O limitador habilitado
recusa `native` e `framework` na inicialização, pois a reescrita antecipada do IP
ocultaria a conexão original. Não adicione filtros ou valves que reescrevam esse
IP antes do limitador sem rever esse contrato. Veja a [documentação do Spring Boot](https://docs.spring.io/spring-boot/how-to/webserver.html#howto.webserver.use-behind-a-proxy-server).

Não configure confiança em redes amplas de clientes, nem em `0.0.0.0/0` ou `::/0`;
esses dois CIDRs são recusados. IPs privados, por si só, não provam que a conexão
veio de um proxy. Sem proxy confiável configurado, usuários atrás de um mesmo proxy
compartilham a cota desse proxy. A topologia real deve ser confirmada pelo operador
antes da promoção; este documento não pressupõe um proxy específico em produção.

## Redis e limites desta proteção

O script Lua verifica a cota, incrementa e estabelece expiração atomicamente,
conforme o [padrão de limitador documentado pelo Redis](https://redis.io/docs/latest/commands/incr/).
As chaves usam o prefixo `financebot:auth-rate-limit:v1:`, a operação e um hash
SHA-256 do IP, sem e-mail, senha, corpo ou IP em claro. O hash é pseudonimização,
não criptografia; endereços previsíveis podem ser inferidos. As chaves expiram ao
fim da janela, e requisições bloqueadas não incrementam indefinidamente o contador.

Usuários em NAT compartilham orçamento. Um atacante com vários IPs pode distribuir
tentativas; esta medida não substitui proteção de borda, MFA ou detecção de abuso.
Não há bloqueio por e-mail, evitando permitir que terceiros bloqueiem uma conta
inteira apenas por conhecer seu endereço.

## Testes

`AuthRateLimitIntegrationTest` usa Testcontainers e Redis 7 temporário, além do
contexto real da API, Spring Security e H2. Docker deve estar ativo. Há testes de
concorrência, expiração, contadores entre instâncias, isolamento por operação/IP,
headers falsos, proxy confiável, CORS, credenciais corretas/incorretas e reparo de
TTL ausente. Testes unitários cobrem resolução IPv4/IPv6, configuração inválida,
modo desabilitado e resposta `503` sem expor detalhes da conexão.

```bash
./mvnw --batch-mode --no-transfer-progress clean verify
git diff --check
```

As outras suítes usam o perfil de teste com o limitador desabilitado; a suíte
específica o habilita explicitamente. Nenhum teste usa o Redis de produção.

Validação local em 24/10/2026: `clean verify` com ordem alfabética aprovado,
com 488 testes da API, incluindo 32 testes desta mudança, sem falhas, erros ou
skips. `git diff --check` também aprovado. A execução local com Colima utilizou
as variáveis de socket descritas em [Desenvolvimento local](../development.md).

## Proxy do frontend web

A declaração HMAC opcional do servidor web preserva a origem da cota sem ampliar
os CIDRs confiáveis. Configuração, limites e ordem de publicação estão em
[web-session.md](web-session.md). Sem assinatura válida, permanece a resolução
normal do peer/chain; isso pode agregar a cota do proxy, mas não libera tentativas
sem contagem.
