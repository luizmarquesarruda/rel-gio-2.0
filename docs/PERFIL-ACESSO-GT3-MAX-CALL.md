# Perfil de acesso — GT3 MAX Call

**Revisão:** 04/11/2026  
**Objetivo:** conectar o Relógio 2.0 ao GT3 MAX Call e aproveitar as funções que o dispositivo realmente expõe. Não é um plano de atualização de firmware.

## Identificação recebida do proprietário

- Nome do produto: GT3 MAX Call
- Modelo: GT3 MAX
- Firmware informado: `V1.07`
- Kernel / build informado: `SH0050-1_1.1.3_1.23_1.18_1.1`

O serial, código de pareamento e MAC foram omitidos deste documento público. O endereço Bluetooth deve permanecer em preferências privadas do aplicativo e nunca ser embutido no código público. O app já oferece armazenamento local da identidade por endereço.

**Importante:** V1.07 e SH0050 são metadados informados pelo proprietário. A captura GATT conhecida não mostra um serviço Device Information (`0x180A`) nem características que comprovem a leitura dessas versões diretamente pelo app.

## Evidência GATT disponível

Captura realizada com nRF Connect em 03/11/2026:

| Serviço | Evidência | Utilização no app |
|---|---|---|
| `6e400001-b5a3-f393-e0a9-e50e24dcca9e` | Nordic UART; TX Notify, RX Write Without Response | Escutar TX; RX permanece bloqueada |
| `00003802-0000-1000-8000-00805f9b34fb` | característica `00004a02...` | Inventário; sem semântica conhecida |
| `cc353442-be58-4ea2-876e-11d8d6976366` | características `c551c36a...` e `00008a8f...` | Inventário; sem semântica conhecida |
| `00003803-0000-1000-8000-00805f9b34fb` | característica `00004a03...` | Inventário; sem semântica conhecida |
| `0xFE59` | Secure DFU | Identificação apenas; não utilizado pelo app |

A assinatura GATT (NUS + três serviços privados) pode identificar uma compatibilidade com a captura deste relógio, mas não autentica hardware, fabricante ou firmware.

## Acesso implementado nesta fase

1. O app continua exigindo correspondência com o endereço Bluetooth configurado localmente. O MAC não é compilado dentro do APK nem publicado no GitHub.
2. Após a descoberta GATT, o app compara o nome GT3 MAX/GT3 MAX Call e o conjunto de serviços/características observado.
3. Somente se nome e fingerprint GATT completo coincidirem, o app habilita automaticamente notificações apenas na característica NUS TX, usando o descritor padrão CCCD (`0x2902`). Correspondência parcial não inicia escuta automática.
4. O app interpreta o quadro AB `0x91/0x80` somente como **candidato de bateria**. A captura `AB 00 05 FF 91 80 00 0C` sugere 12%, mas essa semântica ainda precisa de correlação repetida com o indicador de bateria do próprio relógio.
5. O app não escreve bytes em NUS RX nem em características privadas. A configuração CCCD não é um comando proprietário.

## Funções e limites atuais

| Função | Estado |
|---|---|
| Localizar e conectar ao GT3 MAX configurado | Implementado por BLE/GATT; teste físico após esta fase pendente |
| Descobrir serviços e propriedades GATT | Implementado |
| Receber notificações NUS TX | Inscrição automática condicional adicionada; validação em relógio pendente |
| Bateria | Parser candidato; aguarda confirmação por comparação com o indicador do relógio |
| Frequência cardíaca, SpO₂, pressão, sono e passos diretos | Não há parser/protocolo comprovado; não simular |
| Chamadas e áudio da variante Call | Canal Bluetooth Classic/HFP/A2DP separado; ainda não validado |
| Notificações Android para o relógio | Captura Android local já existe na companion; transporte ao relógio continua sem protocolo |
| Hora, brilho, botão, bloqueio e controles | Requerem protocolo/ação documentada; não enviar comandos experimentais |
| DFU / OTA / reset | Não fazem parte deste objetivo e continuam fora do escopo |

## Próximas implementações orientadas por evidência

- Correlacionar a notificação candidata de bateria com a porcentagem exibida no próprio relógio em diferentes níveis de carga.
- Capturar notificações TX durante medições iniciadas diretamente no relógio: frequência cardíaca, SpO₂, pressão, passos, sono e exercícios. Cada métrica exige duas ou mais observações independentes, timestamp e comparação da UI.
- Identificar separadamente o canal Bluetooth Classic de chamadas/áudio; não inferir seu endereço nem reutilizar UUIDs BLE.
- Ler somente características com semântica pública/documentada ou já comprovada por correlação.
- Manter valores brutos, origem e estado de validação; nenhuma medição desconhecida pode ser exibida como dado de saúde.

## Referências técnicas

- Captura GATT do projeto: [CAPTURA-BLE-GT3-MAX-2026-11-03.md](CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- Perfil candidato de transporte: [Nordic UART Service](https://github.com/NordicSemiconductor/Android-BLE-Library)
- O projeto público DT78 usado para comparação é apenas referência de formato/UUID; não prova identidade de chipset nem compatibilidade integral.
