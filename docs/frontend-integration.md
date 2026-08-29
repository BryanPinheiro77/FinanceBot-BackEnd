# Frontend e integração

O frontend não faz parte deste repositório. Esta página registra a fronteira conhecida entre o frontend e a API para evitar que a documentação fique implícita.

## Integração atual

- URL base local da API: `http://localhost:8080`;
- autenticação: JWT;
- documentação interativa: `/swagger-ui/index.html`;
- health check: `/api/health`;
- CORS: configurado por `CORS_ALLOWED_ORIGINS`.

## Quando o frontend for adicionado

O repositório do frontend deve ter seu próprio `AGENTS.md`, README e documentação de setup. Nesse projeto, devemos registrar apenas contratos compartilhados: URL da API, autenticação, endpoints utilizados, variáveis públicas e compatibilidade de versões.

Pendência: identificar o repositório oficial do frontend e documentar seus comandos, stack, ambiente local e processo de deploy sem misturar responsabilidades com este backend.
