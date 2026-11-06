# Identificação do relógio GT3 MAX

## Identificação diretamente no dispositivo

Registrar, quando disponíveis, nas telas nativas **Configurações → Sobre** ou **Informações do dispositivo** do próprio relógio:

- nome e modelo exibidos;
- fabricante, versão de firmware e versão de hardware;
- identificadores regulatórios impressos na caixa/etiqueta, se houver.

Não publicar endereço MAC, número de série, códigos de pareamento ou identificadores pessoais.

## Inspeção BLE/GATT independente

No Android, usando o Relógio 2.0 ou uma ferramenta de inspeção BLE:

1. Confirmar as permissões Bluetooth adequadas à versão do Android.
2. Fazer scan BLE e registrar nome anunciado, RSSI e tipo de anúncio.
3. Conectar somente após ação explícita do usuário e sem enviar comandos proprietários.
4. Enumerar serviços e características GATT, propriedades (READ/NOTIFY/INDICATE/WRITE) e descritores.
5. Registrar desconexões e erros sem armazenar identificadores sensíveis em logs públicos.
6. Repetir a captura em sessões independentes para separar comportamento estável de observações ocasionais.

Ferramenta de inspeção sugerida: nRF Connect for Mobile. Capturas devem ocultar endereços e identificadores pessoais antes de compartilhamento.

## Critérios para identificar o protocolo

Só criar ou ampliar um driver específico depois de:

- identificar modelo/chipset com evidência independente;
- observar serviços/características em mais de uma sessão;
- documentar comportamento e formato dos dados;
- confirmar unidades, faixas, timestamps e semântica por evidência repetível;
- definir testes para reconexão, dados inválidos e callbacks atrasados.

**Proibido:** escrita em características desconhecidas, reset, alteração de configuração, atualização de firmware ou comandos de calibração.
