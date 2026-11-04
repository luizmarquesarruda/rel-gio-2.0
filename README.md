# Relógio 2.0

Aplicativo Android para investigar e acompanhar dados de um smartwatch atualmente pareado pelo Wearfit Pro.

## Estado atual

A primeira fase de implementação é uma prova de conceito Android nativa (Kotlin) para BLE/GATT. Ela existe para validar a comunicação no Samsung Galaxy A06 antes de consolidar a arquitetura de produção.

O relógio foi observado no nRF Connect como **GT3 MAX**. A conexão BLE e a descoberta GATT funcionaram. Uma notificação de 8 bytes foi recebida pelo Nordic UART TX. A comparação com uma referência pública DT78/X8 Ultra encontrou UUIDs e uma fixture AB semelhantes; o app agora registra o perfil NUS/AB como **candidato**, sem confirmar chipset ou interpretar outros sensores. A leitura de bateria do quadro `0x91/0x80` ainda é **não validada**.

## Funcionalidades em desenvolvimento nesta fase

- Integração de leitura com Health Connect (Conexão Saúde) para total diário agregado de passos.
- Exibição da hora e data local do Android.
- O Google Fit legado não é integrado diretamente; os dados podem chegar pelo Health Connect se o aplicativo de origem os compartilhar.
- Ajuste do horário do relógio físico e leitura BLE de passos seguem pendentes; a identificação parcial do quadro de bateria não comprova esses comandos.

Detalhes: [Integração Health Connect, passos e horário](docs/INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md).

## O que a primeira versão faz

- Solicita permissões Bluetooth conforme a versão do Android.
- Procura dispositivos BLE próximos.
- Mostra nome e RSSI, ocultando parte do endereço MAC na interface.
- Conecta ao dispositivo escolhido e enumera serviços, características, propriedades e descritores GATT.
- Permite leitura explícita de características que declaram a propriedade Read.
- Permite inscrição explícita em notificações por meio do descritor padrão CCCD (0x2902).
- Registra localmente na tela os bytes recebidos em hexadecimal.
- Reconhece um quadro de bateria `0x91/0x80` como **candidato**, sem confirmar o valor no GT3 Maxx.

## O que ela não faz

- Não escreve dados em características do relógio.
- Não envia comandos pela Nordic UART RX.
- Não toca no Secure DFU/OTA, reset, calibração ou configuração.
- Não interpreta frequência cardíaca, passos, sono, SpO2 ou pressão sem parsers validados.
- Não acessa servidores ou dados privados do Wearfit Pro.
- Não persiste ou transmite dados de saúde.

A inscrição em notificações altera somente a assinatura padrão do descritor CCCD. Não é uma escrita de payload em uma característica proprietária.

## Construção e artefato

O workflow **Android CI** executa testes unitários e gera um APK debug em pull requests para `main` ou por execução manual. O arquivo é publicado como artefato do GitHub Actions por 14 dias; não é uma versão de produção assinada.

Arquivo esperado: `app/build/outputs/apk/debug/app-debug.apk`.

## Como usar a prova de conceito

1. Instale o APK debug no Android.
2. Ative o Bluetooth.
3. Toque em **Procurar relógio** e conceda a permissão de dispositivos próximos.
4. Selecione **GT3 MAX**.
5. Aguarde a descoberta GATT.
6. Use **Ler** somente nas características com Read.
7. Use **Ouvir** somente quando quiser observar notificações; o app não apresenta botão de escrita de característica.

## Documentação

Consulte o [índice completo da documentação](docs/README.md).

- [Mapa de módulos](docs/MAPA-DE-MODULOS.md)
- [Inventário de funções Wearfit Pro](docs/INVENTARIO-FUNCOES-WEARFIT-PRO.md)
- [Integração Android, WhatsApp e notificações](docs/INTEGRACAO-NOTIFICACOES-ANDROID-WHATSAPP.md)
- [Diário de bordo](docs/DIARIO-DE-BORDO.md)
- [Changelog](CHANGELOG.md)
- [Versão](VERSION)
- [Arquitetura](docs/ARQUITETURA.md)
- [Pesquisa Wearfit Pro](docs/PESQUISA-WEARFIT-PRO.md)
- [Identificação do relógio](docs/IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil do GT3 Maxx](docs/PERFIL-GT3-MAXX.md)
- [Captura BLE de 03/11/2026](docs/CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Perfil BLE candidato NUS/AB](docs/PROTOCOLO-BLE-CANDIDATO.md)

## Notificações Android

A captura local foi implementada na branch de desenvolvimento 0.3.0-alpha.1: NotificationListenerService, acesso especial concedido manualmente, filtro por aplicativo, pausa e prévias efêmeras em memória. A implementação aguarda CI e teste físico no Galaxy A06. O conteúdo não é encaminhado ao relógio: o protocolo BLE de notificações do GT3 MAX ainda não foi identificado.

## Versão do aplicativo

Versão de desenvolvimento registrada: `0.3.0-alpha.3`. Não é release estável e aguarda validação do CI e testes físicos.

## Segurança e privacidade

A descoberta BLE não deriva localização física. Não guardar endereço MAC em relatórios públicos. Não assumir que nome comercial identifica chipset ou protocolo. Pressão estimada, SpO2 e outras estimativas de smartwatch não são diagnósticos médicos.
