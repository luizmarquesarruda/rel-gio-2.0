# Captura BLE — GT3 MAX — 03/11/2026

## Contexto

- Ferramenta: nRF Connect for Mobile.
- Nome anunciado/lido: `GT3 MAX`.
- Telefone informado: Samsung Galaxy A06, Android 16.
- Endereço MAC: omitido intencionalmente deste documento.
- Conexão: BLE/GATT, status 0 (conectado).
- Descoberta de serviços: status 0 (sucesso).

## Serviços observados no relógio

| Serviço | Características observadas |
|---|---|
| `00003802-0000-1000-8000-00805f9b34fb` | `00004a02-0000-1000-8000-00805f9b34fb` — Notify, Read, Write |
| `cc353442-be58-4ea2-876e-11d8d6976366` | `c551c36a-0377-4a29-9657-74ffb655a188`; `00008a8f-0000-1000-8000-00805f9b34fb` — Notify, Read, Write |
| Generic Access `0x1800` | Device Name `0x2A00`; Appearance `0x2A01` |
| `00003803-0000-1000-8000-00805f9b34fb` | `00004a03-0000-1000-8000-00805f9b34fb` — Notify, Read, Write, Write Without Response |
| Nordic UART `6e400001-b5a3-f393-e0a9-e50e24dcca9e` | TX `6e400003...` — Notify; RX `6e400002...` — Write Without Response |
| Secure DFU `0xFE59` | DFU Control Point, DFU Packet e característica `00000003...` |

## Leituras observadas

- `0x4A02`: resposta de 0 bytes.
- `c551c36a...`: resposta de 0 bytes.
- `0x8A8F`: resposta de 0 bytes.
- `0x4A03`: resposta de 0 bytes.
- Device Name `0x2A00`: texto `GT3 MAX`.
- Appearance `0x2A01`: `00 00`.
- DFU Packet e `00000003...`: 0 bytes.

Uma leitura de 0 bytes não prova que uma característica não tenha função; pode depender de notificações ou de uma troca proprietária iniciada pelo aplicativo companheiro.

## Pacote bruto observado

Uma notificação foi recebida pelo Nordic UART TX:

```
AB-00-05-FF-91-80-00-0C
```

- Comprimento: 8 bytes.
- Significado: desconhecido / não validado.
- Não classificar como frequência cardíaca, passos, SpO2 ou qualquer outra métrica até existir correlação repetível.

## Observações sobre notificações

A notificação de `0x8A8F` ficou habilitada por aproximadamente 1,7 segundo e foi desabilitada em seguida. O intervalo é insuficiente para concluir que a característica não transmite dados.

## Distinção importante: servidor GATT do telefone

Ao final do registro, o nRF Connect iniciou um servidor GATT local no telefone e criou os serviços Heart Rate `0x180D`, User Data `0x181C` e um serviço customizado `0xAAA0`.

Esses serviços foram criados pelo telefone. Não são evidência de que o GT3 MAX os ofereça.

## Decisões de segurança

- Não escrever nas características desconhecidas.
- Não enviar bytes pela Nordic UART RX.
- Não executar Secure DFU, OTA, reset ou calibração.
- Permitir apenas leitura GATT explícita e inscrição em notificações pelo descritor padrão CCCD `0x2902`.
- Não publicar o endereço MAC.
- Manter os bytes recebidos como evidência bruta, sem inferir métricas.

## Estado da identificação

- Conectividade BLE: confirmada nesta sessão.
- Inventário GATT: capturado nesta sessão.
- Protocolo proprietário: não identificado.
- Chipset/fabricante: não identificados.
- Métricas de saúde: não validadas.

## Análise retrospectiva da captura (03/11/2026)

A notificação AB 00 05 FF 91 80 00 0C foi comparada ao projeto público https://github.com/fbiego/dt78/blob/master/dt78_ble_server/dt78_ble_server.ino, que declara os mesmos UUIDs NUS terminados em dcca9e e uma fixture AB 00 05 FF 91 80 00 50 comentada como bateria 80%.

A estrutura é compatível com um quadro de bateria da referência. O último byte 0C sugeriria 12% se o mesmo formato for usado pelo GT3 MAX. **Esse valor permanece candidato/não validado**, pois ainda não houve comparação simultânea com a indicação de bateria do Wearfit Pro nem captura em diferentes níveis de carga.

O parser Kotlin adicionado reconhece apenas quadros completos desse formato e mantém a saída como interpretação candidata. Não envia comandos e não decodifica outros sensores.
