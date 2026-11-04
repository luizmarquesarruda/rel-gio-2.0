# Contrato de métricas — Saúde e atividade

## Objetivo

Definir uma fronteira independente de Android, BLE e Wearfit Pro para normalizar futuras amostras. O contrato não interpreta payloads nem habilita ingestão de sensores.

## Campos obrigatórios

- Tipo de métrica e unidade explícitos.
- Valor finito e não negativo.
- Origem preservada: GT3 MAX, telefone, agregado Health Connect, Wearfit Pro ou entrada manual.
- Timestamp de recebimento obrigatório.
- Timestamp de medição opcional, nunca substituído silenciosamente pelo horário de recebimento.
- Estado de validação explícito.

## Regras específicas

- FC usa batimentos por minuto.
- SpO₂ usa percentual entre 0 e 100; permanece não diagnóstico.
- Pressão sistólica/diastólica usa mmHg e continua identificada como estimativa do wearable até validação clínica independente. O aviso do fabricante diz que os resultados servem apenas como referência.
- Passos usam contagem inteira.
- Duração do sono usa minutos; fases e intervalos exigem modelo próprio antes de ingestão.
- METT usa unidade MET e não é convertido automaticamente em calorias.
- Estresse permanece com unidade desconhecida até confirmar escala e semântica.

## Portão de validação

Uma amostra só é marcada como fisicamente validada quando:
1. A origem GT3 MAX foi identificada sem depender exclusivamente do endereço MAC.
2. Tipo, unidade e semântica foram confirmados por comparação controlada com o relógio e Wearfit Pro.
3. Foram obtidas pelo menos duas sessões independentes e repetíveis.
4. Timestamp, duplicidade, reconexão e qualidade foram avaliados.

PHYSICALLY_VALIDATED não significa validação clínica. Em especial, pressão arterial e SpO₂ não devem ser apresentados como diagnóstico ou substitutos de instrumentos médicos.

## Health Connect

Este contrato não concede permissão nem realiza escrita. A integração exige um mapeamento próprio por tipo de registro, compatibilidade semântica, consentimento e política de deduplicação. Um agregado Health Connect nunca deve ser reclassificado como origem GT3 MAX.

## Estado

O modelo Kotlin e os testes unitários cobrem tipagem, unidades, limites básicos e preservação de origem. Nenhum parser de saúde BLE foi criado; não há leitura de sensores do GT3 MAX validada fisicamente.
