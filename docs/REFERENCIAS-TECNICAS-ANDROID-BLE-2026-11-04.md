# Referências técnicas — Android BLE, armazenamento e Bluetooth SIG

Consulta realizada em 04/11/2026 para a revisão do Relógio 2.0. As URLs abaixo são documentação oficial de Android Developers e Bluetooth SIG.

## Android BLE / GATT

### Service Changed

Fonte: [BluetoothGattCallback.onServiceChanged](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onServiceChanged(android.bluetooth.BluetoothGatt))

A callback informa que a tabela de atributos do dispositivo foi alterada e que a instância local não deve continuar usando a tabela antiga. Após o evento, o cliente deve redescobrir os serviços. Aplicação no repositório:
- verificar que o callback pertence à conexão GATT atual;
- não retomar a descoberta durante desconexão ou handoff ao Wearfit Pro;
- limpar operações GATT pendentes e referências locais;
- chamar `discoverServices()` novamente e manter watchdog;
- não confundir o inventário anterior com dados da sessão atual.

### Callbacks de leitura e API 33+

Fonte: [BluetoothGattCallback.onCharacteristicRead](https://developer.android.com/reference/android/bluetooth/BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt,android.bluetooth.BluetoothGattCharacteristic,byte%5B%5D,int))

Android adicionou a variante que recebe o `byte[] value` no callback e marcou a antiga variante como deprecated na API 33. O projeto mantém ambas para compatibilidade com Android anterior, mas valida conexão e chave da operação ativa antes de persistir o resultado. Callback atrasado ou não correspondente é descartado.

### Permissões BLE

Fonte: [Bluetooth permissions](https://developer.android.com/develop/connectivity/bluetooth/bt-permissions)

O manifesto mantém a separação de Android 12+: `BLUETOOTH_SCAN` com `neverForLocation` e `BLUETOOTH_CONNECT`, além das permissões legadas limitadas à API 30. Esta revisão não adicionou permissão.

## Persistência local

### SharedPreferences

Fonte: [SharedPreferences.Editor.apply](https://developer.android.com/reference/android/content/SharedPreferences.Editor#apply())

`apply()` atualiza a representação em memória imediatamente e programa a persistência em disco para ocorrer de forma assíncrona. Não fornece callback de erro de escrita. Aplicação: a atualização do inventário GATT de metadados não bloqueia o callback Bluetooth com `commit()`. A confirmação de retorno é apenas que a atualização em memória/agendamento foi aceito, não uma garantia de persistência durável imediata em caso de encerramento abrupto.

### AtomicFile

Fonte: [android.util.AtomicFile](https://developer.android.com/reference/android/util/AtomicFile)

`AtomicFile` oferece sequência de escrita substituível via `startWrite()`, `finishWrite()` e `failWrite()`, mas não fornece sincronização de chamadas concorrentes ao consumidor. O app mantém um lock compartilhado por processo e fila serial para operações de telemetria. O formato continua versionado, limitado e privado.

## Bluetooth SIG — serviços e perfis

### Generic Access e Generic Attribute

Fonte: [Bluetooth SIG Assigned Numbers](https://www.bluetooth.com/specifications/assigned-numbers/)

Os UUIDs primários `0x1800` Generic Access e `0x1801` Generic Attribute são serviços SIG básicos. Não são serviços proprietários do fabricante do relógio. O inventário agora os exclui da lista de canais proprietários, sem iniciar leituras ou inscrições adicionais.

### Pulse Oximeter Service — Spot-check Measurement

Fonte: [Pulse Oximeter Service v1.0.1](https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/PLXS_v1.0.1/out/en/index-en.html)

No flag field da característica PLX Spot-check Measurement:
- bit 0: timestamp presente;
- bit 1: Measurement Status presente;
- bit 2: Device/Sensor Status presente;
- bit 3: Pulse Amplitude Index presente;
- bit 4: Device Clock Is Not Set;
- bits 5–7: reservados.

O decoder antigo rejeitava os quatro bits superiores, portanto descartava o flag válido bit 4. Agora aceita bit 4, o apresenta como estado de qualidade/timestamp e continua rejeitando bits reservados 5–7. O novo teste cobre os dois casos.

## Limites da interpretação

- Um UUID ou propriedade GATT indica presença de um perfil/capacidade declarada, não comprova que uma função da interface funcione.
- Um frame PLX sintaticamente válido não comprova qualidade clínica, precisão ou calibração.
- O candidato AB `0x91/0x80` continua sem semântica confirmada no GT3 MAX Call.
- O teste de callback/rediscovery ainda depende de execução da CI e de ensaio físico no Galaxy A06.
- Nenhuma escrita proprietária, NUS RX, CTS, OTA, reset, calibração, mudança de idioma, vibração ou escrita de saúde foi introduzida nesta rodada.

