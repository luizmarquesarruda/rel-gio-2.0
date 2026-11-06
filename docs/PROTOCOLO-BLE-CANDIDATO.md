# Perfil BLE candidato — NUS / quadros AB

## Resumo executivo

A captura do GT3 MAX contém uma correspondência de UUIDs e framing com um projeto público de emulação BLE da família DT78:

- Serviço NUS: `6e400001-b5a3-f393-e0a9-e50e24dcca9e`.
- RX (app → relógio): `6e400002-b5a3-f393-e0a9-e50e24dcca9e`.
- TX (relógio → app): `6e400003-b5a3-f393-e0a9-e50e24dcca9e`.
- Captura do GT3 MAX: `AB 00 05 FF 91 80 00 0C`.
- Fixture pública no projeto DT78: `AB 00 05 FF 91 80 00 50`, comentada como notificação de bateria em 80%.

Referência: https://github.com/fbiego/dt78/blob/master/dt78_ble_server/dt78_ble_server.ino. O código declara os mesmos UUIDs NUS terminados em dcca9e e a fixture de bateria.

## Grau de confiança e limites

**Classificação atual: compatibilidade de protocolo candidata; identidade do chipset/modelo não confirmada.**

A coincidência simultânea dos UUIDs e do formato/bytes da fixture é forte o bastante para justificar um parser isolado. Não prova que o GT3 Maxx seja um DT78, X8 Ultra ou use o mesmo firmware. O projeto upstream é um emulador de servidor BLE e declara como alvo WearFit 2.0; não é documentação oficial do fabricante do GT3 Maxx nem SDK Wearfit Pro.

Outro projeto público documenta Bluetrum AB5610, mas usa UUID NUS terminado em dcca9f e quadros DF/FD. Essa família não deve ser misturada com a captura dcca9e / AB: https://github.com/victorbillyph/chinese-smartwatch-homeassistant.

## Interpretação implementada

O app reconhece somente quadros completos com cabeçalho 0xAB, tamanho big-endian nos bytes 1–2, tamanho total igual a 3 + tamanho declarado, marcador 0xFF no byte 3, comando no byte 4 e resposta no byte 5.

Interpretação específica de bateria, ainda candidata: comando 0x91, resposta 0x80, último byte do payload entre 0 e 100.

Assim, a captura ... 00 0C produz **12% candidatos**, não um valor confirmado. A fixture pública ... 00 50 produz 80%, conforme comentário no código de referência. O app registra a interpretação como não validada e conserva os bytes hexadecimais originais.

O parser rejeita cabeçalho incorreto, tamanho divergente, quadro incompleto e comandos não reconhecidos como bateria. Ainda não faz reassemblagem de fragmentos, processamento de vários quadros concatenados, checksum genérico ou decodificação de outros comandos.

## Configuração aplicada no Relógio 2.0

- Perfil candidato NUS_AB_FRAME_COMPATIBLE_CANDIDATE.
- UUIDs de serviço, RX e TX centralizados em Gt3MaxProtocolProfile.
- Parser puro Kotlin Gt3MaxAbFrameParser, sem dependência Android.
- Testes offline para captura real, fixture pública, tamanho inválido, cabeçalho inválido, comando desconhecido e quadro incompleto.
- O inventário GATT mostra o perfil como candidato quando encontra o serviço e a característica TX.
- O parser só interpreta quadros recebidos no canal TX NUS observado; não inicia inscrição automaticamente.

## Segurança

- Nenhum byte é enviado para RX.
- Nenhuma escrita proprietária foi adicionada.
- O CCCD padrão 0x2902 permanece a única escrita GATT usada para habilitar notificações explicitamente solicitadas pelo usuário.
- Serviço Secure DFU 0xFE59 permanece fora do escopo. Não executar OTA, DFU, reset, configuração, calibração ou comandos de identificação.
- Não encaminhar notificações ao relógio até existir especificação comprovada e teste controlado.

## Validação ainda necessária

1. Capturar o mesmo tipo de quadro em pelo menos duas sessões.
2. Observar o indicador de bateria no próprio relógio no mesmo instante.
3. Repetir em níveis diferentes de bateria, sem enviar comandos.
4. Confirmar se 0x0C corresponde realmente a 12%.
5. Registrar as versões de firmware/hardware diretamente no relógio e comparar inventários GATT.

Até cumprir esses passos, a interface e os logs devem dizer candidato ou não validado, nunca bateria confirmada. O Relógio 2.0 opera diretamente com o GT3 MAX e não depende de aplicativo companheiro.