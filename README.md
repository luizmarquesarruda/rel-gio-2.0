# Relógio 2.0

Aplicativo Android de Saúde e Fitness que se conecta diretamente ao GT3 MAX por BLE/GATT, consulta passos agregados no Health Connect e oferece captura consentida de notificações na variante companion. O Wearfit Pro permanece uma referência/companheiro externo; o app não depende de API ou nuvem privada dele.

## Reconexão automática BLE

Ao abrir o aplicativo, uma busca BLE geral começa automaticamente; não é necessário digitar o endereço do relógio. O endereço salvo é uma pista, não um filtro exclusivo. O app também reconhece nomes GT3 MAX e anúncios NUS; quando nome/endereço não estão disponíveis, lista dispositivos próximos para validação GATT. Uma nova identidade só é salva automaticamente após o fingerprint GATT completo do GT3 MAX Call. Depois da primeira conexão validada, a reconexão automática usa varreduras de baixa potência e intervalos de 1, 2, 5, 10, 20 e até 30 segundos. A ação **Parar reconexão** cancela as tentativas e desativa a preferência local.

A recuperação nesta fase ocorre enquanto o app está em primeiro plano ou quando ele é reaberto/retomado. O Android pode suspender o processo em segundo plano ou após encerramento forçado; recuperação contínua em segundo plano ainda não está implementada. A reconexão restaura o transporte GATT e reinscreve apenas a notificação NUS TX já permitida; não valida sensores e não envia payload ao relógio.

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
- Busca dispositivos BLE automaticamente sem exigir MAC; tenta candidatos por nome GT3 MAX, serviço NUS anunciado ou endereço local salvo e só aceita identidade nova após fingerprint GATT completo.
- Exibe inventário GATT passivo: serviços, características, propriedades e descritores.
- Após a conexão, lê automaticamente apenas metadados básicos e bateria quando perfis Bluetooth SIG compatíveis anunciam READ.
- Permite ativar uma única vez os **testes automáticos GATT**. Depois do consentimento, ao reconhecer o fingerprint completo do GT3 MAX Call, o app percorre sozinho todos os canais compatíveis READ e NOTIFY/INDICATE com CCCD padrão; a exploração manual continua disponível.
- Inscreve automaticamente somente NUS TX no GT3 MAX Call quando nome e fingerprint GATT completo coincidem; NUS RX continua sem escrita.
- Registra leituras, status de callbacks, inscrições confirmadas/falhas e valores Bluetooth SIG decodificados. HEX bruto é guardado na biblioteca privada somente durante captura consentida; o catálogo de inventário conserva metadados.
- Reagrupa quadros AB fragmentados e separa quadros concatenados sem inventar semântica para comandos desconhecidos.
- Reconhece um quadro de bateria `0x91/0x80` como **candidato**, sem confirmar o valor no GT3 Maxx.

## O que ela não faz

- Não envia payloads a características proprietárias do relógio.
- Não envia comandos pela Nordic UART RX.
- Nenhuma escrita em característica é executada nesta fase. A única operação de escrita GATT é a configuração do descritor CCCD padrão para ativar uma notificação/indicação permitida; a sincronização CTS está suspensa.
- Não toca no Secure DFU/OTA, reset, calibração ou configuração.
- Não atribui semântica a canais proprietários desconhecidos. Há decoders somente para formatos Bluetooth SIG identificados; passos, sono e métricas privadas continuam não interpretados até obter protocolo documentado.
- Não acessa servidores ou dados privados do Wearfit Pro.
- Não transmite dados de saúde à rede nem os publica no Health Connect. A captura local de leituras/notifications e HEX exige ativação explícita e permanece no armazenamento privado do app.

A inscrição em notificações altera somente a assinatura padrão do descritor CCCD. Não é uma escrita de payload em uma característica proprietária.

## Construção e artefatos

O workflow **Android CI** executa testes de política do manifesto, Android Lint, testes unitários e builds `diagnosticDebug` e `companionDebug`. As duas variantes passam por inspeção do manifesto mesclado, verificação da assinatura APK v2 e geração de SHA-256.

A CI publica **dois artefatos separados, com finalidades diferentes**:

- `relogio-2.0-diagnostic-debug-apk`: contém **somente** o arquivo instalável `app-diagnostic-debug.apk`. Não inclui arquivos TXT, XML, relatórios, certificados ou checksums.
- `android-validation-reports`: contém relatórios Android Lint, manifestos mesclados, permissões, badging, resultados/certificados de assinatura, hashes SHA-256 e `diagnostic-build-info.txt`. Não contém APK instalável.

A variante `companionDebug` é compilada, testada e auditada, mas **não publicada para sideload**, pois declara Notification Listener. O encaminhamento real de notificações ao GT3 MAX ainda depende da validação do protocolo.

**Atenção ao Play Protect:** a inspeção binária do artefato diagnostic anterior não encontrou INTERNET, SMS, Accessibility ou Notification Listener. Isso afasta a regra específica de bloqueio de sideload por essas capacidades para aquele APK, mas não comprova ausência de malware nem explica o aviso. O APK é debug, não release. Não desative o Play Protect. Consulte [Play Protect e distribuição segura](docs/ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md).

CI verde não equivale a aprovação do Play Protect nem a teste físico no Galaxy A06. A variante de release e a assinatura própria ainda não foram criadas.

## Como usar a prova de conceito

1. Instale o APK diagnostic debug no Android e ative o Bluetooth.
2. Conceda a permissão de dispositivos próximos quando o Android solicitar.
3. Ao abrir o aplicativo, a busca BLE geral começa automaticamente; não é necessário digitar o endereço MAC.
4. Se quiser iniciar outra varredura manualmente, toque em **Buscar e conectar automaticamente**; o endereço Bluetooth configurado localmente é apenas uma pista adicional, não um requisito.
4. O app tenta validar candidatos com nome GT3 MAX, serviço NUS anunciado ou endereço salvo. Dispositivos sem nome aparecem na lista; ao selecioná-los, o fingerprint GATT é verificado antes da exploração.
5. A identidade é salva automaticamente somente após correspondência do fingerprint GATT completo. O endereço manual permanece opcional.
6. Após a descoberta, o inventário GATT é passivo. A inscrição NUS TX ocorre automaticamente apenas quando nome e fingerprint completo do perfil GT3 MAX Call coincidem.
7. Na tela principal, toque uma vez em **Ativar testes automáticos ao conectar** e confirme o aviso de coleta local. A autorização fica salva até você pausar. O app inicia a exploração aprofundada automaticamente nesta conexão e nas próximas conexões com fingerprint completo.
8. Os valores recebidos aparecem na biblioteca local e no relatório TXT. A captura é renovada em blocos de até 10 minutos enquanto o modo estiver ativo; pausar encerra a sessão e desativa inscrições automáticas via CCCD.

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

A versão de desenvolvimento canônica está no arquivo `VERSION`. Os metadados do build ficam no artefato `android-validation-reports`, no arquivo `diagnostic-build-info.txt`; o artefato instalável contém somente o APK. Builds debug não são releases de produção.

## Segurança e privacidade

A descoberta BLE não deriva localização física. Não guardar endereço MAC em relatórios públicos. Não assumir que nome comercial identifica chipset ou protocolo. Pressão estimada, SpO2 e outras estimativas de smartwatch não são diagnósticos médicos.
