# Plano de exploração segura — GT3 MAX

## Evidência disponível

A captura local de 04/11/2026 observou seis serviços GATT e onze características. O serviço NUS (6E400001...) recebeu notificações pela característica TX (6E400003...). O serviço CTS padrão 0x1805/0x2A2B não foi anunciado. O serviço padrão de termômetro 0x1809/0x2A1C também não apareceu no inventário compartilhado.

O firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1 são metadados apresentados pelo app/tela, não foram lidos diretamente do GATT. Sem o arquivo de firmware (.bin/.fw/.zip) ou pacote original do Wearfit Pro, não é possível fazer análise estática real de firmware, extrair tabela de comandos ou afirmar que existe correção de RTC/temperatura implementável.

## Implementação na branch feat/gt3max-safe-exploration-20261104

### Fase 1 — Exploração GATT segura
- Inventário automático dos serviços, características, propriedades e descritores após a descoberta GATT.
- Plano determinístico de operações permitido por lista de UUIDs Bluetooth SIG conhecidos.
- Leituras padronizadas são executadas em fila serial, aguardando callbacks GATT; valores são registrados no diagnóstico local.
- Notificações/indicações padronizadas só são ativadas quando a característica tem propriedade compatível e CCCD 0x2902.
- Número de série (2A25) foi excluído de leituras automáticas por minimização de dados.
- Serviços FE59/DFU, NUS RX e UUIDs proprietários são excluídos do plano.

### Fase 2 — Análise de pacotes
- O motor registra serviço/característica de origem, contagem, tamanho, horário e fingerprint de quadros AB conhecidos.
- O parser atual aceita somente um quadro AB completo; fragmentação e concatenação ainda não são inferidas.
- O catálogo persistente guarda metadados e contagens, não payload bruto.
- O log HEX de diagnóstico continua em memória e só é exportado manualmente pelo usuário.
- Semântica não reconhecida permanece desconhecida; bateria AB/0x91/0x80 permanece candidata.

### Fase 3 — Experimentos guiados
- A tela Autodescoberta BLE agora exibe o catálogo de experimentos: bateria, hora/data, temperatura, frequência cardíaca, SpO2, passos, sono e atividade.
- O roteiro exige ação manual e correlação temporal com o tráfego observado; não dispara medições nem comandos proprietários.
- A tela descreve evidência necessária e impede que o usuário confunda canal GATT com sensor validado.

## Hora e data

O CTS 0x1805 está ausente. O app sincroniza apenas pelo CTS padrão quando serviço, característica 0x2A2B e WRITE estão presentes. Na captura atual isso não ocorre. Não se deve enviar payload aleatório ao NUS RX.

O atalho de menor risco requer capturar a comunicação normal do Wearfit Pro durante sincronização de hora e comparar com o tráfego do Relógio 2.0. Precisamos descobrir UUID de destino, handshake, enquadramento, timezone, checksum, resposta e sequência de inicialização. Até haver capturas repetidas, a sincronização proprietária permanece bloqueada.

## Temperatura

O serviço padrão 0x1809 não foi observado. A tela de temperatura pode ser clima recebido do telefone, temperatura da pele/corporal ou telemetria interna. Não tratar como temperatura corporal sem identificar sensor, unidade, faixa, timestamp e referência independente. Não publicar estimativas no Health Connect.

## Firmware e atalhos

Para análise estática real, fornecer firmware extraído do relógio ou pacote de atualização do Wearfit Pro, com origem e hash. O serviço FE59 é associado a DFU e permanece bloqueado. Não executar atualização, downgrade, reset ou escrita em característica proprietária.

O estudo detalhado está em docs/ATALHO-FIRMWARE-HORA-DATA-TEMPERATURA.md.

## Testes físicos pendentes

- Confirmar leituras padrão no GT3 MAX real e comparar com a tela do relógio.
- Comparar hora/data após sincronização no Wearfit Pro e após reconectar o Relógio 2.0.
- Comparar bateria em pelo menos três níveis.
- Testar temperatura com identificação explícita de “clima” versus “corporal”.
- Repetir cada correlação em pelo menos três sessões.
- Registrar separadamente valor do relógio, valor BLE, Health Connect e referência externa.

Testes físicos permanecem **não validados** até execução no GT3 MAX real.
