# Relógio 2.0

Aplicativo Android de Saúde e Fitness que acompanha dados de um smartwatch pareado pelo Wearfit Pro, organiza métricas autorizadas e oferece uma ponte de leitura de notificações para o dispositivo vestível.

## Finalidade do produto

O Relógio 2.0 é um companheiro de Saúde e Fitness. O fluxo de destino é: sensores do relógio → conexão com o app → dados validados no Conexão Saúde (Health Connect). No sentido inverso, o Android entrega notificações selecionadas ao app para a ponte com o relógio; o app lê os alertas e não responde nem modifica o aplicativo de origem.

A categoria da ficha no Google Play deve ser **Saúde e fitness**, com o recurso declarado como **Atividade e fitness**. Essa classificação é feita no Play Console; o atributo Android `appCategory` não possui um valor `fitness` válido e não deve ser incluído no manifesto. A categoria também não substitui o formulário de apps de saúde, a seção Segurança dos dados ou a política de privacidade.

## Estado atual

A primeira fase de implementação é uma prova de conceito Android nativa (Kotlin) para BLE/GATT. Ela existe para validar a comunicação no Samsung Galaxy A06 antes de consolidar a arquitetura de produção.

O relógio foi observado no nRF Connect como **GT3 MAX**. A conexão BLE e a descoberta GATT funcionaram. Uma notificação de 8 bytes foi recebida pelo Nordic UART TX. A comparação com uma referência pública DT78/X8 Ultra encontrou UUIDs e uma fixture AB semelhantes; o app agora registra o perfil NUS/AB como **candidato**, sem confirmar chipset ou interpretar outros sensores. A leitura de bateria do quadro `0x91/0x80` ainda é **não validada**.

## Funcionalidades em desenvolvimento nesta fase

- Integração de leitura com Health Connect (Conexão Saúde) para total diário agregado de passos.
- Preparação da integração de escrita para publicar no Health Connect apenas amostras reais do smartwatch, após validar o protocolo e solicitar a permissão específica. Ainda não há escrita de passos do GT3 Maxx nesta versão.
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

## Construção e artefatos

O workflow **Android CI** executa testes de política do manifesto, Android Lint, testes unitários e builds `diagnosticDebug` e `companionDebug`. As duas variantes passam por inspeção do manifesto mesclado, verificação da assinatura APK v2 e geração de SHA-256.

Somente o pacote de instalação `relogio-2.0-diagnostic-debug-apk` é publicado. Ele contém:
- `app-diagnostic-debug.apk` — variante sem Notification Listener e sem permissão de rede;
- `SHA256SUMS` — hash do APK para conferir a integridade após baixar;
- `BUILD-INFO.txt` — pacote, versão, variante, commit e limites de uso;
- `SIGNATURE.txt` — resultado e certificado apresentados pelo `apksigner`.

O segundo artefato, `android-validation-reports`, reúne Android Lint, manifestos mesclados, permissões, dependências runtime, informações de assinatura e hashes das duas variantes. É um pacote de relatórios técnicos, não outro aplicativo.

A variante `companionDebug` é compilada, testada e auditada, mas **não publicada para sideload**, pois declara Notification Listener. O encaminhamento real de notificações ao GT3 MAX ainda depende da validação do protocolo.

**Atenção ao Play Protect:** a inspeção binária do artefato diagnostic anterior não encontrou INTERNET, SMS, Accessibility ou Notification Listener. Isso afasta a regra específica de bloqueio de sideload por essas capacidades para aquele APK, mas não comprova ausência de malware nem explica o aviso. O APK é debug, não release. Não desative o Play Protect. Consulte [Play Protect e distribuição segura](docs/ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md).

CI verde não equivale a aprovação do Play Protect nem a teste físico no Galaxy A06. A variante de release e a assinatura própria ainda não foram criadas.

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
- [Matriz de compatibilidade GT3 MAX](docs/MATRIZ-COMPATIBILIDADE-GT3-MAXX.md)
- [Integração Android, WhatsApp e notificações](docs/INTEGRACAO-NOTIFICACOES-ANDROID-WHATSAPP.md)
- [Integração Health Connect, passos, rotas e horário](docs/INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md)
- [Diário de bordo](docs/DIARIO-DE-BORDO.md)
- [Changelog](CHANGELOG.md)
- [Versão](VERSION)
- [Arquitetura](docs/ARQUITETURA.md)
- [Pesquisa Wearfit Pro](docs/PESQUISA-WEARFIT-PRO.md)
- [Identificação do relógio](docs/IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil do GT3 Maxx](docs/PERFIL-GT3-MAXX.md)
- [Captura BLE de 03/11/2026](docs/CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Perfil BLE candidato NUS/AB](docs/PROTOCOLO-BLE-CANDIDATO.md)

## Notificações para o smartwatch

A finalidade é retransmitir notificações selecionadas para o relógio por meio do aplicativo companheiro. O Google lista esse uso como permitido para apps de Saúde e Fitness, desde que seja realmente uma retransmissão para o wearable, com consentimento explícito. A variante companion preserva o NotificationListenerService; o acesso é somente de leitura em relação aos apps de origem. O envio ao GT3 Maxx continua bloqueado até a validação do protocolo de transporte.

A captura local usa NotificationListenerService e acesso especial concedido manualmente. O processamento fica restrito aos pacotes selecionados e requer ativação explícita. As prévias permanecem somente em memória (máximo de 30) e expiram em 15 minutos; o cache é limpo na desconexão/destruição do listener. Nenhum texto de notificação é gravado em arquivos/logs ou enviado à rede. O Play Protect deve continuar ativo; CI aprovado não equivale a aprovação do Google nem a teste físico no Galaxy A06.

## Versão do aplicativo

Versão de desenvolvimento registrada: `0.4.0-alpha.2`. Não é release estável e aguarda validação do CI e testes físicos.

## Segurança e privacidade

A descoberta BLE não deriva localização física. Não guardar endereço MAC em relatórios públicos. Não assumir que nome comercial identifica chipset ou protocolo. Pressão estimada, SpO2 e outras estimativas de smartwatch não são diagnósticos médicos.
