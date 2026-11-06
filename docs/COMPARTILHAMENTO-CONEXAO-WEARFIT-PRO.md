# Liberação da conexão BLE para o Wearfit Pro

**Data:** 04/11/2026  
**Dispositivo de referência:** GT3 MAX Call  
**Estado:** implementação Android em CI; teste físico pendente.

## Objetivo

Permitir que o usuário escolha qual aplicativo controla a conexão BLE com o relógio. O Wearfit Pro deve conseguir conectar diretamente ao GT3 MAX sem o Relógio 2.0 iniciar reconexões em segundo plano/ao retomar a tela.

## Comportamento implementado

- A ação **Liberar conexão para Wearfit Pro** persiste a intenção de entregar a conexão ao aplicativo original.
- O Relógio 2.0 interrompe qualquer scan próprio, cancela tentativas agendadas, desativa a política de reconexão automática e encerra a sessão GATT que ele próprio abriu.
- A intenção de liberação sobrevive ao fechamento/reabertura do Relógio 2.0; ao voltar ao primeiro plano, o app não inicia scan BLE nem reconecta automaticamente.
- A ação muda para **Retomar conexão no Relógio 2.0**. Somente ao tocar nela o app remove a liberação e inicia uma busca BLE manual.
- O estado é local ao aparelho e não envia dados ou comandos ao relógio.

## Limites importantes

- A liberação encerra somente a conexão GATT gerenciada pelo Relógio 2.0. Ela não remove o pareamento Android, não desliga o Bluetooth e não altera o perfil Bluetooth Classic de chamadas/áudio.
- O usuário deve abrir o Wearfit Pro e conectar o GT3 MAX por dentro dele. Se o Wearfit Pro continuar sem conectar mesmo com o Relógio 2.0 liberado, a causa não está demonstrada como disputa de conexão pelo nosso app.
- Nenhum payload proprietário, NUS RX, DFU/OTA, reset ou calibração é enviado. Não foi adicionada escrita GATT.
- Não é possível confirmar por código que o Wearfit Pro sincronizou hora ou dados; isso exige teste físico no Galaxy A06 com o relógio.

## Roteiro de validação física

1. No Relógio 2.0, tocar **Liberar conexão para Wearfit Pro**.
2. Confirmar a indicação **Conexão liberada para Wearfit Pro**.
3. Fechar e reabrir o Relógio 2.0; confirmar que ele não inicia scan nem reconecta.
4. Abrir o Wearfit Pro e conectar o GT3 MAX diretamente pelo app.
5. Confirmar se a sincronização de hora/data e demais dados volta a ocorrer.
6. Para devolver o relógio ao Relógio 2.0, tocar **Retomar conexão no Relógio 2.0** e iniciar a conexão manual.

**CI:** aguardando execução desta branch.  
**Teste físico Galaxy A06 / Android 16 + GT3 MAX / Wearfit Pro:** não validado.
