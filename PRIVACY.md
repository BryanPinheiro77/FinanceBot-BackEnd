# Privacidade

O FinanceBot pode processar dados financeiros pessoais, identificadores do Telegram, nome, e-mail, categorias, descrições e estado de conversas, conforme os recursos habilitados pelo operador.

## Execução self-hosted

Nesta modalidade, a pessoa ou organização que instala o FinanceBot controla a infraestrutura, os bancos, os backups, os logs e os serviços externos. O operador deve:

- informar seus usuários sobre os dados coletados e a finalidade;
- limitar acesso por usuário e por serviço;
- definir retenção e exclusão de dados;
- proteger backups, secrets e endpoints administrativos;
- avaliar requisitos legais aplicáveis à sua operação.

O repositório não deve receber dados reais de usuários em testes, issues, logs ou pull requests.

## Serviços externos

Telegram, OpenAI e outros provedores podem receber dados quando suas integrações estiverem habilitadas. Revise os termos e as políticas desses provedores antes de ativar uma integração em produção. A configuração de IA deve enviar somente o conteúdo necessário para a finalidade escolhida.

## Serviço hospedado

O projeto atualmente documenta o modo self-hosted. Um futuro serviço hospedado deverá publicar política de privacidade, termos de uso, retenção, subprocessadores e canais de atendimento próprios antes de aceitar dados de clientes.

Este documento é uma orientação técnica para o repositório e não constitui aviso legal ou aconselhamento jurídico.
