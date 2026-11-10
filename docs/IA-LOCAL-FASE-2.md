# Relógio 2.0 — IA local, Fase 2: histórico diário

## Escopo

Implementa a fundação de persistência local para observações diárias de passos. Não altera BLE, Health Connect, permissões, UI, notificações ou agendamento.

## Contrato de armazenamento

- Arquivo privado do sandbox do aplicativo, escrito por Android AtomicFile.
- Codec binário independente de Android, schema versionado inicial 1.
- Cada registro preserva dia (epoch day), passos, origem, timestamp de recebimento e estado de validação.
- Chave lógica única: dia + origem. Fontes distintas não são fundidas.
- Repetição idêntica é idempotente. Uma observação recebida depois pode atualizar a mesma chave; uma observação mais antiga ou conflitante com timestamp igual é ignorada.
- Leitura distingue arquivo vazio, disponível, corrompido e schema futuro.
- Arquivo corrompido ou schema futuro nunca é sobrescrito automaticamente.
- Escrita usa AtomicFile; falha de escrita é reportada sem confirmar sucesso.
- Limite: 4.000 observações e 1 MiB. Não há limpeza automática ou descarte dos registros antigos; ao atingir o limite, novas chaves são recusadas.
- O arquivo permanece no armazenamento interno privado do app. Não há criptografia adicional nesta fase nem transmissão de dados.

## Testes

Cobertura unitária de proveniência, idempotência, atualização, rejeição de dado antigo, separação por origem, consulta por intervalo, corrupção, schema futuro, conteúdo malformado e falha de gravação simulada.

## Limitações e próxima etapa

- Repositório ainda não foi ligado ao HealthConnectStepsRepository nem ao ActivityTrendEngine.
- Nenhuma amostra do GT3 MAX é inserida automaticamente; validação física continua obrigatória.
- Schema 1 é a versão inicial. Migrações serão adicionadas somente quando houver mudança real de formato, com testes dedicados.
- A capacidade de 4.000 observações é limite de segurança, não política de retenção. É necessário revisar capacidade e política de exportação/limpeza explícita antes de coleta longitudinal contínua.
- Teste físico no Galaxy A06 e GT3 MAX: **não validado**.
