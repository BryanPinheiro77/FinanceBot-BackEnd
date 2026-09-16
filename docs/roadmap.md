# Roadmap

O FinanceBot está em desenvolvimento inicial. O roadmap indica direção, não uma promessa de datas. Estado reconciliado em 16/09/2026 (UTC), após a v1.10.0.

## Entregue

- Lembretes assíncronos via RabbitMQ: [v1.8.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.0).
- Observabilidade inicial: [v1.8.1](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.1); deploy e acesso privado: [v1.9.1](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.9.1).
- Criptografia inicial e suporte a PDF, imagem e áudio: [v1.8.2](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.2), issues [#81](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/81) e [#82](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/82). OCR e transcrição são opcionais e dependem de configuração.
- Cobertura geral de testes: [v1.8.3](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.3), issue [#42](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/42).
- Proteção de Redis, RabbitMQ, logs e procedimentos de backup: [v1.8.4](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.8.4), issue [#188](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/188).
- Rotação segura de chaves, com execução opt-in: [v1.9.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.9.0), issue [#187](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/187).
- Alertas financeiros, resumos automáticos e preferências Telegram: [v1.10.0](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/releases/tag/v1.10.0), issue [#119](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/119) e subtarefas #140–#145.

## Em andamento

- [Governança de releases (#123)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/123): reconciliação do histórico e definição do fluxo de atualização contínua.

## Próximos temas

- [Planos, assinatura e limites (#121)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/121): definir o contrato do produto antes de cobrança; preservar a instalação self-hosted.
- [Dashboard web inicial (#122)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/122): neste repositório, limitar o trabalho aos contratos e endpoints da API; o frontend não está aqui.
- [IA como camada de enriquecimento com fallback determinístico (#120)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/120): evolução além da interpretação OpenAI já disponível.

## Frentes abertas de manutenção

- [Evolução gradual da arquitetura (#37)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/37).
- [Governança e contribuição open source (#117)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/117): guias públicos já existem; conferir os critérios e subtarefas restantes antes de fechar.
- [Proteção de campos textuais pesquisáveis (#186)](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues/186): extensão da proteção inicial, com análise de impacto nas buscas.

Há subtarefas abertas que podem sobrepor entregas já publicadas. Antes de iniciar uma, conferir o código e os critérios para evitar duplicar trabalho; não tratá-las como concluídas apenas pelo título.

Atualizar este documento no PR que muda o estado de uma frente e reconciliá-lo antes de cada promoção, conforme [Governança de releases](releases.md). Consulte as [issues abertas](https://github.com/BryanPinheiro77/FinanceBot-BackEnd/issues) para o estado detalhado e o [changelog](../CHANGELOG.md) para o histórico.
