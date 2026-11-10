# Relógio 2.0 — IA local, Fase 2: histórico diário

## Escopo

Implementa a fundação de persistência local para observações diárias de passos. Não altera BLE, permissões, UI, notificações ou agendamento. A integração com Health Connect foi adicionada na Fase 3.

## Contrato de armazenamento

- Arquivo privado do sandbox do aplicativo, escrito por Android AtomicFile.
- Codec binário independente de Android, schema versionado inicial 1.
- Cada registro preserva dia (epoch day), passos, origem, timestamp de recebimento e estado de validação.
- Chave lógica única: dia + origem. Fontes distintas não são fundidas.
- Repetição da mesma contagem e estado de validação é idempotente, mesmo com timestamp posterior. Uma contagem corrigida mais recente pode atualizar a chave.
- Leitura distingue arquivo vazio, disponível, corrompido, falha de armazenamento e schema futuro.
- Arquivo corrompido ou schema futuro nunca é sobrescrito automaticamente.
- Escrita usa AtomicFile; falha de escrita é reportada sem confirmar sucesso.
- Limite: 4.000 observações e 1 MiB. Não há limpeza automática ou descarte dos registros antigos; ao atingir o limite, novas chaves são recusadas.
- O arquivo permanece no armazenamento interno privado do app. Não há criptografia adicional nesta fase nem transmissão de dados.

## Testes

Cobertura unitária de proveniência, idempotência, atualização, rejeição de dado antigo, separação por origem, consulta por intervalo, corrupção, schema futuro, conteúdo malformado e falha de gravação simulada.

## Limitações e próxima etapa

- A Fase 3 conectou o histórico ao HealthConnectStepsRepository. O motor de tendências ainda não consome o histórico automaticamente.
- Nenhuma amostra do GT3 MAX é inserida automaticamente; validação física continua obrigatória.
- Schema 1 é a versão inicial. Migrações serão adicionadas somente quando houver mudança real de formato, com testes dedicados.
- A capacidade de 4.000 observações é limite de segurança, não política de retenção. É necessário revisar capacidade e política de exportação/limpeza explícita antes de coleta longitudinal contínua.
- Teste físico no Galaxy A06 e GT3 MAX: **não validado**.
