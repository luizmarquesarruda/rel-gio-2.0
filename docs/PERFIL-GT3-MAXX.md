# Perfil do dispositivo: GT3 Maxx

## Identificação informada pelo usuário

- Nome comercial: GT3 Maxx
- Categoria: smartwatch genérico chinês
- Aplicativo companheiro: Wearfit Pro
- Telefone de desenvolvimento: Samsung Galaxy A06, Android 16
- Estado: pareamento pelo Wearfit Pro informado pelo usuário; protocolo e chipset ainda não identificados.

## Hipóteses que NÃO devem ser tratadas como fatos

O nome GT3 Maxx não identifica de forma inequívoca fabricante, SoC, firmware ou protocolo BLE. Não assumir compatibilidade com FitPro, DaFit, GloryFit ou qualquer protocolo publicado apenas por semelhança visual/comercial.

## Plano de identificação

1. Capturar nome anunciado pelo BLE, fabricante (Manufacturer Data), service UUIDs e RSSI.
2. Inspecionar serviços e características GATT com nRF Connect, sem escrever em nenhuma característica.
3. Comparar UUIDs e propriedades com referências públicas apenas como pistas.
4. Fazer captura repetida em duas sessões e registrar diferenças.
5. Comparar dados exibidos pelo Wearfit Pro com qualquer notificação/leitura observada.
6. Determinar se o relógio anuncia separadamente Bluetooth Classic para chamadas; esse canal não deve ser confundido com BLE de sensores.

## Segurança

- Não executar OTA, DFU, reset, calibração ou comandos de configuração.
- Não enviar bytes para características write/write-no-response.
- Não publicar MAC, serial, identificadores pessoais ou capturas com dados privados.
- Frequência cardíaca, SpO2 e pressão estimada não devem ser apresentados como diagnóstico médico. Em particular, pressão estimada por relógio não substitui aparelho de braço validado.

## Próxima evidência necessária

Captura de tela do nRF Connect após scan, mostrando o dispositivo GT3 Maxx e, após conexão, a lista de serviços GATT. Ocultar endereço MAC antes de compartilhar. Se o Wearfit Pro mostrar versão de firmware/hardware, registrar também.
