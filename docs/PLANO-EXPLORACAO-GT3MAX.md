# Plano de exploração segura — GT3 MAX

## Evidência disponível

A captura local de 04/11/2026 observou seis serviços GATT e onze características. O serviço NUS (6E400001...) recebeu notificações pela característica TX (6E400003...). O serviço CTS padrão 0x1805/0x2A2B não foi anunciado. O serviço padrão de termômetro 0x1809/0x2A1C também não apareceu no inventário compartilhado.

O firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1 são metadados apresentados pelo app/tela, não foram lidos diretamente do GATT. Sem o arquivo de firmware (.bin/.fw/.zip) ou pacote original do Wearfit Pro, não é possível fazer análise estática real de firmware, extrair tabela de comandos ou afirmar que existe correção de RTC/temperatura implementável.

## Implementação no branch feat/gt3max-safe-exploration-20261104

### Fase 1 — Exploração GATT segura
- Inventário automático de serviços, características, propriedades e descritores após a descoberta GATT.
- Plano automático restrito a metadados básicos e bateria, somente em características Bluetooth SIG com READ.
- Sensores de saúde, medições ambientais e canais customizados podem ser executados pelo plano aprofundado após ativação explícita única do modo automático ou pela ação manual: somente READ e NOTIFY/INDICATE com CCCD 0x2902.
- O plano aprofundado percorre todas as operações elegíveis encontradas no inventário GATT, sem truncamento silencioso por quantidade fixa. No modo automático, uma confirmação local é solicitada uma única vez; o modo permanece ativo até ser pausado. O tempo depende da quantidade de leituras/inscrições e dos timeouts do Android.
- A exploração manual cobre perfis SIG como frequência cardíaca, pressão arterial, oximetria, termômetro, temperatura/pressão/umidade ambientais, composição corporal e cadência, quando anunciados pelo dispositivo.
- FE59/DFU, NUS RX, identificadores de dispositivo (serial/system ID) e serviços SIG já tratados pelo plano padrão são excluídos.
- Nenhuma escrita de payload em característica proprietária é criada. A única escrita de descritor é a configuração padrão CCCD para ativar notificações/indicações.

### Fase 2 — Análise de pacotes
- Fingerprint é calculado sobre cada notificação recebida: tamanho, prefixo, SHA-256 truncado e contagem de repetição.
- O decodificador de fluxo reagrupa quadros AB fragmentados e separa quadros concatenados, com limite de buffer e ressincronização após bytes inválidos.
- A semântica dos comandos permanece independente do framing; comando desconhecido continua desconhecido.
- O catálogo persistente guarda inventário, UUIDs, propriedades, contagens, tamanho e horário da última notificação; payload bruto não é persistido.
- O HEX de leituras e notificações é persistido na biblioteca privada somente durante sessões de captura consentidas; fora da captura, o log/exportação registra UUID, tamanho e fingerprint sem incluir o payload bruto.
- Bateria AB/0x91/0x80 continua candidata, não validada.

### Fase 4 — Testes automáticos e persistência dos resultados
- Uma autorização local única habilita coleta automática após reconhecimento do fingerprint GATT completo do GT3 MAX Call. A preferência pode ser pausada na tela principal.
- O modo automático executa todas as operações elegíveis produzidas por `SafeGattExplorationPlan.buildDeep`: READ em características que anunciam READ e NOTIFY/INDICATE somente com CCCD 0x2902. A fila serial aguarda callback ou timeout para cada operação.
- Resultado de leitura distingue API iniciada, callback GATT bem-sucedido, bytes recebidos, valor decodificado e erro/timeout. Um retorno `true` de `readCharacteristic` é apenas início assíncrono, não prova de resposta.
- Decoders só reconhecem formatos Bluetooth SIG mapeados: Battery 2A19, Device Information 2A24/2A26/2A27/2A28/2A29, Heart Rate 2A37, Blood Pressure 2A35, Health Thermometer 2A1C/2A1E, Environmental Sensing 2A6E/2A6F e Pulse Oximeter 2A5E/2A5F.
- Canais proprietários, inclusive 0x3802/0x4A02 e características correlatas, são testados apenas por READ/CCCD autorizados; sem parser, permanecem UUID, propriedades, status, tamanho, fingerprint e HEX sob captura.
- A biblioteca local registra resultados/descobertas. Payload HEX e valores decodificados são gravados em janelas de até 10 minutos, renovadas enquanto o modo automático está habilitado e a conexão GATT permanece ativa. O limite de armazenamento existente continua aplicado.
- Pausar encerra a captura e cancela as inscrições automáticas através do CCCD padrão. A inscrição NUS TX automática já existente segue política própria.
- Notificações não iniciam a medição no wearable. Para disparar sob demanda, é necessária documentação/captura validada de comando oficial; nenhum comando proprietário foi inventado ou enviado.
- Testes Kotlin cobrem decoders e a política de consentimento; CI e teste físico no Galaxy A06/GT3 MAX ficam pendentes.

### Fase 3 — Experimentos guiados
- A tela principal oferece ativação única dos testes GATT automáticos; após fingerprint completo do GT3 MAX Call, o aplicativo executa sozinho as leituras e inscrições elegíveis e salva resultados na biblioteca local.
- O roteiro guiado permanece disponível para correlacionar horário de uso e tráfego; ele não dispara medições por comandos proprietários desconhecidos.
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
- Ativar testes automáticos, conectar o GT3 MAX real, manter captura por pelo menos 10 minutos e exportar TXT com UUIDs, resultados READ, fingerprints, CCCD e notificações por canal.
- Comparar hora/data após sincronização no Wearfit Pro e após reconectar o Relógio 2.0.
- Comparar bateria em pelo menos três níveis.
- Testar temperatura com identificação explícita de clima versus corporal.
- Repetir cada correlação em pelo menos três sessões.
- Registrar separadamente valor do relógio, valor BLE, Health Connect e referência externa.

Testes físicos permanecem **não validados** até execução no GT3 MAX real.
