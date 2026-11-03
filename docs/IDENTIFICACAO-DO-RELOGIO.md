# Identificação do relógio

## Informação necessária

No Wearfit Pro, abrir **Dispositivo** e registrar:
- nome exibido do dispositivo;
- modelo e fabricante, se mostrados;
- versão de firmware e hardware, se disponíveis;
- versão do aplicativo Wearfit Pro;
- recursos habilitados (frequência cardíaca, sono, SpO2, pressão, temperatura etc.).

Se não houver modelo na tela, enviar capturas de:
1. página Dispositivo;
2. Sobre/Informações do dispositivo;
3. página de informações do relógio, se existir.

Não publicar endereço MAC, número de série, identificadores de conta ou dados pessoais em issues públicas.

## Prova de conceito BLE (somente leitura)

No Android:
1. Confirmar permissões Bluetooth adequadas à versão do Android.
2. Fazer scan BLE e registrar nome anunciado, RSSI e tipo de anúncio.
3. Após autorização do usuário, conectar sem enviar comandos proprietários.
4. Enumerar serviços e características GATT, propriedades (read/notify/indicate/write) e descritores.
5. Registrar desconexões e erros sem armazenar identificadores sensíveis em logs públicos.

Ferramenta de inspeção sugerida: nRF Connect for Mobile. Capturas devem ocultar endereços e identificadores pessoais antes de compartilhamento.

## Critérios para avançar

Só criar um driver específico depois de:
- identificar modelo/chipset com evidência;
- observar serviços/características em mais de uma sessão;
- documentar comportamento e formato dos dados;
- comparar leituras com Wearfit Pro;
- definir testes para reconexão e dados inválidos.

**Proibido nesta fase:** escrita em características desconhecidas, reset, alteração de configuração, atualização de firmware ou comandos de calibração.
