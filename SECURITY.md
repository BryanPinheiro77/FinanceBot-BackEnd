# Política de segurança

O FinanceBot está em desenvolvimento inicial. Relate vulnerabilidades de forma privada para reduzir o risco aos usuários e aos operadores de instalações self-hosted.

## Como reportar

Use o [recurso privado de reporte de vulnerabilidade do GitHub](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/security/advisories/new). Não abra uma issue pública para credenciais expostas, bypass de autenticação, acesso a dados financeiros ou execução remota.

Inclua, quando possível:

- descrição e impacto;
- versão, commit ou ambiente afetado;
- passos mínimos para reproduzir;
- evidências sem dados pessoais, tokens ou secrets;
- sugestão de correção, se houver.

Se uma chave ou senha for exposta, revogue-a e faça a rotação imediatamente antes de enviar o relato.

## Escopo de segurança

O reporte pode envolver a API, o bot Telegram, autenticação, integrações RabbitMQ/Redis/OpenAI, imagens Docker, workflows e documentação que revele credenciais ou dados operacionais sensíveis.

## Práticas para operadores

- mantenha secrets fora do Git e dos logs;
- use HTTPS/TLS quando os serviços atravessarem redes não confiáveis;
- restrinja portas de administração e o acesso ao RabbitMQ, Redis, Grafana e Actuator;
- faça backups protegidos e teste sua restauração;
- atualize dependências e imagens de infraestrutura;
- defina retenção adequada para dados financeiros e contextos do Telegram.

Veja a [classificação e estratégia de proteção de dados](docs/security/data-protection.md). Esta política não substitui avaliação jurídica, requisitos de privacidade ou o threat model de cada instalação.
