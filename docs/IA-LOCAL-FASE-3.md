# Relógio 2.0 — IA local, Fase 3: integração com Health Connect

## Escopo

Conecta a leitura agregada de passos do Health Connect ao histórico local versionado. Não adiciona permissões, não escreve dados no Health Connect, não altera BLE/GATT, nem introduz rede, LLM ou notificações.

## Regra de completude

- Importa somente o dia local anterior, cujo intervalo de calendário já terminou.
- Início e fim são calculados com o ZoneId do Android; java.time resolve offsets e transições de horário.
- O dia atual nunca é gravado como observação diária completa.
- Agregado ausente (null) permanece ausente; não é convertido em zero.
- Cada registro guarda epochDay local, total, origem HEALTH_CONNECT_AGGREGATE, horário de recebimento e estado UNVALIDATED.
- Os passos não são atribuídos ao GT3 MAX: Health Connect pode agregar dados do telefone e de outras fontes autorizadas.
- Repetições com a mesma data, origem, contagem e validação são idempotentes, mesmo com timestamp de recebimento posterior.
- Se o provider corrigir a contagem, uma observação mais recente atualiza a mesma chave dia/origem.

## Execução

- Sincronização iniciada após uma leitura diária bem-sucedida, com no máximo uma tentativa por dia local e processo em execução.
- Ao reiniciar o processo, a leitura pode ser repetida; a chave dia/origem e a idempotência impedem duplicações.
- Não agenda trabalho em segundo plano. A atualização visual de hoje continua no ciclo de vida da Activity.
- Health Connect permanece somente leitura.

## Testes

- Seleção do dia anterior no fuso America/Sao_Paulo.
- Ausência de agregado sem inventar zero.
- Dia incompleto não persistido.
- Repetição idempotente sem regravar bytes.
- Estados de permissão/provider/falha sem gravação.
- Transições DST de 23 e 25 horas.
- Teste físico Galaxy A06/GT3 MAX: **não validado**.
