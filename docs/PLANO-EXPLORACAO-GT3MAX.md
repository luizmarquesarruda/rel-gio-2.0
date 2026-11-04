# Plano de exploração segura — GT3 MAX

## Evidência disponível

A captura local de 04/11/2026 observou seis serviços GATT e onze características. O serviço NUS (6E400001...) recebeu notificações pela característica TX (6E400003...). O serviço CTS padrão 0x1805/0x2A2B não foi anunciado. O serviço padrão de termômetro 0x1809/0x2A1C também não apareceu no inventário compartilhado.

O firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1 são metadados apresentados pelo app/tela, não foram lidos diretamente do GATT. Sem o arquivo de firmware (.bin/.fw/.zip) ou pacote original do Wearfit Pro, não é possível fazer análise estática real de firmware, extrair tabela de comandos ou afirmar que existe correção de RTC/temperatura implementável.

## Fases implementadas nesta branch

1. Planejador GATT determinístico: só cria operações de leitura/inscrição para características Bluetooth SIG conhecidas e com propriedades compatíveis. Serviço Nordic Secure DFU (FE59) é excluído. UUIDs proprietários e NUS RX nunca são sondados.
2. Fingerprint de pacotes: calcula tamanho, prefixo, SHA-256 truncado e contagem de repetição; não guarda payload bruto.
3. Catálogo de experimentos: bateria, hora/data, temperatura, frequência cardíaca, SpO2, passos, sono e atividade. A correlação exige ação manual controlada e evidência repetida.

## Hora e data

O CTS 0x1805 está ausente. O app já tenta sincronização apenas pelo CTS padrão e registra corretamente a ausência. Não se deve enviar payload aleatório ao NUS RX. O caminho de atalho requer descobrir, a partir do firmware ou de captura comparativa Wearfit Pro, qual comando de sincronização o app original envia, seu enquadramento, timezone, checksum, resposta e sequência de inicialização. Até isso existir, o estado correto é “não sincronizado”.

## Temperatura

O serviço padrão 0x1809 não foi observado. A tela de temperatura pode ser clima recebido do telefone, temperatura corporal medida pelo sensor, ou apenas dado interno do firmware. Não tratar como temperatura corporal sem identificar sensor, unidade, faixa, timestamp e referência independente. Não publicar estimativas no Health Connect.

## Firmware e atalhos

Para análise estática real, fornecer firmware extraído do relógio ou pacote de atualização do Wearfit Pro, com origem e hash. O serviço FE59 é associado a DFU e permanece bloqueado. Não executar atualização, downgrade, reset ou escrita em característica proprietária durante a descoberta.

## Testes físicos pendentes

- Confirmar leitura padrão apenas se um perfil padronizado estiver presente.
- Comparar hora/data após sincronização no Wearfit Pro e após reconectar o Relógio 2.0.
- Comparar bateria em pelo menos três níveis.
- Testar temperatura com identificação explícita de “clima” versus “corporal”.
- Repetir cada correlação em pelo menos três sessões.
- Registrar separadamente valor do relógio, valor BLE, Health Connect e referência externa.

Todos os testes físicos permanecem **não validados** até execução no GT3 MAX real.
