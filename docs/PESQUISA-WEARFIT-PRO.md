# Pesquisa: Wearfit Pro

## Fontes consultadas

1. Declaração de privacidade: https://wearfit.app/statement-on-wearfit-pro-and-privacy/
2. Contrato de uso: https://wearfit.app/wearfit-pro-user-agreement/
3. Referência independente OpenWearOTA (FitPro, não Wearfit Pro): https://github.com/HackSucks/OpenWearOTA/blob/main/DOCUMENTATION.md

## O que está documentado

A declaração do Wearfit Pro descreve:
- gerenciamento de dispositivos e conexão Bluetooth;
- identificador aleatório, nome, informações de hardware e estado do dispositivo;
- possibilidade de aplicações de terceiros interagirem com o dispositivo pareado pelo Wearfit Pro;
- acesso a dados de sensores, sono, tipo de exercício, estado de uso, logs e localização mediante autorização expressa.

## O que NÃO está comprovado

- disponibilidade pública de SDK para desenvolvedores independentes;
- nome, versão, artefato Maven ou documentação de API de integração;
- compatibilidade dessa integração com o relógio específico do usuário;
- protocolo BLE, UUIDs, comandos ou formato de pacotes do dispositivo.

A política de privacidade não é documentação de SDK. Não presumir que o aplicativo possa consultar o banco interno do Wearfit Pro, reutilizar sessão de conta ou acessar endpoints privados.

## Caminhos a investigar

1. Contato com o desenvolvedor Wakeup/Weike para solicitar SDK/API e termos de integração.
2. Verificar se o Wearfit Pro oferece exportação/compartilhamento de dados na versão instalada.
3. Identificar o relógio e investigar BLE GATT com ferramenta de inspeção, sem escrita.
4. Avaliar Android Health Connect apenas como fonte complementar, se o Wearfit Pro realmente exportar/sincronizar dados compatíveis.

## Referência OpenWearOTA

O projeto OpenWearOTA descreve protocolos de produtos FitPro e é material de engenharia reversa independente. Não deve ser tratado como protocolo Wearfit Pro nem aplicado sem identificar chipset e validar capturas. Qualquer operação OTA/firmware está fora do escopo.
