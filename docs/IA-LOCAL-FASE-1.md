# Relógio 2.0 — IA local, Fase 1

## Escopo implementado

Primeiro núcleo determinístico de inteligência local para tendência de passos. Não inclui LLM, rede, notificações, WorkManager, alteração de UI, ingestão de sensores BLE ou persistência longitudinal.

## Contrato da análise

- Entrada: 14 agregados diários completos e consecutivos, referentes a dias encerrados.
- Comparação: média dos primeiros 7 dias contra a média dos últimos 7 dias.
- Limiar inicial: variação absoluta de 25%.
- Queda significativa: insight de atenção.
- Aumento significativo: insight informativo.
- Variações abaixo do limiar: nenhum insight.
- Dados ausentes, duplicados ou não consecutivos: análise insuficiente.
- Fontes diferentes na mesma janela: análise recusada.
- GT3 MAX: só é aceito se cada observação estiver marcada como PHYSICALLY_VALIDATED.
- Health Connect permanece identificado como agregado; não é reclassificado como GT3 MAX.

O limiar de 25% é uma regra inicial de produto, não um parâmetro clínico. Deve ser calibrado com dados reais e revisão de UX antes de alertas ao usuário.

## Limites

- Não diagnostica, prescreve ou faz recomendações médicas.
- Não presume que dados Health Connect vieram do relógio.
- Não exige API externa ou chave de API.
- Não introduz permissões Android.
- O Domain não importa Android, Health Connect, armazenamento ou BLE.
- Não existe ainda agendamento proativo; isso depende da camada Platform e de uma política de notificações aprovada.

## Próxima etapa

Fase 2 implementou a fundação de histórico versionado e escrita atômica. A próxima etapa é integrar o repositório ao agregado diário do Health Connect com testes de origem, fuso horário, dia incompleto, repetição e atualização; só depois avaliar WorkManager e notificações locais.
