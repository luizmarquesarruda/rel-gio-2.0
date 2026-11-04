# Testes GATT automáticos e coleta local — GT3 MAX Call

**Data da implementação:** 04/11/2026  
**Aplicativo:** Relógio 2.0 / Android Kotlin  
**Hardware de validação planejado:** GT3 MAX Call + Samsung Galaxy A06 / Android 16  
**Estado de validação física:** não validado nesta revisão

## Objetivo

Reduzir a operação manual de diagnóstico. Depois de uma autorização local única, o aplicativo deve:

1. procurar o relógio por BLE e tentar conexão sem exigir digitação de MAC;
2. confirmar compatibilidade pelo fingerprint GATT completo já capturado;
3. percorrer automaticamente todos os canais elegíveis;
4. executar leituras READ e ativar NOTIFY/INDICATE por meio do descritor CCCD padrão;
5. registrar início, callback, resultado, erro, timeout, frequência e volume de tráfego;
6. decodificar apenas formatos Bluetooth SIG conhecidos;
7. guardar dados recebidos localmente durante sessões de captura consentidas e incluí-los no relatório TXT.

## Fluxo executado

`scan BLE → conexão GATT → discoverServices → validação do fingerprint → inventário → fila serial de operações → callbacks → biblioteca local`

A exploração automática não é iniciada para qualquer dispositivo encontrado. Requer:

- consentimento local ativado pelo proprietário;
- serviço NUS e o conjunto completo de serviços/características customizados da captura de referência;
- conexão GATT atual ainda ativa;
- nenhuma exploração automática já iniciada nesta conexão.

O fingerprint GATT é uma verificação de compatibilidade com a captura, não autenticação criptográfica. Endereço MAC, nome anunciado, RSSI e serviço NUS isoladamente não identificam com segurança o relógio.

## Operações permitidas

### Leitura

O app solicita READ somente quando a característica anuncia a propriedade GATT READ. A fila serial não inicia a próxima operação antes do callback correspondente ou do timeout. O retorno positivo imediato de `readCharacteristic` significa apenas que o Android iniciou a operação; o resultado real chega depois em `onCharacteristicRead`.

São registrados separadamente:
- solicitação iniciada ou recusada pela API;
- callback de sucesso ou falha;
- quantidade de bytes;
- valor Bluetooth SIG reconhecido, quando aplicável;
- resposta vazia, formato desconhecido ou timeout.

### Notificações e indicações

O app só ativa NOTIFY/INDICATE quando:
- a característica anuncia a propriedade correspondente;
- existe o descritor Client Characteristic Configuration (CCCD, UUID 0x2902);
- o proprietário ativou a coleta automática ou confirmou a exploração manual.

O fluxo usa a ativação local do Android e o valor padrão do CCCD para notificação ou indicação. Ao pausar o modo automático, o app tenta desativar as inscrições que ele próprio criou, usando o valor padrão DISABLE do mesmo CCCD.

Uma inscrição não obriga o firmware a iniciar uma medição. Apenas permite receber mudanças de valor que o relógio efetivamente emitir.

## Decoders Bluetooth SIG

O módulo `GattValueDecoder` reconhece somente UUIDs e layouts padronizados:

| Serviço / característica | Interpretação do formato |
|---|---|
| Battery 0x180F / 0x2A19 | Percentual de bateria (0–100) |
| Device Information 0x180A / 0x2A24, 0x2A26, 0x2A27, 0x2A28, 0x2A29 | Modelo, fabricante e versões anunciadas como texto |
| Heart Rate 0x180D / 0x2A37, 0x2A38 | Frequência cardíaca, flags, contato, energia, intervalos RR e localização declarada do sensor |
| Blood Pressure 0x1810 / 0x2A35, 0x2A36, 0x2A49 | Pressão, pressão intermediária da braçadeira (incluindo os campos SFLOAT-placeholder exigidos) e recursos declarados |
| Health Thermometer 0x1809 / 0x2A1C, 0x2A1E | Temperatura IEEE-11073 FLOAT e campos opcionais |
| Environmental Sensing 0x181A / 0x2A6D, 0x2A6E, 0x2A6F | Pressão atmosférica, temperatura ambiente SFLOAT e umidade |
| Pulse Oximeter 0x1822 / 0x2A5E, 0x2A5F, 0x2A60 | SpO₂/pulso spot-check ou contínuos, campos opcionais e recursos/status suportados |
| Running Speed and Cadence 0x1814 / 0x2A53, 0x2A54 | Velocidade, cadência, passada/distância opcionais, estado caminhada/corrida e recursos |
| Cycling Speed and Cadence 0x1816 / 0x2A5B, 0x2A5C | Contadores de revolução da roda/pedivela, horários dos últimos eventos e recursos |
| Body Composition 0x181B / 0x2A9B | Recursos de composição corporal e resoluções declaradas de massa/altura; a característica 0x2A9C permanece capturada sem conversão semântica nesta revisão |

A decodificação da característica 0x2A9B informa capacidades e resoluções anunciadas, não uma medição corporal. A característica 0x2A9C (Body Composition Measurement) ainda é preservada em HEX durante a captura, sem apresentar peso, altura, gordura ou massa como valores confirmados: o parser completo precisa respeitar todos os flags, unidades, resoluções do 0x2A9B e regras de pacotes múltiplos.

Para CSC, o aplicativo registra revoluções acumuladas e horários de evento, mas não calcula velocidade/cadência final sem circunferência da roda e histórico temporal suficiente. Para RSC, os campos decodificados são os reportados pelo sensor, não uma validação independente.

Decodificar o formato não valida precisão, calibração, qualidade clínica, localização do sensor ou correspondência com a tela do relógio. Esses dados não são gravados no Health Connect nesta fase.

Se serviço/UUID ou layout não forem conhecidos, a semântica permanece desconhecida. O código não tenta adivinhar unidades nem converter payload proprietário em passos, sono, pressão ou SpO₂.

## Canais proprietários observados

A captura compartilhada identificou, entre outros:

- serviço 0x3802 / característica 0x4A02;
- serviço `cc353442-be58-4ea2-876e-11d8d6976366` / características `c551c36a-0377-4a29-9657-74ffb655a188` e 0x8A8F;
- serviço 0x3803 / característica 0x4A03;
- Nordic UART Service, com TX notificador e RX gravável;
- serviço Secure DFU FE59.

Os UUIDs 0x3802/0x4A02 e os demais canais customizados continuam **sem semântica confirmada**. O app pode executar READ se anunciado e ativar NOTIFY/INDICATE somente com CCCD padrão, após consentimento. HEX é guardado apenas durante uma sessão de captura ativa; fora dela, a biblioteca mantém metadados, tamanho, status, timestamp e fingerprints.

NUS RX, escritas de payload em características customizadas, controles de medição não documentados, FE59/DFU, OTA, reset e restauração de fábrica permanecem bloqueados.

## Persistência e privacidade

- A ativação é uma autorização local persistida no aparelho até o usuário pausar.
- Cada janela de captura tem até 10 minutos; novas janelas são renovadas automaticamente enquanto o modo está habilitado, GATT conectado e a tela principal permanece em primeiro plano. Ao ir para segundo plano, o ticker é suspenso e a renovação só retoma quando o aplicativo volta ao primeiro plano. Não existe serviço foreground nesta fase.
- O HEX de leituras/notificações e os valores decodificados são persistidos apenas durante uma sessão válida.
- A biblioteca privada do app aplica seus limites existentes de quantidade e tamanho de arquivo/registro.
- O TXT exportado pode conter dados de saúde quando houve captura consentida; deve ser tratado como relatório privado.
- Fora de uma sessão ativa, o log não inclui HEX bruto de notificação/leitura.
- Pausar finaliza uma sessão quando ela foi criada pelo modo automático e agenda a desinscrição dos canais ativados por ele. Se o modo automático tiver reutilizado uma sessão iniciada manualmente, essa sessão manual é preservada. A política separada de NUS TX preexistente permanece inalterada.
- A tela Testes e descobertas informa o estado da automação e atualiza o inventário quando retorna ao primeiro plano. A biblioteca exibe serviço, característica, origem, confiança e HEX somente quando houve sessão autorizada.

## Limites que não podem ser confundidos

**Leitura GATT** demonstra que o atributo respondeu, não que o valor seja correto ou atual.

**CCCD confirmado** demonstra que o Android/relógio aceitou a inscrição, não que o relógio enviará dados.

**Notificação observada** demonstra que o firmware enviou bytes naquele canal, não que o conteúdo tenha semântica conhecida.

**Formato Bluetooth SIG decodificado** demonstra que os bytes obedecem ao layout conhecido, não que o sensor seja clinicamente preciso.

**Sem tráfego** pode significar que o firmware não enviou uma atualização, que a medição não foi iniciada no relógio, que o sensor exige uma ação local, ou que o canal/serviço não existe na variante. Não se deve transformar silêncio em zero.

## Por que não iniciar medições por comandos ainda

A documentação pública localizada para o Wearfit Pro descreve uma rota de integração de parceiros, mas não fornece nesta evidência o contrato Android, comandos, payloads e elegibilidade específica do GT3 MAX Call. Ver `INTEGRACAO-WEARFIT-PARTNER-SDK.md`.

Até obter SDK oficial, protocolo documentado ou captura comparativa reproduzível do app oficial, não serão inventados comandos para solicitar pressão, SpO₂, temperatura, passos, sono, horário, reset ou configuração. O modo automático percorre as operações GATT de leitura/recepção seguras e registra o que o firmware realmente entrega.

## Referências técnicas

- Android `BluetoothGatt`: https://developer.android.com/reference/android/bluetooth/BluetoothGatt
- Android `BluetoothGattCallback`: https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback
- Bluetooth SIG Assigned Numbers: https://www.bluetooth.com/specifications/assigned-numbers/
- Bluetooth SIG GATT specifications: https://www.bluetooth.com/specifications/specs/
- Pulse Oximeter Service 1.0.1: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/PLXS_v1.0.1/out/en/index-en.html
- Body Composition Service 1.0.1: https://www.bluetooth.com/specifications/specs/body-composition-service-bcs/
- Running Speed and Cadence Service 1.0.1: https://www.bluetooth.com/specifications/specs/running-speed-and-cadence-service/
- Cycling Speed and Cadence Service 1.0.1: https://files.bluetooth.com/download/cscs_v-1-0-1-pdf/

## Estado dos testes

- Testes unitários de decoders e política de consentimento adicionados.
- Teste de exploração no CI: pendente até execução da nova branch.
- Compilação Android diagnostic/companion após as mudanças: pendente.
- Comunicação física BLE com o GT3 MAX no Galaxy A06: **não validada**.
- Correlação dos valores recebidos com a tela do relógio/Wearfit Pro em sessões repetidas: **não validada**.
- Escrita no Health Connect: permanece bloqueada até fonte, semântica, unidade e origem serem validadas.
