# Relógio 2.0

## Organização do código

A implementação permanece em um único módulo Android nativo `:app`, em Kotlin e Views. As Activities ficam em `ui/screens`, utilitários de apresentação em `ui/formatters`, contratos puros em `domain`, BLE em `device/ble`, perfil/protocolo GT3 MAX em `device/gt3max/protocol`, persistência e integrações em `data`. A estrutura foi organizada sem alterar o comportamento GATT. A extração do ciclo BLE de `MainActivity` continua como etapa arquitetural pendente. Consulte [estrutura de pacotes](docs/architecture/ESTRUTURA-DE-PACOTES.md) e [mapa de módulos](docs/MAPA-DE-MODULOS.md).

Aplicativo Android de Saúde e Fitness que se conecta diretamente ao GT3 MAX por BLE/GATT, consulta passos agregados no Health Connect e oferece captura consentida de notificações na variante companion. A conexão BLE/GATT é gerenciada diretamente pelo Relógio 2.0. O aplicativo não oferece handoff nem integração operacional com Wearfit Pro; a documentação histórica sobre ele serve apenas como referência de pesquisa de protocolo.

## Reconexão automática BLE

Ao abrir o aplicativo, uma busca BLE geral começa automaticamente; não é necessário digitar o endereço do relógio. O endereço salvo é uma pista, não um filtro exclusivo. O app também reconhece nomes GT3 MAX e anúncios NUS; quando nome/endereço não estão disponíveis, lista dispositivos próximos para validação GATT. Uma nova identidade só é salva automaticamente após o fingerprint GATT completo do GT3 MAX Call. Depois da primeira conexão validada, a reconexão automática usa varreduras de baixa potência e intervalos de 1, 2, 5, 10, 20 e até 30 segundos. A ação **Parar reconexão** cancela as tentativas e desativa a preferência local.

A recuperação nesta fase ocorre enquanto o app está em primeiro plano ou quando ele é reaberto/retomado. O Android pode suspender o processo em segundo plano ou após encerramento forçado; recuperação contínua em segundo plano ainda não está implementada. A reconexão restaura o transporte GATT e reinscreve apenas a notificação NUS TX já permitida; não valida sensores e não envia payload ao relógio.

## Recuperação da tabela GATT

Se o Android sinalizar **Service Changed**, o aplicativo invalida a fila/operações e solicita uma nova descoberta dos serviços na mesma sessão GATT. Respostas de READ, WRITE e CCCD só alteram o estado quando correspondem à conexão atual e à operação efetivamente em andamento; callbacks atrasados, de outra conexão ou de operações invalidadas são descartados. Uma operação que expirou ou foi interrompida por Service Changed não é repetida na mesma conexão GATT; uma nova conexão permite tentar novamente. Um canal silencioso ou sem callback não é considerado sensor ausente.

## Controle direto da conexão BLE

O Relógio 2.0 é o único controlador BLE previsto pelo aplicativo. A opção de liberar a sessão para Wearfit Pro foi removida, e a preferência antiga de handoff é apagada na inicialização. Ao iniciar uma conexão, o app habilita recuperação automática com intervalos progressivos de 1, 2, 5, 10, 20 e até 30 segundos. A ação **Desconectar / Parar reconexão** continua sendo a decisão explícita para interromper o controle e as tentativas.

O app não tem permissão para desativar ou encerrar outro aplicativo Android. Se Wearfit Pro ainda estiver conectado ao relógio, encerre a conexão ou force a parada dele manualmente nas configurações do Android antes de conectar pelo Relógio 2.0. O comportamento de reconexão contínua permanece limitado ao app em primeiro plano ou retomado; serviço BLE permanente em segundo plano ainda não está implementado.

## Finalidade do produto

O Relógio 2.0 é um companheiro de Saúde e Fitness. O fluxo de destino é: sensores do relógio → conexão com o app → dados validados no Conexão Saúde (Health Connect). No sentido inverso, o Android entrega notificações selecionadas ao app para a ponte com o relógio; o app lê os alertas e não responde nem modifica o aplicativo de origem.

A categoria da ficha no Google Play deve ser **Saúde e fitness**, com o recurso declarado como **Atividade e fitness**. Essa classificação é feita no Play Console; o atributo Android `appCategory` não possui um valor `fitness` válido e não deve ser incluído no manifesto. A categoria também não substitui o formulário de apps de saúde, a seção Segurança dos dados ou a política de privacidade.

## Estado atual

A primeira fase de implementação é uma prova de conceito Android nativa (Kotlin) para BLE/GATT. Ela existe para validar a comunicação no Samsung Galaxy A06 antes de consolidar a arquitetura de produção.

O relógio foi observado no nRF Connect como **GT3 MAX**. A conexão BLE e a descoberta GATT funcionaram. Uma notificação de 8 bytes foi recebida pelo Nordic UART TX. A comparação com uma referência pública DT78/X8 Ultra encontrou UUIDs e uma fixture AB semelhantes; o app agora registra o perfil NUS/AB como **candidato**, sem confirmar chipset ou interpretar outros sensores. A leitura de bateria do quadro `0x91/0x80` ainda é **não validada**.

## Funcionalidades em desenvolvimento nesta fase

- Integração de leitura com Health Connect (Conexão Saúde) para total diário agregado de passos.
- Preparação da integração de escrita para publicar no Health Connect apenas amostras reais do smartwatch, após validar o protocolo e solicitar a permissão específica. Ainda não há escrita de passos do GT3 Maxx nesta versão.
- Hora local do Android, com indicação das opções de data/hora automática e fuso automático do sistema.
- Consulta de clima pela rede móvel ou Wi-Fi via Open-Meteo, com pesquisa manual de cidade, cache privado, previsão atual/diária e atualização periódica aproximada.
- A sincronização física de horário é automática somente quando o GATT anuncia o serviço padrão CTS 0x1805/0x2A2B com WRITE. O GT3 MAXX capturado não anunciou CTS; seu RTC/visor ainda não foi sincronizado de forma comprovada.
- A previsão meteorológica ainda não é transmitida ao GT3 MAXX: o protocolo de clima deste firmware não foi identificado. Nenhum comando proprietário é enviado.
- O Google Fit legado não é integrado diretamente; os dados podem chegar pelo Health Connect se o aplicativo de origem os compartilhar.

Detalhes: [Integração Health Connect, passos e horário](docs/INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md) e [integração de hora e clima pela rede](docs/INTEGRACAO-HORA-CLIMA-REDE.md).

## O que a primeira versão faz

- Solicita permissões Bluetooth conforme a versão do Android.
- Procura dispositivos BLE próximos.
- Mostra nome e RSSI, ocultando parte do endereço MAC na interface.
- Busca dispositivos BLE automaticamente sem exigir MAC; tenta candidatos por nome GT3 MAX, serviço NUS anunciado ou endereço local salvo e só aceita identidade nova após fingerprint GATT completo.
- Exibe inventário GATT passivo: serviços, características, propriedades e descritores.
- Após a conexão, lê automaticamente apenas metadados básicos e bateria quando perfis Bluetooth SIG compatíveis anunciam READ.
- Permite ativar uma única vez os **testes automáticos GATT**. Depois do consentimento, ao reconhecer o fingerprint completo do GT3 MAX Call, o app percorre sozinho todos os canais compatíveis READ e NOTIFY/INDICATE com CCCD padrão; a exploração manual continua disponível.
- Inscreve automaticamente somente NUS TX no GT3 MAX Call quando nome e fingerprint GATT completo coincidem; NUS RX continua sem escrita.
- Registra leituras, status de callbacks, inscrições confirmadas/falhas e valores Bluetooth SIG decodificados (frequência cardíaca, pressão, oximetria, termômetro, ambiente, corrida/cadência, ciclismo e capacidades de composição corporal quando anunciados). HEX bruto é guardado na biblioteca privada somente durante captura consentida; o catálogo de inventário conserva metadados.
- A tela Testes e descobertas mostra se os testes automáticos estão ativos; a biblioteca detalha UUID de serviço/característica e os resultados preservados.
- O painel principal acompanha o progresso READ/CCCD por canal, distinguindo respostas, inscrições aguardando dados, notificações, decodificações, falhas e timeouts. Um marcador temporal permite registrar ações feitas fisicamente no relógio e compará-las com os eventos BLE no TXT.
- Reagrupa quadros AB fragmentados e separa quadros concatenados sem inventar semântica para comandos desconhecidos.
- Reconhece um quadro de bateria `0x91/0x80` como **candidato**, sem confirmar o valor no GT3 Maxx.

## O que ela não faz

- Não envia payloads a características proprietárias do relógio.
- Não envia comandos pela Nordic UART RX.
- Não são enviados payloads a características proprietárias. A única escrita em característica de dados permitida é a sincronização manual pelo Bluetooth SIG CTS (0x1805/0x2A2B), habilitada somente quando o relógio anuncia a característica com propriedade WRITE e após toque explícito do usuário. O callback GATT não comprova atualização do RTC/mostrador. A configuração do descritor CCCD padrão continua limitada à ativação de notificações/indicações permitidas.
- Não toca no Secure DFU/OTA, reset, calibração ou configuração.
- Não atribui semântica a canais proprietários desconhecidos. Há decoders somente para formatos Bluetooth SIG identificados; passos e sono proprietários continuam não interpretados. Body Composition Measurement 0x2A9C também permanece bruto até completar a leitura contextual de flags, unidades, resoluções e pacotes múltiplos.
- Não acessa servidores ou dados privados do Wearfit Pro.
- Não transmite dados de saúde à rede nem os publica no Health Connect. A captura local de leituras/notifications e HEX exige ativação explícita e permanece no armazenamento privado do app.

A inscrição em notificações altera somente a assinatura padrão do descritor CCCD. Não é uma escrita de payload em uma característica proprietária.

## Construção e artefatos

O workflow **Android CI** executa testes de política do manifesto, Android Lint, testes unitários e builds `diagnosticDebug` e `companionDebug`. As duas variantes passam por inspeção do manifesto mesclado, verificação da assinatura APK v2 e geração de SHA-256.

A CI publica **dois artefatos separados, com finalidades diferentes**:

- `relogio-2.0-diagnostic-debug-apk`: contém **somente** o arquivo instalável `app-diagnostic-debug.apk`. Não inclui arquivos TXT, XML, relatórios, certificados ou checksums.
- `android-validation-reports`: contém relatórios Android Lint, manifestos mesclados, permissões, badging, resultados/certificados de assinatura, hashes SHA-256 e `diagnostic-build-info.txt`. Não contém APK instalável.

A variante `companionDebug` é compilada, testada e auditada, mas **não publicada para sideload**, pois declara Notification Listener. O encaminhamento real de notificações ao GT3 MAX ainda depende da validação do protocolo.

**Atenção ao Play Protect:** a inspeção binária citada foi feita em um artefato diagnostic anterior à integração meteorológica. A nova versão declara INTERNET e ACCESS_NETWORK_STATE somente para geocodificação e previsão Open-Meteo; o manifesto e os guardrails foram atualizados, mas a nova compilação ainda precisa ser auditada. Isso não explica nem garante a remoção de alertas do Play Protect. O APK é debug, não release. Não desative o Play Protect. Consulte [Play Protect e distribuição segura](docs/ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md).

CI verde não equivale a aprovação do Play Protect nem a teste físico no Galaxy A06. A variante de release e a assinatura própria ainda não foram criadas.

## Como usar a prova de conceito

1. Instale o APK diagnostic debug no Android e ative o Bluetooth.
2. Conceda a permissão de dispositivos próximos quando o Android solicitar.
3. Ao abrir o aplicativo, a busca BLE geral começa automaticamente; não é necessário digitar o endereço MAC.
4. Para iniciar outra varredura, toque em **Buscar e conectar automaticamente**. O endereço MAC é opcional.
5. O app tenta validar candidatos por nome GT3 MAX, serviço NUS anunciado ou endereço salvo. Dispositivos sem nome aparecem na lista; ao selecioná-los, o fingerprint GATT é verificado antes da exploração.
6. A identidade é salva automaticamente somente após correspondência do fingerprint GATT completo. O endereço manual permanece opcional.
7. Após a descoberta, o NUS TX é inscrito somente quando o fingerprint completo do perfil GT3 MAX Call coincide.
8. Na tela principal, toque uma vez em **Ativar testes automáticos ao conectar** e confirme a coleta local. A autorização fica salva até você pausar. A exploração aprofundada inicia nesta conexão e nas próximas conexões com fingerprint completo.
9. Os valores recebidos aparecem na biblioteca local e no relatório TXT. As janelas de captura são renovadas em blocos de até 10 minutos enquanto o app está em primeiro plano, conectado e com o modo ativo. Ao sair da tela, a renovação fica suspensa e retoma ao voltar; pausar encerra a captura e agenda a desinscrição dos canais automáticos via CCCD padrão.

## Documentação

Consulte o [índice completo da documentação](docs/README.md).

- [Mapa de módulos](docs/MAPA-DE-MODULOS.md)
- [Inventário de funções Wearfit Pro](docs/INVENTARIO-FUNCOES-WEARFIT-PRO.md)
- [Matriz de compatibilidade GT3 MAX](docs/MATRIZ-COMPATIBILIDADE-GT3-MAXX.md)
- [Integração Android, WhatsApp e notificações](docs/INTEGRACAO-NOTIFICACOES-ANDROID-WHATSAPP.md)
- [Integração Health Connect, passos, rotas e horário](docs/INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md)
- [Integração de hora e clima pela rede do celular](docs/INTEGRACAO-HORA-CLIMA-REDE.md)
- [Diário de bordo](docs/DIARIO-DE-BORDO.md)
- [Changelog](CHANGELOG.md)
- [Versão](VERSION)
- [Arquitetura](docs/ARQUITETURA.md)
- [Pesquisa Wearfit Pro](docs/PESQUISA-WEARFIT-PRO.md)
- [Identificação do relógio](docs/IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil do GT3 Maxx](docs/PERFIL-GT3-MAXX.md)
- [Estudo técnico do firmware GT3 MAX Call](docs/ESTUDO-FIRMWARE-GT3-MAXX.md)
- [Captura BLE de 03/11/2026](docs/CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Perfil BLE candidato NUS/AB](docs/PROTOCOLO-BLE-CANDIDATO.md)

## Retenção da biblioteca BLE

Leituras e HEX bruto só são capturados após consentimento e durante uma sessão cronometrada. Depois de capturados, os registros permanecem no arquivo privado local até exclusão individual pela Biblioteca interna, remoção pela política de limite (máximo de 1.000 registros) ou remoção dos dados do aplicativo no Android. A exclusão individual solicita confirmação e grava o novo snapshot de forma atômica.

## Notificações para o smartwatch

A finalidade é retransmitir notificações selecionadas para o relógio por meio do aplicativo companheiro. O Google lista esse uso como permitido para apps de Saúde e Fitness, desde que seja realmente uma retransmissão para o wearable, com consentimento explícito. A variante companion preserva o NotificationListenerService; o acesso é somente de leitura em relação aos apps de origem. O envio ao GT3 Maxx continua bloqueado até a validação do protocolo de transporte.

A captura local usa NotificationListenerService e acesso especial concedido manualmente. O processamento fica restrito aos pacotes selecionados e requer ativação explícita. As prévias permanecem somente em memória (máximo de 30) e expiram em 15 minutos; o cache é limpo na desconexão/destruição do listener. Nenhum texto de notificação é gravado em arquivos/logs ou enviado à rede. O Play Protect deve continuar ativo; CI aprovado não equivale a aprovação do Google nem a teste físico no Galaxy A06.

## Versão do aplicativo

A versão de desenvolvimento canônica está no arquivo `VERSION`. Os metadados do build ficam no artefato `android-validation-reports`, no arquivo `diagnostic-build-info.txt`; o artefato instalável contém somente o APK. Builds debug não são releases de produção.

## Segurança e privacidade

A descoberta BLE não deriva localização física. Não guardar endereço MAC em relatórios públicos. Não assumir que nome comercial identifica chipset ou protocolo. Pressão estimada, SpO2 e outras estimativas de smartwatch não são diagnósticos médicos.
