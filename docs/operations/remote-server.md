# Servidor remoto

## Acesso

Os detalhes de acesso ficam somente no gerenciador de senhas e na configuração local do administrador. Não registrar neste repositório o hostname/IP Tailscale, usuário SSH, caminho do checkout ou comandos com dados reais do servidor.

No Mac, use um alias local no `~/.ssh/config`, com valores mantidos fora do projeto. Exemplo genérico:

```sshconfig
Host financebot-server
  HostName <tailscale-hostname-ou-ip-privado>
  User <usuario-do-servidor>
  IdentityFile ~/.ssh/<chave-privada>
```

## Diagnóstico inicial

```bash
tailscale status
ssh financebot-server
docker ps
docker compose version
```

Se o SSH falhar, confira o Tailscale no Mac e no notebook e depois o serviço SSH no notebook. Não exponha a porta SSH publicamente quando a rede Tailscale for suficiente.

## Checklist do notebook

- [ ] Tailscale inicia automaticamente e aparece conectado.
- [ ] O usuário do runner tem acesso ao Docker sem `sudo` interativo.
- [ ] O runner GitHub Actions está online.
- [ ] O diretório privado do checkout aponta para o repositório correto.
- [ ] Os dois `.env` existem e têm permissões restritas.
- [ ] `backend-network` existe.
- [ ] Há backup verificável do PostgreSQL.
- [ ] Logs e espaço em disco são monitorados.

## Inspeção segura

Use `docker compose -f compose.prod.yml ps` para ver o estado dos serviços e `docker logs --tail=100 <container>` para consultar logs recentes.

Nunca cole `.env`, tokens, IPs privados, nomes de host ou logs com dados sensíveis em issues, PRs ou documentação pública.
