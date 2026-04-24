# FinanceBot - Instruções permanentes para review com IA

## Contexto do projeto

FinanceBot é uma aplicação Java 21 com Spring Boot para controle financeiro pessoal, integração com Telegram e API REST.

O projeto utiliza:

- Java 21
- Spring Boot
- PostgreSQL
- H2 para testes
- Docker
- JaCoCo para cobertura de testes
- SonarQube para análise de qualidade
- GitHub Actions para CI/CD

O projeto atualmente possui arquitetura em camadas e será migrado gradualmente para uma arquitetura baseada em Clean Architecture.

## Estado atual da arquitetura

A arquitetura atual ainda pode conter:

- Controllers chamando services
- Services concentrando regras de negócio
- Repositories Spring Data acessados diretamente por services
- DTOs usados na camada de entrada e saída
- Entities JPA representando persistência

Durante a migração, nem todo PR precisa estar 100% em Clean Architecture.

O objetivo do review é apontar violações reais, riscos e oportunidades de melhoria, sem exigir uma refatoração completa em todo PR.

## Arquitetura alvo

A arquitetura alvo será baseada em Clean Architecture, separando:

- `domain`: modelos, regras de negócio puras, value objects e exceptions de domínio
- `application`: casos de uso, ports de entrada e ports de saída
- `infrastructure`: persistência, JPA, banco de dados, configurações, segurança e integrações externas
- `interfaces`: REST controllers, Telegram bot, DTOs, presenters/adapters de entrada

## Regra de dependência

A regra principal é:

- `domain` não deve depender de Spring, JPA, banco de dados, Telegram, controllers ou frameworks.
- `application` pode depender de `domain`, mas não deve depender diretamente de detalhes de infraestrutura.
- `infrastructure` implementa ports de saída definidos pela aplicação.
- `interfaces` chama ports de entrada ou use cases.
- Controllers não devem conter regra de negócio.
- Handlers do Telegram não devem conter regra financeira.
- DTOs REST não devem vazar para o domínio.
- Entities JPA não devem ser usadas como modelo de domínio quando a migração permitir separar essas responsabilidades.

## O que revisar em qualquer PR

### Arquitetura

Verificar:

- Se novas regras foram colocadas na camada correta.
- Se controllers apenas recebem request, validam entrada básica e delegam.
- Se services/use cases concentram regras de aplicação.
- Se o domínio está livre de dependências de framework.
- Se repositories Spring Data não estão vazando para camadas indevidas.
- Se DTOs estão limitados às bordas da aplicação.
- Se a alteração respeita a migração gradual para Clean Architecture.
- Se há acoplamento desnecessário entre camadas.

### Código Java e Spring Boot

Verificar:

- Legibilidade do código.
- Clareza nos nomes de classes, métodos e variáveis.
- Métodos grandes demais.
- Classes com muitas responsabilidades.
- Duplicação de regra de negócio.
- Risco de `NullPointerException`.
- Uso correto de `BigDecimal` para valores monetários.
- Uso adequado de validações.
- Uso adequado de exceptions.
- Uso de `@Transactional` quando houver operação de escrita ou fluxo que precise de consistência.
- Código morto ou métodos não utilizados.
- Imports desnecessários.
- Uso incorreto de `Optional`.
- Conversões ou mapeamentos duplicados.

### Segurança

Verificar:

- Exposição indevida de dados sensíveis.
- Endpoints sem autenticação/autorização quando deveriam estar protegidos.
- Uso indevido de tokens, secrets, senhas ou chaves.
- Logs contendo informações sensíveis.
- Configuração de CORS permissiva demais.
- Validação insuficiente de entrada.
- Tratamento de erro expondo detalhes internos.
- Alterações em autenticação ou autorização que possam enfraquecer a segurança.

### Testes

Verificar:

- Se novas regras possuem testes.
- Se alterações em use cases/services possuem testes unitários.
- Se mudanças em controllers possuem testes quando o contrato HTTP muda.
- Se testes com H2 continuam coerentes com o comportamento esperado.
- Se cenários de erro foram cobertos.
- Se a cobertura JaCoCo pode ser impactada.
- Se testes foram apenas adaptados para passar, sem validar regra real.
- Se mocks estão excessivos ou escondendo comportamento importante.

### SonarQube e manutenibilidade

Verificar:

- Possíveis issues de Maintainability.
- Possíveis issues de Reliability.
- Possíveis Security Hotspots.
- Complexidade ciclomática elevada.
- Duplicação de código.
- Métodos longos demais.
- Condicionais difíceis de entender.
- Classes grandes demais.
- Nomes genéricos.
- Código que dificulta manutenção futura.

### Performance e uso de recursos

Verificar:

- Consultas ao banco que podem gerar lentidão.
- Risco de N+1 queries em relacionamentos JPA.
- Loops desnecessários em listas grandes.
- Carregamento excessivo de dados em memória.
- Endpoints que deveriam ter paginação, filtro ou limite.
- Relatórios/dashboard buscando dados demais sem necessidade.
- Cálculos repetidos em fluxos frequentes.
- Operações síncronas demoradas dentro de requisições.
- Logs excessivos dentro de loops.
- Criação desnecessária de listas intermediárias grandes.
- Uso de `stream`, `collect` ou conversões que prejudiquem clareza ou memória.

Não sugerir otimizações prematuras. Apontar performance apenas quando houver risco real, código claramente ineficiente ou impacto provável em produção.

### Changelog

Verificar se alterações relevantes atualizaram `CHANGELOG.md`.

Usar:

- `Added` para funcionalidades novas, documentação nova ou scripts novos.
- `Changed` para refatorações, mudanças internas e reorganizações.
- `Fixed` para correções de bug.
- `Tests` para criação ou alteração relevante de testes.
- `Security` para correções relacionadas a segurança.

Para refatorações sem mudança funcional externa, preferir `Changed`.

Para melhorias de SonarQube/JaCoCo/manutenibilidade, usar `Changed` e, quando houver alteração em testes, também `Tests`.

## Como responder

Responder sempre em português brasileiro.

O review deve ser útil, didático e objetivo.

Usar o seguinte formato:

## Review técnico do PR

### Resumo das alterações
Explique o que o PR parece mudar.

### Pontos positivos
Liste decisões boas encontradas.

### Riscos ou problemas encontrados
Aponte apenas problemas reais ou riscos relevantes baseados no diff.

### Sugestões de melhoria
Sugira melhorias práticas e objetivas.

### Testes recomendados
Diga quais testes deveriam existir ou serem ajustados.

### Performance
Aponte riscos reais de desempenho, memória ou consultas, se existirem.

### Changelog
Diga se o `CHANGELOG.md` precisa ser atualizado e em qual seção.

### Veredito técnico
Use uma das opções:

- Parece bom para seguir
- Bom, com observações
- Recomendo ajustar antes do merge

## Regras importantes

- Não inventar problemas.
- Não ser genérico.
- Não bloquear PR por detalhe pequeno.
- Não sugerir mudança fora do escopo do PR.
- Não dizer que executou testes se não executou.
- Não aprovar oficialmente o PR.
- Não fazer merge.
- Não sugerir Clean Architecture em todo PR.
- Só apontar arquitetura quando houver violação clara ou quando o PR tocar em refatoração arquitetural.
- Se o PR estiver bom, dizer claramente.
- Se não houver problema de performance, dizer que não encontrou risco relevante de performance no diff analisado.