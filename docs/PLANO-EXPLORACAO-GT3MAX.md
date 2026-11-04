# Plano de exploração segura — GT3 MAX

## Evidência disponível

A captura local de 04/11/2026 observou seis serviços GATT e onze características. O serviço NUS (6E400001...) recebeu notificações pela característica TX (6E400003...). O serviço CTS padrão 0x1805/0x2A2B não foi anunciado. O serviço padrão de termômetro 0x1809/0x2A1C também não apareceu no inventário compartilhado.

O firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1 são metadados apresentados pelo app/tela, não foram lidos diretamente do GATT. Sem o arquivo de firmware (.bin/.fw/.zip) ou pacote original do Wearfit Pro, não é possível fazer análise estática real de firmware, extrair tabela de comandos ou afirmar que existe correção de RTC/temperatura implementável.

## Implementação no branch feat/gt3max-safe-exploration-20261104

### Fase 1 — Exploração GATT segura
- Inventário automático de serviços, características, propriedades e descritores após a descoberta GATT.
- Plano automático para perfis Bluetooth SIG conhecidos: leituras READ e inscrições NOTIFY/INDICATE com CCCD padrão.
- Plano manual aprofundado para canais customizados: somente características que anunciam READ e NOTIFY/INDICATE com CCCD 0x2902.
- O plano manual é limitado a 24 operações por sessão e exige confirmação explícita, pois os payloads podem conter dados privados de saúde.
- FE59/DFU, NUS RX, identificadores de dispositivo (serial/system ID) e serviços SIG já tratados pelo plano padrão são excluídos.
- Nenhuma escrita de payload em característica proprietária é criada. A única escrita de descritor é a configuração padrão CCCD para ativar notificações/indicações.

### Fase 2 — Análise de pacotes
- Fingerprint é calculado sobre cada notificação recebida: tamanho, prefixo, SHA-256 truncado e contagem de repetição.
- O decodificador de fluxo reagrupa quadros AB fragmentados e separa quadros concatenados, com limite de buffer e ressincronização após bytes inválidos.
- A semântica dos comandos permanece independente do framing; comando desconhecido continua desconhecido.
- O catálogo persistente guarda inventário, UUIDs, propriedades, contagens, tamanho e horário da última notificação; payload bruto não é persistido.
- O HEX permanece apenas no log de diagnóstico em memória e na exportação manual TXT.
- Bateria AB/0x91/0x80 continua candidata, não validada.

### Fase 3 — Experimentos guiados
- A tela Autodescoberta BLE apresenta roteiro para bateria, hora/data, temperatura, frequência cardíaca, SpO2, passos, sono e atividade.
- O roteiro exige ação manual e correlação temporal com tráfego observado; não dispara medições nem comandos.
- Dados de passos Health Connect permanecem identificados como agregado externo, sem atribuição automática ao relógio.

## Hora e data

O CTS 0x1805 está ausente. O app sincroniza apenas pelo CTS padrão quando serviço, característica 0x2A2B e WRITE estão presentes. Na captura atual isso não ocorre. Não se deve enviar payload aleatório ao NUS RX.

O atalho de menor risco requer capturar a comunicação normal do Wearfit Pro durante sincronização de hora e comparar com o tráfego do Relógio 2.0. Precisamos descobrir UUID de destino, handshake, enquadramento, timezone, checksum, resposta e sequência de inicialização. Até haver capturas repetidas, a sincronização proprietária permanece bloqueada.

## Temperatura

O serviço padrão 0x1809 não foi observado. A exploração também reconhece o serviço Bluetooth SIG Environmental Sensing 0x181A e seus campos padronizados de temperatura, pressão e umidade quando presentes. Esses serviços não apareceram na captura atual. A tela de temperatura pode ser clima recebido do telefone, temperatura da pele/corporal ou telemetria interna. Não tratar como temperatura corporal sem identificar sensor, unidade, faixa, timestamp e referência independente. Não publicar estimativas no Health Connect.

## Firmware e atalhos

Para análise estática real, fornecer firmware extraído do relógio ou pacote de atualização do Wearfit Pro, com origem e hash. O serviço FE59 é associado a DFU e permanece bloqueado. Não executar atualização, downgrade, reset ou escrita em característica proprietária.

O estudo detalhado está em docs/ATALHO-FIRMWARE-HORA-DATA-TEMPERATURA.md e docs/ANALISE-FIRMWARE-OTA-WEARFIT-PRO.md.

## Testes físicos pendentes

- Confirmar leituras padrão no GT3 MAX real e comparar com a tela do relógio.
- Executar exploração manual e exportar TXT com UUIDs, resultados READ, fingerprints e notificações por canal.
- Comparar hora/data após sincronização no Wearfit Pro e após reconectar o Relógio 2.0.
- Comparar bateria em pelo menos três níveis.
- Testar temperatura com identificação explícita de clima versus corporal.
- Repetir cada correlação em pelo menos três sessões.
- Registrar separadamente valor do relógio, valor BLE, Health Connect e referência externa.

Testes físicos permanecem **não validados** até execução no GT3 MAX real.
