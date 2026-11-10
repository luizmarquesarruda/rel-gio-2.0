## 2026-11-10 — IA local, Fase 3: integração Health Connect

- Integrada a leitura agregada de passos do Health Connect ao histórico local versionado.
- A sincronização importa somente o dia local anterior, depois de seu intervalo de calendário terminar; usa ZoneId do Android para calcular limites.
- Dia atual/incompleto nunca é armazenado como observação diária completa.
- Agregado ausente permanece null e não é convertido em zero.
- Proveniência preservada como HEALTH_CONNECT_AGGREGATE e validação UNVALIDATED; nenhum dado é atribuído ao GT3 MAX.
- Repetição com mesma data/origem/contagem/validação não regrava o arquivo, mesmo com timestamp de recebimento posterior.
- Integração é disparada após leitura diária bem-sucedida, com no máximo uma tentativa por dia local e processo. Não adiciona serviço de segundo plano.
- Testes cobrem seleção de dia/fuso, ausência de agregado, dia incompleto, idempotência, falhas e transições DST.
- CI da Fase 3 e teste físico Galaxy A06/GT3 MAX: **aguardando validação**.

