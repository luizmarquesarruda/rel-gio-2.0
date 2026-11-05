## 04/11/2026 — Controle de privacidade do histórico BLE

- A biblioteca interna ganhou a ação **Excluir registro** em cada item, com confirmação antes de remover metadados e, quando presente, HEX capturado.
- A exclusão ocorre no executor serial de persistência e grava um novo snapshot via AtomicFile; a UI não faz I/O de arquivo no thread principal.
- A remoção atinge somente o ID selecionado, preserva os demais registros e mantém o estado da sessão ativa.
- Testes unitários foram adicionados para remoção exata, preservação da sessão e ID inexistente.
- Execução dos testes, lint, build, auditoria de APK e teste físico: **não validados**. CI não disparada conforme solicitação.
## 04/11/2026 — Liberação da conexão BLE para Wearfit Pro

- Adicionada ação explícita na tela principal para entregar o controle da sessão ao Wearfit Pro.
- O Relógio 2.0 interrompe scans próprios, cancela tentativas pendentes, desativa a reconexão automática e fecha a sessão GATT gerenciada pelo próprio app.
- A intenção de liberação fica persistida; reabrir ou retomar o Relógio 2.0 não inicia busca BLE enquanto o modo estiver ativo.
- A retomada exige ação explícita **Retomar conexão no Relógio 2.0**, que remove a liberação e inicia nova busca manual.
- A ação não remove pareamento Android, não desliga Bluetooth e não modifica o canal Bluetooth Classic de chamadas.
- Testes unitários cobrem o bloqueio de reconexão e da descoberta inicial durante a liberação.
- CI desta branch e teste físico Galaxy A06 / Android 16 + GT3 MAX / Wearfit Pro: **não validados**.

## 04/11/2026 — Watchdog e rastreabilidade do ciclo de descoberta GATT

- A conexão GATT agora registra persistentemente quando `discoverServices()` foi aceito, quando `onServicesDiscovered` respondeu e quando a descoberta falhou.
- Adicionado watchdog de 12 segundos para a ausência do callback. Se a API recusar o início, retornar erro ou não responder dentro do prazo, a sessão GATT é encerrada de forma controlada e a política de reconexão pode tentar novamente.
- O inventário anterior permanece preservado, mas o TXT passa a registrar explicitamente que ele não representa a conexão atual enquanto o callback estiver pendente ou tiver falhado.
- Operações pendentes são classificadas como não executadas; filas, inscrições pendentes e timeouts são limpos antes da reconexão.
- Se o usuário desconectar ou o aplicativo for encerrado antes do callback, a biblioteca grava `INTERRUPTIDA`, evitando que a tentativa pareça uma descoberta concluída.
- Teste unitário cobre a decisão de recuperação e impede que callback já recebido ou conexão obsoleta disparem recuperação.
- Fases de exploração serializada, decodificação Bluetooth SIG, persistência atômica e painel de progresso permanecem integradas; nenhuma semântica proprietária foi presumida.
- CI após esta alteração e validação física no Samsung Galaxy A06 / Android 16 com GT3 MAX: **não validados**.

## 04/11/2026 — Ocultação do prefixo HEX fora da captura consentida

- Auditoria de privacidade identificou que o log do fingerprint BLE exibia os quatro primeiros bytes do payload mesmo sem sessão de captura ativa.
- Prefixo HEX agora é registrado somente durante captura consentida; fora dela, o log informa que o prefixo foi ocultado e mantém tamanho, SHA-256 truncado e contagem de repetição.
- O armazenamento de payload completo continua vinculado à sessão autorizada. Nenhum comando BLE adicional foi introduzido.
- CI e teste físico GT3 MAX/Galaxy A06: **não validados nesta alteração**.

## 04/11/2026 — Métricas do painel sem confundir operações puladas

- Ajustado o resumo GATT: itens não executados após desconexão/pausa deixam de ser somados às operações concluídas.
- O percentual indica operações tratadas, enquanto os contadores separam finalizadas e não executadas. Falhas/timeouts continuam visíveis como resultados, não como sucesso.
- Testes garantem que desconectar com uma operação na fila gera 0 concluídas, 1 não executada e progresso de tratamento de 100%, sem alegar sucesso do sensor.

## 04/11/2026 — Eliminação de leitura GATT redundante

- Auditoria identificou que a bateria 0x2A19 já era lida pelo plano básico de conexão e podia entrar novamente como READ no plano aprofundado.
- O plano aprofundado agora evita repetir esse READ e mantém apenas a inscrição opcional NOTIFY/INDICATE quando a propriedade e o CCCD são anunciados.
- Removida duplicação na tabela interna de características Body Composition Feature 0x2A9B.
- Operações READ que forem interrompidas antes do início são marcadas como não executadas no painel, sem serem contadas como falha do sensor.
- Teste unitário garante que a exploração aprofunda a bateria sem duplicar a leitura básica.
- CI e teste físico no GT3 MAX/Galaxy A06: **não validados nesta alteração**.

## 04/11/2026 — Progresso GATT e marcadores temporais

- Adicionado rastreador em memória por operação READ/CCCD para apresentar total planejado, concluído, respondido, inscrito, notificações observadas, valores decodificados, falhas, timeouts e itens na fila.
- Cada canal recebe classificação baseada apenas em evidências de transporte. CCCD confirmado sem notificação é exibido como "inscrito, aguardando dados"; não é interpretado como sensor ausente.
- Operações que ainda estavam na fila/em execução no momento da desconexão são marcadas como não executadas.
- Adicionado marcador temporal manual para registrar ações feitas fisicamente no relógio. Eventos são exportados no TXT com fonte distinta e não são confundidos com medições BLE.
- Testes unitários cobrem resumo de progresso, estados de canal silencioso/time-out/falha e interrupção da exploração.
- Não foram adicionadas escritas GATT de payload, comandos de medição, serviços em segundo plano ou novas permissões.
- CI e validação física no GT3 MAX/Galaxy A06: **não validadas nesta alteração**.

## 04/11/2026 — Vinculação dos valores decodificados à sessão de captura

- Auditoria identificou que os payloads GATT brutos já exigiam sessão ativa, mas registros decodificados Bluetooth SIG eram gravados pela rotina genérica de descobertas, sem `sessionId`.
- Criado `recordSessionDiscovery`, que só grava valores se existir sessão válida e associa o registro ao identificador dessa sessão. Sessão expirada é finalizada e não aceita o novo valor.
- Leituras READ e notificações Bluetooth SIG passaram a usar a nova rotina. Inventários e metadados de descoberta continuam podendo ser registrados fora da captura, sem carregar os valores dos sensores.
- Teste de codec cobre a persistência do vínculo da descoberta decodificada com a sessão.
- CI após esta correção e validação física com GT3 MAX/Galaxy A06: **não validadas**.

## 04/11/2026 — Testes GATT automáticos e salvamento local de resultados

- Adicionado na tela principal o controle **Ativar testes automáticos ao conectar / Pausar coleta automática**. A ativação solicita consentimento explícito uma única vez e persiste a preferência no aparelho.
- Após conexão, a coleta automática só inicia quando o fingerprint GATT completo observado do GT3 MAX Call coincide com a referência local. Nome/MAC não substituem a validação GATT; o fingerprint não é autenticação criptográfica.
- O executor aprofundado agora despacha tanto leituras padronizadas Bluetooth SIG quanto inscrições padrão e operações READ proprietárias que já eram planejadas, em série pela fila GATT e aguardando callbacks/timeouts.
- Canais proprietários com READ/NOTIFY/INDICATE são observados e gravados sem inventar semântica. NUS RX, escrita de payload proprietária, DFU/OTA, reset e qualquer comando de medição desconhecido continuam bloqueados.
- Adicionado `GattValueDecoder` para os formatos Bluetooth SIG de bateria, Device Information, frequência cardíaca, pressão arterial, termômetro, temperatura/umidade ambientais e oximetria spot-check/contínua. A decodificação indica formato observado, não exatidão clínica/calibração do sensor.
- Leituras e notificações só persistem HEX e valores decodificados durante captura consentida na biblioteca local. As janelas são de até 10 minutos e renovadas enquanto o modo automático permanece ativo, GATT conectado e a MainActivity em primeiro plano; a renovação suspende em segundo plano e retoma ao voltar, pois ainda não existe serviço foreground BLE. Fora da captura, logs contêm tamanho/fingerprint, não HEX bruto.
- Pausar encerra a janela criada pelo modo automático e agenda cancelamento das inscrições criadas por ele via CCCD padrão. Se uma sessão manual já estava ativa quando a automação começou, ela é apenas reutilizada e não será encerrada pelo botão de pausa.
- A tela Testes e descobertas apresenta o estado do modo automático e recarrega o inventário ao retornar ao primeiro plano. A biblioteca exibe UUID de serviço e característica para cada leitura/notificação.
- Adicionados testes unitários para decoders, contato do sensor cardíaco, versão de firmware, flags de recursos PLX/Body Composition, RSC/CSC, pressão intermediária, flags reservadas e payloads malformados, além da política de consentimento e plano de exploração.
- A presença de um serviço/UUID não garante que o firmware gere medição. Inscrição NOTIFY/INDICATE apenas permite receber atualizações; não envia comando que inicie pressão, SpO2 ou outra medição.
- CI/build desta alteração e teste físico no Samsung Galaxy A06/Android 16 com GT3 MAX: **não validados** até a nova execução e teste no aparelho.

## 04/11/2026 — Correção do empacotamento dos artefatos Android CI

- Corrigido o workflow `.github/workflows/android-ci.yml`: o artefato `relogio-2.0-diagnostic-debug-apk` passa a receber diretamente o único arquivo `app-diagnostic-debug.apk`, sem diretório auxiliar e sem TXT/XML anexados.
- Os arquivos `diagnostic-build-info.txt`, manifestos, permissões, badging, assinaturas/certificados e hashes SHA-256 permanecem no artefato separado `android-validation-reports`.
- Mantidos dois artefatos distintos: **APK instalável** e **relatórios de validação**. A variante companion continua compilada/testada/auditada, mas não publicada como instalação.
- README e changelog alinhados com a separação; instrução antiga de procurar BUILD-INFO.txt dentro do APK foi corrigida.
- Adicionado teste Python de contrato para impedir regressão: o artefato instalável deve apontar para um único `.apk`, enquanto o relatório técnico permanece em artefato separado.
- Ajustada a chave de concorrência do workflow para usar a branch da PR (ou `github.ref_name` em push/manual); eventos `push` e `pull_request` da mesma branch agora compartilham o grupo de cancelamento e não mantêm dois builds idênticos em paralelo.
- A execução #297, anterior à correção do empacotamento, concluiu com sucesso em Lint, testes, builds, guardrails, auditoria e publicação dos dois artefatos; confirmou-se que o pacote do APK incluía arquivos auxiliares junto do instalador.
- A nova execução #303 foi disparada para o HEAD com o workflow corrigido; seu resultado ainda precisa ser conferido. A estrutura do novo artefato não será considerada validada até inspecionar a execução que usa este workflow.
- Teste físico no Galaxy A06/Android 16: **não validado**.

## 04/11/2026 — Acesso direto à exploração pelo laboratório

- Adicionado à tela Testes e descobertas um atalho para solicitar a exploração aprofundada de todos os canais GATT elegíveis.
- O atalho retorna ao painel principal e encaminha uma solicitação local; se o relógio já estiver conectado, abre a confirmação de segurança. Se ainda não estiver conectado, a solicitação aguarda a descoberta GATT da conexão seguinte.
- A confirmação existente continua obrigatória antes de qualquer leitura ou inscrição. Nenhum comando ou escrita de payload foi habilitado.
- CI e teste físico no Galaxy A06/Android 16: **não validados**.

## 04/11/2026 — Exploração aprofundada sem truncamento de canais GATT

- Removido o limite fixo de 24 operações que podia deixar características elegíveis fora da exploração manual sem indicação explícita.
- A exploração aprofundada agora percorre todas as características encontradas que anunciem READ ou NOTIFY/INDICATE com CCCD padrão; NUS, DFU, identificadores e serviços excluídos continuam fora do plano.
- A interface deixa de prometer "até 24 canais" e informa que a varredura pode levar vários minutos. A captura HEX continua condicionada à sessão cronometrada consentida.
- Adicionado teste de regressão com 30 canais proprietários elegíveis para garantir que o plano não trunque a lista.
- A característica observada 0x3802/0x4A02 só será inscrita se o inventário real confirmar NOTIFY/INDICATE e CCCD 0x2902; o UUID sozinho não comprova semântica. Nenhum payload de escrita será enviado.
- CI após estas alterações: aguardando. Teste físico no Galaxy A06/Android 16 com GT3 MAX: **não validado**.

## 04/11/2026 — Busca BLE geral e exportação de descobertas no TXT

- Corrigido o fluxo que exigia configurar manualmente o endereço Bluetooth antes de iniciar uma busca. Ao abrir o app, uma varredura BLE geral começa automaticamente; o campo de endereço passou a ser opcional.
- A busca não descarta endereços diferentes do MAC salvo. Cada dispositivo observado é listado e registrado no log da sessão com nome disponível, RSSI e endereço mascarado.
- Nome GT3 MAX, serviço NUS anunciado e endereço previamente salvo são pistas de descoberta, não prova de identidade. Quando o anúncio não identifica o relógio, o usuário pode selecionar um dispositivo sem digitar MAC.
- Candidatos com endereço novo são conectados somente para validar o fingerprint GATT; sem correspondência completa do GT3 MAX Call, a conexão é encerrada antes de leituras, inscrições ou exploração.
- Após fingerprint GATT completo, o endereço atual é salvo automaticamente. O fingerprint é compatibilidade observada, não autenticação criptográfica.
- O TXT passou a incluir até 1.000 registros persistidos da biblioteca (inventários GATT, quadros AB e capturas consentidas), além dos 250 eventos mais recentes da sessão. MAC permanece mascarado.
- Testes adicionados para política de candidatos BLE, fingerprint com nome ausente e inclusão/mascaramento das descobertas no TXT.
- A sincronização CTS por escrita de característica foi suspensa nesta fase para preservar a política somente leitura/notificações. Apenas configuração do descritor CCCD padrão permanece necessária para inscrições.
- Nenhum comando proprietário, OTA/DFU, reset ou nova permissão foi adicionado.
- CI, instalação e teste físico no Galaxy A06/Android 16 com GT3 MAX: **não validados**.

## 04/11/2026 — Contrato de justificativa de privacidade do Health Connect

- Ligada a tela existente HealthPrivacyActivity ao intent ACTION_SHOW_PERMISSIONS_RATIONALE para versões até Android 13.
- Adicionado activity-alias VIEW_PERMISSION_USAGE protegido por START_VIEW_PERMISSION_USAGE para Android 14+.
- A tela expõe somente a política informativa existente; nenhuma operação de saúde, permissão ou Bluetooth é executada ao ser aberta externamente.
- O auditor do manifesto compilado agora exige os dois pontos de entrada e verifica a proteção do alias; testes de política adicionados para ambas as variantes.
- A referência oficial do Android atualizada em 22/10/2026 documenta esses pontos de entrada e exige que a atividade apresente a mesma política publicada no Play Console.
- CI e validação no dispositivo: **não validados**.

## 04/11/2026 — Recuperação explícita da permissão Health Connect

- Corrigido o fluxo que fazia Atualizar atividade chamar connectHealthConnect() quando READ_STEPS não estava inicializada; atualizar agora apenas apresenta o estado sem acesso e nunca inicia uma solicitação de permissão.
- A recusa e a revogação de READ_STEPS são persistidas localmente como estados distintos; o estado armazenado não substitui a consulta real a getGrantedPermissions().
- Após uma recusa, a interface oferece acesso às configurações da Conexão Saúde e mantém a opção de nova solicitação somente após ação explícita do usuário.
- Android 14+ usa a tela do sistema para gerenciar permissões de saúde da aplicação; versões anteriores usam a ação oficial de configurações do Health Connect com fallback para detalhes do provider.
- Ao retornar das configurações, onResume reconsulta o provider e atualiza o estado sem abrir diálogos; uma autorização concedida passa a permitir leitura agregada diária.
- Adicionados testes unitários para primeira solicitação, recusa persistida, concessão, revogação e recuperação pelas configurações.
- Nenhuma permissão adicional, escrita no Health Connect, comando BLE ou alteração do protocolo GT3 MAX foi introduzida.
- CI desta fase, validação em Galaxy A06/Android 16 e fluxo de configurações no aparelho: **não validados**.

## 04/11/2026 — Melhoria da tela de conexão GT3 MAX

- Reorganizado o painel Bluetooth em cartão de estado, com indicador visual, título contextual e instrução objetiva conforme o estado informado pelo ciclo BLE.
- A ação principal foi renomeada para **Buscar relógio**; a ação de desconexão é habilitada apenas durante conexão, conexão estabelecida ou encerramento.
- Dispositivos encontrados passaram a ser exibidos em cartões com nome, RSSI e endereço parcialmente mascarado, mantendo a seleção por toque.
- Mantidos os limites semânticos: conexão GATT não significa protocolo identificado nem sensores validados. O texto da interface explicita essa distinção.
- Alteração restrita à apresentação de MainActivity.kt; não foram alterados scan, permissões, callbacks, fila GATT, descoberta, leitura, parser, armazenamento, Health Connect ou comandos ao relógio.
- CI da nova branch fix/ui-conexao-gt3-20261104: aguardando execução após os commits da interface. Validação visual e teste físico no Galaxy A06/GT3 MAX: **não validados**.

## 04/11/2026 — Correção do auditor de manifesto APK

- Run 41764184171 falhou somente na etapa de auditoria do manifesto final: `apkanalyzer manifest print` emitiu `Saúde & Fitness` com ampersand não escapado, e o XML deixou de ser bem-formado para o parser Python.
- A falha não demonstra que o manifesto binário do APK estava inválido; é uma incompatibilidade na serialização textual do `apkanalyzer`. O pacote de instalação foi corretamente retido e não publicado.
- Corrigido `scripts/audit_apk_manifest.py` para normalizar ampersands crus sem alterar entidades XML válidas. Adicionados testes para ampersand cru e `&amp;`; o reconhecimento de entidades numéricas foi corrigido.
- Atualizado `docs/INVESTIGACAO-BLOQUEIO-PLAY-PROTECT-2026-11-04.md` com a causa e o estado.
- Nova execução no SHA `4cac663eef14f5c9b236e725837bddcd52c61d55`: run 41764797014, inicialmente em fila. A atualização documental subsequente pode disparar outra execução; validar sempre o SHA mais recente.
- O Play Protect e a instalação física no Galaxy A06 continuam **não validados**.

## 04/11/2026 — Classificação B do bloqueio Play Protect

- O usuário identificou o aviso como “O app foi bloqueado para proteger seu dispositivo”, com base em síntese do Gemini; a transcrição literal e a captura de tela não estão disponíveis.
- A orientação oficial do Google associa esse bloqueio de sideload a capacidades sensíveis como READ_SMS, RECEIVE_SMS, NOTIFICATION_LISTENER e ACCESSIBILITY; Health & Fitness que retransmitem notificações ao próprio wearable são um caso de uso permitido, sujeito à finalidade e consentimento.
- Revisão dos manifestos: diagnostic não declara Notification Listener; companion declara NotificationCaptureService protegido por BIND_NOTIFICATION_LISTENER_SERVICE e exported=false.
- Portanto, a hipótese principal é instalação da variante companion ou de um APK anterior com Notification Listener. Se foi exatamente o APK diagnostic do run 41761934088, a regra de sideload por essas capacidades não explica o bloqueio. Causa final não determinada.
- Criado docs/INVESTIGACAO-BLOQUEIO-PLAY-PROTECT-2026-11-04.md e indexado em docs/README.md.
- Run 41764184171: testes de política, Lint, testes unitários/build e guardrails passaram; auditoria final de manifestos/assinaturas ainda estava em andamento na última consulta. Nenhum artefato novo confirmado.
- Não desativar Play Protect, não distribuir companion por sideload e não afirmar aprovação Google com base em CI.
- Instalação física no Galaxy A06, classificação final do Play Protect e identidade do APK que o usuário tentou instalar: **não validadas**.

## 04/11/2026 — Auditoria binária do Play Protect e endurecimento do CI

- Baixado e inspecionado o artefato `relogio-2.0-diagnostic-debug-apk` do run 41761934088, commit `e5e4b85ac1ad969956635a860a4b655b4466b768`.
- O teste do ZIP passou e o SHA-256 calculado correspondeu ao digest do artefato informado pelo GitHub Actions: `1e09ebbff67960e9bd6f30e0b7a7639b85fdc21e2475b4e91fc083dd349386cd`.
- SHA-256 do APK extraído: `21fcdb2f63abd21cdd8a6a5ce8229a1f833a27337ecbe4f3da6ffd944ac0eed1`.
- Manifesto binário diagnostic: pacote `com.luizmarquesarruda.relogio20.diagnostic`, versionCode 7, versão `0.4.0-alpha.1-diagnostic`, debug=true, allowBackup=false. Não há INTERNET, SMS, Accessibility, Notification Listener nem NotificationCaptureService.
- O certificado encontrado é o Android Debug padrão; não é identidade de release. A análise local ainda não realizou verificação criptográfica pelo apksigner.
- Conclusão: regra de bloqueio automático de sideload baseada em Notification Listener/SMS/Accessibility não explica esse binário diagnostic específico. A causa do aviso permanece **não determinada**; não foi possível confirmar o texto literal nem qual APK foi instalado.
- Implementado em novo commit da branch: `usesCleartextTraffic=false`; auditor Python do manifesto APK final com testes para pacote/variante, INTERNET, permissões sensíveis, serviço de acessibilidade e Notification Listener; expansão do guardrail de dependências; artefato com checksum, build-info e certificado; relatórios técnicos combinados em segundo artefato.
- CI novo deverá auditar ambos os manifestos via apkanalyzer, verificar assinatura v2 com apksigner e gerar SHA-256. Até o novo run concluir, estes portões estão **aguardando CI**.
- Nenhum comportamento BLE, parser, escrita no relógio, Health Connect ou captura companion foi alterado. PR #5 continua draft, aberta e sem merge.
- Play Protect no Galaxy A06 e instalação física: **não validados**.

## 04/11/2026 — Auditoria estrutural e navegação do catálogo GT3 MAX

- A tela inicial foi mantida como painel de conexão/diagnóstico e recebeu entrada para o catálogo de funções.
- Criada tela secundária de catálogo com áreas Saúde, Atividades, Dispositivo, Conectividade, Ferramentas e Privacidade.
- Os recursos do catálogo foram classificados por estado: existente no app, dependente de integração direta ainda não validada, ou bloqueado por segurança.
- Recursos de risco (OTA, reset de fábrica, controle de bloqueio, cartões de acesso e pagamentos) não recebem comandos nem ações executáveis.
- Saúde deixa explícito que sincronização Wearfit não comprova parser BLE próprio nem validação clínica. METT não é convertido automaticamente em calorias.
- Notificações e privacidade encaminham às telas já existentes; os demais itens são informativos até existir integração testada.
- Auditoria foi feita sobre o código e estrutura Android. Captura visual em aparelho/emulador não foi possível nesta sessão; contraste, TalkBack, tamanhos dinâmicos e comportamento físico permanecem **não validados**.
- MagicPath recusou chamadas por quota externa esgotada; nenhuma alteração foi feita no workspace MagicPath.
- CI/build após esta alteração: aguardando execução.
 
## 04/11/2026 — Contrato de domínio para métricas do GT3 MAX

- Criado modelo Kotlin puro em domain/health para tipo, unidade, origem, timestamps e estado de validação; sem dependência de Android, BLE ou Wearfit Pro.
- Criados testes para compatibilidade de unidades, limites básicos de SpO₂, contagem inteira de passos e preservação da origem de agregados Health Connect.
- METT permanece em unidade MET, sem conversão automática para calorias. Pressão arterial e SpO₂ permanecem não diagnósticos; a validação física do relógio não equivale à validação clínica.
- Criado docs/CONTRATO-METRICAS-SAUDE.md e indexado em docs/README.md.
- Nenhum parser de saúde BLE, permissão adicional, escrita Health Connect ou comando proprietário foi adicionado.
- Testes desta alteração e CI: aguardando execução. Testes físicos no Galaxy A06/GT3 MAX: **não validados**.

## 04/11/2026 — Alinhamento de módulos e catálogo GT3 MAX

- Revisadas as funções fornecidas do Wearfit Pro e registradas em matriz com 33 entradas, separando funções Android locais, dependências BLE/protocolo, Bluetooth Classic, integrações externas e ações bloqueadas. A presença no catálogo não comprova suporte no GT3 MAX específico.
- Criado `docs/MATRIZ-COMPATIBILIDADE-GT3-MAXX.md` com módulo responsável, dependências, prioridade, risco e estado de cada entrada.
- Corrigidos mapa de módulos e arquitetura para refletir o aplicativo Android nativo Kotlin; não presumir Wear OS, SDK proprietário ou chipset.
- Movidos `SerialGattOperationQueue` para `device/ble/gatt` e `WearfitProtocolProfile`/`WearfitAbFrameParser` para `device/gt3max/protocol`; os testes acompanham os pacotes. Lógica e bytes não foram alterados.
- `MainActivity.kt` continua concentrando UI, ciclo GATT e leitura Health Connect. A extração do coordenador e do adaptador Health Connect permanece como dívida técnica explícita.
- Corrigido o caminho do APK diagnostic no README e alinhada a versão documental para `0.4.0-alpha.1`.
- Nenhuma permissão, SDK, integração em nuvem ou escrita BLE proprietária foi adicionada. OTA/DFU, reset, restauração de fábrica, Alipay e cartão de acesso permanecem bloqueados.
- CI após este commit: aguardando execução. Testes físicos das 33 funções no Galaxy A06/GT3 MAX: **não validados**.

## 04/11/2026 — Continuidade: substituição progressiva do Wearfit Pro

- Objetivo consolidado: Relógio 2.0 deve conectar diretamente ao GT3 Maxx por BLE/GATT e integrar somente dados reais e validados ao Health Connect. Wearfit Pro fica temporariamente como referência comparativa, não como dependência permanente.
- Revisão da captura BLE: conexão e descoberta GATT foram observadas no Galaxy A06; NUS e quadro AB seguem candidatos. Não há evidência para classificar GT3 Maxx como Wear OS nem para validar passos, frequência cardíaca, pressão, sono, SpO₂ ou notificações retransmitidas.
- Auditoria arquitetural identificou dívida: `MainActivity.kt` ainda concentra UI, permissões BLE, ciclo de vida GATT, fila e leitura Health Connect. A extração em serviços independentes deve ser incremental, com testes e sem alterar o protocolo.
- Criado `docs/PLANO-SUBSTITUICAO-WEARFIT.md` com fases de captura passiva, correlação de métricas, parser isolado, persistência, Health Connect, UX e critérios para considerar o Wearfit Pro substituível.
- Corrigido erro XML em `app/src/main/AndroidManifest.xml`: o ampersand do rótulo Saúde & Fitness precisava ser escapado como `&amp;`. Commit: `b485bada2fb4d7de238796093f061077c57ce75e`.
- Atualizada a descrição da PR #5 para remover afirmação incorreta sobre `android:appCategory=fitness` e refletir o objetivo real de substituir progressivamente o Wearfit Pro.
- CI anterior do SHA `687f3a5` falhou no parse do manifesto antes de Lint/testes/build. Nova execução para `b485bada`: **em andamento** no momento deste registro. APK novo ainda não validado/disponível.
- PR #5 permanece aberta, draft e sem merge. Teste físico com GT3 Maxx/Galaxy A06, validação de sensores, sincronização Health Connect e ponte de notificações: **não validados**.

## 04/11/2026 — Definição do produto Saúde & Fitness e ponte wearable

- O usuário confirmou o modelo funcional: os sensores físicos do relógio medem passos e outras métricas; o companion recebe e valida as amostras; somente dados reais e autorizados devem ser compartilhados com o Conexão Saúde (Health Connect).
- Fluxo inverso separado: o Android publica notificações; o app lê apenas aplicativos selecionados e a camada de transporte faz a ponte para o relógio. A leitura não responde, apaga, arquiva nem altera notificações nos aplicativos de origem.
- A orientação oficial Play Protect lista expressamente apps de Saúde e Fitness que retransmitem notificações aos próprios dispositivos vestíveis como caso permitido de NOTIFICATION_LISTENER. Categoria/declaração não substitui a retransmissão efetiva nem o consentimento.
- A interface principal e o rótulo do Android foram alinhados a Saúde & Fitness. A categoria da ficha deve ser definida no Play Console como Saúde e fitness, com recurso Atividade e fitness; não foi usado `android:appCategory=fitness`, pois o valor não é válido no manifesto Android.
- Health Connect permanece em leitura agregada READ_STEPS nesta versão. Não declarar WRITE_STEPS nem pedir permissões de outros sensores enquanto o driver do GT3 Maxx não entregar amostras validadas.
- Health Connect é dispositivo-cêntrico; integração cloud/account-centric com Google Health API não está implementada.
- A tela de notificações explica finalidade de ponte, leitura somente, consentimento, filtros e limite de retenção. O encaminhamento físico ao GT3 Maxx ainda está bloqueado por ausência de protocolo validado.
- Criados docs/SAUDE-FITNESS-ARQUITETURA.md e HealthPrivacyActivity para transparência dos dados.
- Teste físico, integração de escrita Health Connect, ponte BLE de notificações, Play Console e Play Protect: **não validados**.

# Diário de bordo — Relógio 2.0

Este arquivo é o registro cronológico principal. Atualizar em toda PR que altere arquitetura, funções, dependências, versão, dados ou protocolo.

## 03/11/2026 — Alinhamento Play Protect e estratégia de distribuição

- Consultada a orientação oficial Google para avisos do Play Protect. O Google documenta que, em mercados selecionados, instalações por sideload da internet com capacidades sensíveis como NOTIFICATION_LISTENER podem ser bloqueadas.
- O manifesto desta branch declara NotificationCaptureService protegido por BIND_NOTIFICATION_LISTENER_SERVICE e exported=false, sem declarações READ_SMS, RECEIVE_SMS ou Accessibility Service.
- O serviço exige acesso especial, ativação explícita e seleção de aplicativos; as prévias ficam em memória, limitadas a 30 itens/15 minutos, sem gravação ou envio à rede.
- A implementação atual ainda não encaminha notificações ao GT3 Maxx. Não declarar que já cumpre o caso de uso de retransmissão para wearable reconhecido na orientação do Google.
- APK atual é debug e não release assinada. CI aprovado confirma build/testes, não validação Play Protect.
- Decisão implementada nesta branch: separar variante diagnostic sem Notification Listener e variante companion com o serviço isolado em source set/manifest próprio. A diagnostic é a única variante publicada como artefato de instalação local; companion é compilada/testada, sem upload de APK de sideload.
- Proibido orientar desativação do Play Protect, ocultar capacidades do manifesto ou contornar a análise.
- CI da PR #5: run 41750419237 concluído com sucesso no SHA 9b7c563b206e63f6ba961a2f90ef2d5c1e2cbab2. Artefato debug e relatório Lint publicados; validade informada até 18/11/2026. Isso não significa que o Play Protect aprovou o APK.
- Documento criado: docs/ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md. CI atualizado para testar as duas variantes e conferir o manifesto final de cada APK. Bloqueio exato e teste físico no Galaxy A06: **não validados**, pois a mensagem exibida pelo Play Protect não foi fornecida.

## 03/11/2026 — Alinhamento com documentação oficial Google/Android

- Analisada a documentação de integração do app complementar Google Pixel Watch, Wearable Data Layer, permissões Bluetooth, Companion Device Manager, notificações Wear OS, NotificationListenerService, Health Connect, Health Services e qualidade Wear OS.
- Decisão: o GT3 Maxx permanece tratado como dispositivo BLE/GATT genérico pareado pelo Wearfit Pro; não existe evidência que permita classificá-lo como Wear OS.
- A Wearable Data Layer e Health Services não são adotadas no dispositivo atual, pois são APIs do ecossistema Wear OS.
- Para o telefone Android, manter permissões BLE mínimas, Health Connect somente para leitura autorizada e origem dos dados preservada.
- A ponte de notificações do Wear OS é feita pelo sistema; no GT3 Maxx, nenhum envio de mensagens por GATT será implementado sem protocolo comprovado.
- Companion Device Manager fica como experimento futuro, sujeito à compatibilidade do anúncio real do relógio e testes de regressão.
- Documento de referência criado em `docs/ALINHAMENTO-GOOGLE-ANDROID-WEAROS.md`.
- Nenhuma dependência Wear OS foi adicionada. Teste físico Galaxy A06/GT3 Maxx: **não validado**.

## 03/11/2026 — Endurecimento de privacidade das notificações (PR #5)

- Adicionada expiração de prévias após 15 minutos usando tempo monotônico, evitando depender de alterações do relógio civil do aparelho.
- Cache de notificações é limpo em `onListenerDisconnected()` e `onDestroy()`.
- A tela apresenta aviso de privacidade antes do atalho para a tela especial do Android, explica o uso do acesso, o filtro por aplicativo e a retenção.
- Ativação da captura bloqueia-se sem acesso concedido ou sem ao menos um pacote selecionado.
- Novo teste unitário verifica o limite de expiração; os testes de remoção e limite de 30 itens foram adaptados para relógio monotônico controlado.
- Versão atualizada para `0.3.0-alpha.4` / versionCode 6.
- CI desta revisão: aguardando execução. Play Protect e teste físico no Galaxy A06/GT3 Maxx: **não validados**.
- O manifesto continua sem permissões SMS e serviço de acessibilidade; a presença de NotificationListenerService, por si só, não identifica a causa do aviso “Prejudicial”.

## 03/11/2026 — Auditoria adicional e perfil BLE candidato (PR #5)

### Causa do Lint e correções
- O primeiro CI da PR #5 falhou em lintDebug por leitura de BluetoothDevice.name sem verificação explícita de BLUETOOTH_CONNECT. Foram centralizados helpers de nome/endereço com verificação de permissão e fallback seguro.
- O scan agora trata SecurityException e IllegalStateException se a permissão for revogada ou o estado Bluetooth mudar entre a checagem e a chamada.
- A API Android permite fornecer Handler a connectGatt; o app agora usa o Handler principal (minSdk 26), garantindo que callbacks GATT e fila serial compartilhem a mesma thread. Callbacks de falha e desconexão também limpam o estado, e operações são bloqueadas durante o desligamento.
- Novas operações são bloqueadas durante a desconexão. Falhas GATT limpam estado e fila.
- A lista de apps na tela de notificações recebeu uma área de rolagem própria.

### Evidência de protocolo
- A captura local contém NUS UUIDs terminados em dcca9e e quadro AB 00 05 FF 91 80 00 0C.
- O projeto público fbiego/dt78 contém os mesmos UUIDs e uma fixture AB 00 05 FF 91 80 00 50 comentada como bateria 80%.
- Criado perfil NUS_AB_FRAME_COMPATIBLE_CANDIDATE e parser Kotlin offline para quadros completos, com testes.
- A hipótese de bateria 12% para o byte 0C permanece **não validada**. O GT3 Maxx não foi identificado como DT78/X8 Ultra e o chipset continua desconhecido.
- Nenhuma escrita de payload ou comando proprietário foi enviada. A característica RX e Secure DFU continuam bloqueadas.

### Auditoria de módulos
- BLE scan/permissões, ciclo de vida GATT, serialização, callbacks e parsing: alterações e testes offline registrados.
- Health Connect: consulta agregada permanece separada dos dados do relógio; ausência de valor não é zero; teste físico não validado.
- Notificações: filtro por pacote, cache volátil de 30 itens, remoção por callback e revogação de pacote; conteúdo não é persistido nem enviado à rede.
- UI: lista de aplicativos rolável; teste visual no Galaxy A06 não validado.
- CI separado em etapas: lint, testes/build e upload do relatório Lint; o APK debug é publicado quando a etapa de testes/build passa, mesmo que o Lint mantenha a execução global vermelha.
- Pushes para branches fix/* também iniciam o workflow, além de PRs.
- Estrutura: MainActivity.kt continua concentrando BLE e Health Connect; extração de serviços/camadas segue como dívida arquitetural, não foi mascarada como concluída.

### Versão e validação
- Versão de desenvolvimento proposta: 0.3.0-alpha.3.
- Lint, testes e APK: aguardando execução do CI após as correções.
- Teste físico no Galaxy A06 e GT3 Maxx: **não validado**.
## 03/11/2026 — Correções de integridade Android (PR de correção encadeada)

### Correções implementadas na branch `fix/integridade-android-20261103`
- O timeout da busca BLE é guardado e cancelado quando a busca termina antecipadamente, falha ou o usuário inicia conexão. Resultados e falhas de callbacks antigos são descartados.
- Leituras de característica e inscrições CCCD passam por uma fila serial. A fila avança após callback de leitura/escrita, e uma inscrição só é marcada ativa após `onDescriptorWrite` com status de sucesso.
- Callbacks de GATT de conexões anteriores não limpam nem atualizam o estado da conexão atual.
- Health Connect não converte ausência de agregado em zero; o painel apresenta “sem registros”. O ciclo periódico não abre repetidamente o pedido de permissão negado; a solicitação continua disponível pela ação explícita de conexão.
- `NotificationListenerService.onNotificationRemoved` elimina a prévia pela chave Android. Desmarcar um pacote também elimina as prévias daquele aplicativo.
- Testes unitários adicionados para fila GATT, semântica de passos e remoção/limite do cache.
- Android Lint foi incluído no workflow junto de testes e APK debug.
- Versão proposta de desenvolvimento: `0.3.0-alpha.2`.

### Riscos/limites ainda conhecidos
- A fila depende dos callbacks GATT do Android; um callback perdido pode manter operações seguintes aguardando até desconexão/reconexão. Timeout/recuperação de operação é uma etapa posterior, pois expirar uma operação sem correlacionar callbacks atrasados pode concluir incorretamente a operação seguinte.
- A remoção de prévias depende de o Android entregar `onNotificationRemoved`; política de expiração por tempo ainda não foi definida.
- CI, lint, testes nesta branch e build APK: **não validados** até execução do workflow.
- Instalação e teste físico no Galaxy A06/GT3 MAX: **não validados**.

## 03/11/2026 — Implementação inicial do listener Android (PR #4)

### Implementado nesta branch
- NotificationListenerService com permissão de vinculação do sistema.
- Tela para abrir as configurações de acesso às notificações.
- Controle separado para ativar/pausar a captura.
- Lista de aplicativos de usuário instalados; captura somente dos pacotes selecionados explicitamente.
- Prévia de título/texto somente quando disponibilizados pelo Android.
- Cache volátil das 30 últimas prévias; sem gravação em banco/arquivo, sem telemetria e sem envio à rede.
- Limpeza manual do cache e atualização da tela enquanto aberta.
- Teste unitário para normalização, espaços em branco e limite de 500 caracteres.
- Versão de desenvolvimento elevada para `0.3.0-alpha.1`.

### Limites mantidos
- O serviço não lê banco, arquivos, chaves ou sessões do WhatsApp.
- A prévia depende das configurações do WhatsApp e do Android; conteúdo oculto/redigido não é recuperado.
- Nenhuma notificação é enviada ao GT3 MAX, pois o protocolo BLE de mensagens permanece não identificado.
- CI, instalação e teste físico desta nova fase: **não validados** até conclusão do workflow e teste no Galaxy A06.

## 03/11/2026 — Organização, Android e notificações

### Estado observado
- Repositório público: `luizmarquesarruda/rel-gio-2.0`.
- Branch principal: `main`; permanece documental neste momento.
- PR #1: prova de conceito BLE Kotlin, aberta e não mesclada.
- PR #2: Health Connect/passos e referência de horário, aberta e não mesclada, baseada na branch BLE.
- O GT3 MAX conectou por BLE e expôs serviços GATT; o pacote de 8 bytes recebido pela Nordic UART permanece bruto e não validado.
- Não há protocolo de leitura de passos, ajuste de horário ou envio de notificações ao relógio comprovado.

### Auditoria de CI
- Execuções anteriores da PR #1 falharam em compilação Kotlin por comparação booleana incorreta na chamada de leitura GATT.
- Execuções anteriores da PR #2 falharam pela mesma comparação e por assinatura incompatível de `onRequestPermissionsResult`.
- PR #1: CI aprovado no run `41733030099`, SHA `8f5476a8bc09f0613ecbb46a29a45c41b5fe1c23`; artefato `relogio-2.0-debug-apk`, ID `13278182980`, validade informada até 17/11/2026.
- PR #2: CI aprovado no run `41733033992`, SHA `52b0a1df5e486ae3c9edf7ed8e941f2993ed2de5`; artefato ID `13276687560`, validade informada até 17/11/2026.
- Depois do primeiro build aprovado, PR #2 recebeu sincronização de ancestralidade com a PR #1; novo run `41733599125` está enfileirado no SHA `085b856c5c00c2cd3da65f3bb55eaeabf829688a`. Esse SHA ainda não está validado.
- PR #3: run `41733318639` está em execução sobre o tree de código/documentação antes do commit de sincronização de ancestralidade; validar novamente o HEAD atual após a execução correspondente.
- APKs dos runs aprovados existem como artefatos GitHub Actions; são builds debug, não releases assinadas de produção.

### Pesquisa e decisões
- Health Connect permanece a integração preferencial para passos no Android. Total agregado não significa total exclusivo do GT3 MAX.
- APIs Google Fit estão em fase de descontinuação e o suporte oficial é indicado até o fim de 2026.
- NotificationListenerService é a API Android indicada para receber notificações publicadas pelo sistema após autorização especial do usuário.
- Notificações WhatsApp podem ser observadas somente na forma e nos campos fornecidos pelo Android; não acessar banco privado nem contornar conteúdo oculto.
- A ponte das notificações do Android para o GT3 MAX permanece bloqueada pela ausência de protocolo validado.
- A página pública do Wearfit Pro sobre notificações lista telefone, SMS, WeChat, QQ e Weibo; WhatsApp não está confirmado nessa página.
- As APIs de notificação Wear OS não são aplicáveis por presunção ao GT3 MAX.

### Organização de branches e PRs
- PR #1 permanece aberta contra `main`.
- PR #2 permanece aberta contra `feat/ble-readonly-proof-of-concept`; recebeu commit de sincronização para eliminar divergência de histórico com a branch de BLE.
- PR #3 permanece aberta contra `feat/health-connect-steps-clock`; sua branch foi sincronizada com a atualização da PR #2.
- Nenhuma PR foi mesclada em `main`.

### Organização documental criada
- Índice de documentação.
- Mapa de módulos e estrutura-alvo Kotlin.
- Inventário ampliado das funções do Wearfit Pro, com prioridades e recursos bloqueados por risco.
- Especificação da integração Android/WhatsApp.
- Changelog e arquivo de versão.

### Próximas etapas
1. Confirmar CI verde das PRs #1 e #2 e obter APK debug.
2. Fazer teste físico no Galaxy A06: permissões de notificação e leitura de eventos sem gravar conteúdo em logs.
3. Validar no Galaxy A06 o listener Android implementado na PR #4, incluindo WhatsApp, apps selecionados, pausa e limpeza.
4. Separar `MainActivity.kt` em módulos pequenos sem alterar comportamento BLE.
5. Investigar se Wearfit Pro compartilha passos com Health Connect.
6. Validar um método suportado de encaminhamento de notificações ao GT3 MAX; não enviar payload experimental.

## Registro de versão

A versão `0.2.0-alpha.1` é uma versão de desenvolvimento proposta/registrada nesta branch. Não representa release publicada nem build validado. A numeração só deve ser promovida após CI verde e aprovação dos testes aplicáveis.


## 04/11/2026 — Exportação local do diagnóstico BLE em TXT

- Adicionada a ação **Exportar diagnóstico TXT** na tela principal. O Android abre o seletor nativo de destino (`CreateDocument`); o app não grava em diretório público por conta própria nem envia o relatório à rede.
- O arquivo reúne versão do app/Android, estado da interface, identificação do dispositivo com endereço MAC mascarado, eventos da sessão, inventário GATT e payloads BLE recebidos em hexadecimal quando registrados.
- O relatório separa observações de hipóteses e informa que conexão GATT não comprova leitura válida de sensores. Interpretações candidatas continuam explicitamente não validadas.
- Não são exportados textos de notificações do telefone. Payloads BLE hexadecimais podem conter métricas de saúde e devem ser compartilhados somente com autorização.
- Adicionados testes unitários para mascaramento de MAC, relatório sem eventos e preservação dos bytes observados.
- Não foram introduzidos comandos proprietários, escrita em características desconhecidas, permissões novas, rede ou upload automático.
- Build/CI e teste físico após esta alteração: **não validados** até a conclusão da execução correspondente.


## 04/11/2026 — Busca e conexão automática BLE

- A ação principal inicia busca BLE por até 15 segundos e conecta apenas após correspondência exata com o endereço GT3 MAX configurado localmente.
- O endereço é informado pelo usuário e armazenado em preferências privadas do aplicativo; não é embutido no repositório público nem exportado no relatório TXT.
- Dispositivos divergentes são bloqueados também no método de conexão, evitando bypass pela seleção manual.
- RSSI igual ou inferior a -90 dBm suspende a conexão automática. É um limite operacional heurístico, não uma medida exata de distância ou garantia de enlace.
- Status GATT 147 recebe mensagem específica de timeout, sem atribuição de causa definitiva.
- A conexão GATT não valida, por si só, frequência cardíaca, sono, SpO2 ou outros sensores.
- CI e teste físico no Galaxy A06/GT3 MAX após esta alteração: não validados.


## 04/11/2026 — Correção do estado BLE e hierarquia da página principal

### Evidência que motivou a correção
- O relatório físico do GT3 MAX mostrou conexão GATT bem-sucedida (status 0 / STATE_CONNECTED) e descoberta de seis serviços. Depois disso, novas buscas BLE foram iniciadas e a interface voltou a exibir “Procurando relógio”.
- A inspeção do código confirmou que startBleScan() não bloqueava uma nova busca quando bluetoothGatt ainda estava ativo; o botão de busca também permanecia habilitado no estado conectado.

### Correções implementadas na PR #6
- A busca BLE agora é bloqueada quando existe uma sessão GATT ativa ou em encerramento. O botão Buscar fica desabilitado durante a busca e enquanto a sessão GATT existir.
- O estado CONNECTING é refletido na interface; a recusa do Android em iniciar discoverServices() agora produz estado e evento de diagnóstico explícitos.
- O cartão de conexão, a configuração de identidade e a exportação TXT foram movidos para o topo da página, logo após o relógio do telefone.
- A página principal usa uma única rolagem vertical. O inventário GATT não cria uma rolagem interna adicional.
- O registro visual exibe as oito linhas mais recentes; o buffer interno continua limitado a 250 eventos e a exportação TXT preserva o buffer da sessão.
- Nenhum comando foi enviado ao relógio, nenhuma característica proprietária foi escrita e nenhuma leitura de sensor foi presumida.

### Validação e pendências
- Alterações enviadas para a branch fix/ui-conexao-gt3-20261104, na PR #6; sem merge.
- CI após estes commits: aguardando/ não validado até confirmar a execução no HEAD final.
- MagicPath: análise visual do canvas não realizada nesta execução porque a conta retornou cota de chamadas externas esgotada. A análise de layout foi feita por inspeção do código Android e registrada em docs/NAVEGACAO-E-LAYOUT.md.
- Renderização, TalkBack, fonte ampliada, rotação e teste físico após a correção no Galaxy A06/GT3 MAX: **não validados**.
- A descoberta GATT comprova o canal BLE e a enumeração dos serviços, mas o protocolo dos sensores continua **não validado**.
## 04/11/2026 — Extração do adaptador Health Connect e base para rotas/saúde

### Alterações nesta fase
- Criado StepsSnapshot no domínio para representar o agregado de passos com intervalo, hora de recebimento e origem explícita HEALTH_CONNECT_AGGREGATE.
- Criado HealthConnectStepsRepository em data/healthconnect para centralizar disponibilidade do provider, permissão READ_STEPS, consulta agregada diária e tratamento de ausência/revogação/indisponibilidade.
- MainActivity deixou de montar diretamente AggregateRequest e consultar StepsRecord; passa a consumir resultados tipados do repositório.
- Adicionados testes unitários para total zero versus ausência, origem, intervalo e rejeição de valores negativos.
- Documentação classifica as informações de sensor óptico, Bluetooth e marcações RoHS/CE como descrição comercial informada, não como prova de precisão, chipset ou protocolo.
- Documentada a separação entre registrar caminhadas com GPS do telefone e importar rotas do relógio; ambas exigem autorização e fontes reais.

### Limites preservados
- Não foram adicionadas permissões de localização, rota, pressão arterial, frequência cardíaca ou SpO₂.
- Não foram adicionadas permissões WRITE ao Health Connect.
- Não foram criadas medições sintéticas nem atribuído o agregado Health Connect ao GT3 MAX.
- Nenhuma escrita BLE, envio para NUS RX, sincronização de horário, OTA, reset ou configuração foi habilitada.
- Health Connect permanece leitura de passos agregados e não equivale a backup na nuvem da Conta Google.

### Validação
- Testes unitários do novo snapshot adicionados; resultado do Android CI desta branch: **não validado até o workflow concluir**.
- Teste no Galaxy A06, inspeção dos registros publicados pelo Wearfit Pro no Health Connect e validação física dos sensores: **não validados**.
- Rotas no mapa e leitura de sinais vitais: **planejadas, não implementadas**.
- A atualização periódica do Health Connect só é ativada após READ_STEPS concedida e é interrompida quando o acesso é negado ou revogado.

- A janela diária de passos é calculada a partir do mesmo instante de consulta no fuso local; cancelamento de coroutine é propagado e a consulta periódica para quando o provider fica indisponível/precisa de atualização.


## 04/11/2026 — Perfil de acesso ao GT3 MAX Call

### Objetivo confirmado pelo usuário
- O objetivo não é obter nem instalar firmware. O aplicativo deve usar os identificadores da unidade para reconhecer o GT3 MAX Call e acessar somente as funções comprovadas pelo relógio.

### Alterações na branch `feat/gt3max-call-device-profile-20261104`
- Registrados como metadados informados pelo proprietário: modelo GT3 MAX Call, firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1. Esses valores não são lidos/autenticados pela conexão GATT.
- Criado `Gt3MaxCallDeviceProfile` com identificação do nome BLE e fingerprint dos serviços/características observados na captura de 03/11.
- A inscrição automática em NUS TX exige nome GT3 MAX reconhecido e fingerprint completo dos três serviços proprietários capturados, além de RX/TX NUS. Se faltar qualquer parte, o app não faz inscrição automática.
- O app escuta apenas NUS TX com notificação padrão CCCD. Não envia comandos nem dados para NUS RX. Nenhuma característica privada é escrita.
- O quadro AB 0x91/0x80 é mostrado como **bateria candidata**, ainda pendente de comparação com Wearfit Pro em mais de um nível de carga.
- Acrescentado teste para impedir inscrição automática quando somente nome/NUS coincidem sem fingerprint completo.
- Adicionada entrada de bateria ao catálogo visual de funções.

### Limites
- MAC, SN e código de pareamento não foram gravados no repositório. O endereço continua a ser salvo somente nas preferências privadas do app pelo fluxo de identificação existente.
- Frequência cardíaca, SpO2, pressão, sono, passos originados no relógio e sessões de atividade ainda não têm parser comprovado.
- Chamadas/áudio da variante Call dependem de investigação separada de Bluetooth Classic/HFP/A2DP.
- Horário, brilho, botões, notificações enviadas ao relógio e outros controles ainda não possuem protocolo de comando validado.
- OTA/DFU, reset e configurações destrutivas continuam fora deste objetivo.

### Validação
- Testes adicionados ao código; resultado real de CI neste HEAD: **não validado** até a conclusão do workflow.
- Teste físico no Samsung Galaxy A06/Android 16 e comparação da bateria com Wearfit Pro: **não validados**.
- A implementação não foi instalada no telefone do usuário por esta alteração.


## 04/11/2026 — Canal de aplicativos parceiros Wearfit Pro — análise da política de 2023

- Incorporada a política oficial Weike em chinês, atualizada em 04/08/2023 e efetiva desde 10/03/2022, correspondente ao texto enviado pelo proprietário.
- A seção 1.3.2 prevê acesso por apps terceiros autorizados a um identificador aleatório exclusivo por aplicativo, nome/hardware/estado do wearable, envio de mensagens/notificações e acesso autorizado a sensores, sono, tipo de exercício, estado de uso, logs, localização e informações do dispositivo.
- A política não fornece nome/versão de SDK, artefato Maven/AAR, APIs, callbacks, intents, exemplo Android, estrutura de dados, unidades ou timestamps.
- A página corporativa WAKE UP descreve uma plataforma ecológica aberta/compartilhada e fornece canal de cooperação comercial; isso sugere processo de parceria, mas não confirma SDK público ou acesso automático.
- Escopo: a cláusula de terceiros fala especificamente de vestíveis Weike pareados. A compatibilidade/eligibilidade do GT3 MAX Call ainda precisa ser confirmada; o nome comercial e o uso do Wearfit Pro não bastam para assumir que pertence ao catálogo Weike.
- Third-Party SDK List é a lista de dependências do próprio Wearfit Pro (analytics/publicidade/mapas/push etc.), não o SDK para um app parceiro controlar o wearable.
- Prioridade definida: obter da WAKE UP/Weike documentação, SDK Android, autorização, exemplo e confirmação explícita para GT3 MAX Call, firmware V1.07/build SH0050.
- Nenhuma API privada foi presumida ou simulada. Nada foi lido do armazenamento interno do Wearfit Pro; nenhuma credencial foi reutilizada; nenhuma escrita proprietária/OTA/reset foi executada.
- Perfil BLE já adicionado continua como caminho local de compatibilidade/diagnóstico; bateria é candidata e semântica de sensores/controles continua não validada.
- A implementação do SDK de parceiros não foi feita por ausência de SDK/contrato técnico verificável. CI e teste físico permanecem **não validados**.


## 04/11/2026 — Auditoria modular e correções de compatibilidade/segurança

### Evidências identificadas
- O Android Lint do run 180 apontou um erro `NewApi`: `LocalDate.ofInstant` exige API 34, mas o aplicativo declara `minSdk 26`. O mesmo run concluiu testes, builds, guardrails de privacidade e auditoria dos manifestos/assinaturas; a etapa de Lint falhou e impediu a publicação do APK.
- O workflow preenchia `BUILD-INFO.txt` com a versão antiga `0.4.0-alpha.2-diagnostic`, embora `VERSION` e `app/build.gradle.kts` estivessem em `0.4.0-alpha.3`.
- A tela de inventário GATT disponibilizava botões genéricos de leitura e inscrição em qualquer característica que anunciasse READ/NOTIFY/INDICATE. Isso ampliava a superfície de interação além do perfil de compatibilidade permitido.
- `MainActivity.kt` segue concentrando UI e ciclo BLE; a extração do transporte permanece dívida arquitetural e não foi simulada como concluída.

### Correções aplicadas na branch `fix/security-quality-gates-20261104`
- Criado `HealthConnectDayWindow`, calculando a data local com `Instant.atZone(zoneId).toLocalDate().atStartOfDay(zoneId)`, compatível com `minSdk 26`; o repositório Health Connect passou a usar o helper.
- Adicionados testes da virada de data em `America/Sao_Paulo` e `Pacific/Auckland`.
- Removidos os controles manuais genéricos “Ler” e “Ouvir” das características descobertas. O inventário continua exibindo UUIDs, propriedades e descritores. A inscrição automática limita-se à NUS TX, exige nome conhecido + fingerprint GATT completo e usa apenas o CCCD padrão; NUS RX não recebe dados.
- O workflow agora lê a versão canônica de `VERSION`, compara com `versionName` do Gradle e gera o `BUILD-INFO.txt` dinamicamente com sufixo `-diagnostic`.
- Adicionado teste Python para manter `VERSION`, Gradle e metadados do artefato alinhados.
- Mapa de módulos e changelog atualizados.

### Situação de validação
- O CI executado no HEAD anterior à correção reportou um erro Lint `NewApi` e 61 avisos; os testes unitários, builds das variantes, guardrails e auditorias de APK concluíram com sucesso, mas a execução global falhou no Lint.
- Após a correção, o novo CI ainda precisa concluir no HEAD final desta branch. Nenhum status verde é declarado nesta etapa.
- Testes físicos de BLE e interface no Galaxy A06/GT3 MAX Call, TalkBack, escala de fonte e comparação da bateria com Wearfit Pro: **não validados**.
- Integração Wearfit Partner SDK, sensores, rotas e encaminhamento BLE de notificações permanecem pendentes de contrato/protocolo validado.
- OTA/DFU, reset, comandos proprietários e gravação de dados de saúde continuam bloqueados.


## 04/11/2026 — Segunda rodada de auditoria: obsolescência, manifesto e proveniência do APK

### Evidências da nova rodada
- A execução #193 da PR #9 passou em Lint, testes, builds diagnostic/companion, guardrails, auditoria dos manifestos/assinaturas e publicação do APK. O relatório Lint ainda mostrava Activity KTX 1.10.1 e coroutines Android 1.10.2, ausência de ícone e regras explícitas de extração, além de referências Bluetooth API 31 no caminho legado.
- O manifesto declarava BLUETOOTH_SCAN/CONNECT como permissões comuns, embora sejam permissões do Android 12/API31+. O atributo neverForLocation também aparecia no nó comum.
- Após a retirada dos botões de leitura GATT, dois callbacks onCharacteristicRead e handleRead permaneceram sem chamadores.
- README.md ainda instruía usar os botões Ler/Ouvir, informava a versão 0.4.0-alpha.2 e descrevia seleção manual, divergindo do fluxo atual de identificação por endereço local e conexão automática.
- BUILD-INFO.txt registrava o SHA de merge/teste usado pelo evento pull_request, sem distinguir o SHA fonte da branch/PR.

### Correções aplicadas nesta rodada
- Atualizadas AndroidX Activity KTX para 1.13.0 e kotlinx-coroutines-android para 1.11.0, versões estáveis publicadas pelos respectivos projetos. Health Connect 1.1.0 foi mantido.
- BLUETOOTH_SCAN e BLUETOOTH_CONNECT permanecem no elemento padrão uses-permission; o atributo neverForLocation recebe tools:targetApi=s para delimitar o aviso de Lint à API 31+. A permissão de localização para versões legadas continua limitada a maxSdkVersion=30.
- Adicionado ícone vetorial local e regras de exclusão para backup legado, backup em nuvem e transferência entre dispositivos. allowBackup permanece false.
- Auditor de manifestos ampliado para verificar permissões padrão, rejeitar o elemento não suportado uses-permission-sdk-31, além de verificar ícone, dataExtractionRules e fullBackupContent.
- Removidos callbacks GATT de leitura e função handleRead que ficaram sem chamadores. O fluxo CCCD mantém o retorno Boolean legado e isola a chamada moderna em helper anotado para API 33+.
- Workflow passa a registrar source_commit (HEAD da PR) e tested_commit (SHA efetivamente testado pelo GitHub Actions).
- README atualizado para refletir identificação local, conexão automática, inventário GATT passivo e versão canônica.
- targetSdk 36 mantido: desde 31/08/2026, o Google Play exige API 36 ou superior para novas apps/atualizações. A elevação a API 37 não é necessária para cumprir o requisito atual e fica pendente de testes Android 17.
- Avisos menores de internacionalização/setTextI18n e recomendações UseKtx podem permanecer, mesmo com compilação e testes aprovados.

### Validação
- Os testes de contrato foram executados primeiro e falharam em 7 condições nos runs #194/#195, identificando as inconsistências antes da correção.
- O CI deverá validar novamente os contratos, os dois sabores, permissões/serviços mesclados, assinatura e artefatos no HEAD final.
- Galaxy A06/Android 16, instalação, TalkBack, transferências do Android e comunicação física GT3 MAX: **não validados nesta rodada**.
- Nenhum sensor, protocolo proprietário, notificação enviada ao relógio, OTA/DFU ou reset foi habilitado.

## 04/11/2026 — Ajuste final do auditor de APK compilado

- Run #207 comprovou que AAPT/apkanalyzer serializa referências de recursos do manifesto como `@ref/0x...`, embora a fonte declare `@xml/data_extraction_rules` e `@xml/backup_rules`.
- O auditor foi ajustado para exigir que ambos os atributos existam e não estejam vazios no APK final, sem exigir que o nome simbólico sobreviva à compilação.
- Os testes de fonte continuam exigindo os identificadores de recursos exatos e confirmam que backup em nuvem, transferência e backup legado excluem os dados do aplicativo.
- Adicionado teste que reproduz o manifesto compilado com referências `@ref` e testes de regressão para ausência de cada atributo.
- Corrigida última frase obsoleta no README: a seleção manual de um dispositivo genérico foi removida; a conexão usa somente o endereço GT3 MAX configurado localmente.
- Run #207 passou Lint, testes Kotlin, build das duas variantes e guardrails de dependências/permissões. Falhou somente na comparação textual dos atributos de backup no auditor. O CI seguinte deve revalidar o novo comportamento, auditoria das assinaturas e publicação.
- Não houve instalação física ou teste de conexão no Galaxy A06 nesta rodada.


## 04/11/2026 — Modernização compatível do toolchain e limpeza de Lint

- O relatório Lint do APK da execução #207 mostrou 54 avisos: 50 SetTextI18n, 2 UseKtx, 1 UseRequiresApi e 1 OldTargetApi. Lint passou, mas os três avisos estruturais foram corrigidos; avisos SetTextI18n permanecem como dívida de internacionalização.
- O Gradle 8.13 produziu aviso explícito de versão desatualizada. A matriz Kotlin declara KGP 2.4.21 compatível com Gradle até 9.7.0 e AGP entre 8.5.2 e 9.3.1. O projeto mantém AGP 8.13.2 (dentro da faixa), atualiza KGP para 2.4.21 e Gradle para 9.5.0.
- Atualizada a anotação de helper API33+ de TargetApi para RequiresApi, propagando a exigência ao chamador.
- Substituídas duas operações manuais SharedPreferences.edit().apply() pela extensão androidx.core.content.edit, já presente na árvore runtime.
- Corrigida a última descrição do README que dizia que o usuário escolhia um dispositivo genérico; agora registra conexão somente ao endereço local configurado.
- Testes Python adicionados para fixar as versões compatíveis do toolchain, exigir RequiresApi/KTX e manter o README coerente.
- CI nesta combinação deve validar Lint, testes, montagem dos APKs, manifestos e assinatura antes de declarar a atualização aprovada.
- A atualização do AGP para 9.x foi deixada fora deste conjunto: AGP9 muda o modelo de integração Kotlin e requer migração específica do projeto. AGP8.13.2 permanece dentro da faixa compatível do KGP 2.4.21.


### Ajuste de compatibilidade do Gradle — 04/11/2026
- O run #210/#211 falhou ao aplicar AGP 8.13.2 com Gradle 9.7.0: o log confirmou que AGP8.x usa InternalProblems, API interna removida no Gradle9.6.
- Gradle ajustado para 9.5.0, versão anterior à remoção e dentro da faixa plenamente suportada pelo KGP 2.4.21. AGP8.13.2 permanece sem alteração.
- A migração para AGP9.x permanece separada, pois exige adotar o modelo built-in Kotlin/Android Gradle Plugin 9 e rever a aplicação do plugin Kotlin.
- CI da combinação Gradle9.5.0 + KGP2.4.21 + AGP8.13.2 ainda precisa concluir.


## 04/11/2026 — Validação final da segunda rodada (run #213)

- Pull Request CI #213: **concluído com sucesso** no HEAD `246bc75fb3a23c69b25813fdd0291e51432a587f`.
- Gradle 9.5.0 + Kotlin Gradle Plugin 2.4.21 + Android Gradle Plugin 8.13.2 aprovados em Lint, testes Kotlin, build diagnostic/companion, guardrails de dependência/permissão, auditoria dos manifestos mesclados, verificação APK Signature Scheme v2 e publicação.
- Artefato publicado: `relogio-2.0-diagnostic-debug-apk`, versão `0.4.0-alpha.3-diagnostic`; BUILD-INFO registra source_commit `246bc75fb3a23d6327a2c74acd69915263bdff16` e tested_commit `7b201544f5423ebd2f6133009584db4e88b7cf42`.
- SHA-256 do APK diagnostic: `27e4bcc623be0e76c29ccf7f16dbf33ff7be0e5ade22e2fdc200adee9813f24b`; conferido contra SHA256SUMS. Assinatura v2 verificada.
- Lint final: 31 avisos na variante diagnostic e 38 na companion; nenhum erro. Permanecem avisos SetTextI18n (strings diretas sem recursos de localização) e OldTargetApi (targetSdk 36). UseKtx e UseRequiresApi foram removidos.
- targetSdk 36 mantido por requisito Play; API37 não foi ativada sem testes Android 17.
- Artefato é build debug, com certificado Android Debug e `release_ready=false`; não é release de produção.
- PR #9 continua aberta, draft e sem merge. Galaxy A06/Android16, instalação, TalkBack e comunicação física GT3 MAX: **não validados**.

## 04/11/2026 — Reconexão automática BLE do GT3 MAX

- Criada política Kotlin pura `BleReconnectPolicy`, com retentativas em 1, 2, 5, 10, 20 e 30 segundos, limitadas a 30 segundos; a sequência reinicia após conexão GATT bem-sucedida.
- A primeira conexão GATT confirmada habilita e persiste localmente `auto_reconnect_enabled`; a recuperação usa somente o endereço GT3 MAX configurado no aparelho.
- Varreduras de recuperação usam modo de baixa potência e duração menor que a busca manual. Resultados de outros endereços são ignorados durante a recuperação.
- Erros GATT, desconexões inesperadas, timeout de conexão e falha/não localização durante scan agendam nova tentativa quando a reconexão está habilitada.
- Ausência de permissão não dispara diálogo em loop: a recuperação pausa até o usuário restaurar a permissão e retornar ao app.
- O botão permite interromper a reconexão mesmo sem GATT ativo; a solicitação manual desativa e persiste a preferência desligada.
- Ao pausar a Activity, scans em andamento são interrompidos; ao retomar/reabrir o app, a identidade previamente conectada é procurada novamente.
- Nenhuma permissão, dependência, escrita proprietária, comando NUS RX, OTA/DFU, reset ou consulta de sensor foi adicionada.
- Limite explícito: não há garantia de reconexão contínua após force-stop ou enquanto o processo permanece suspenso em segundo plano; CompanionDeviceManager/serviço conectado exige uma fase separada.
- Teste físico no Galaxy A06/Android 16 com GT3 MAX Call: **não validado**. CI do HEAD final ainda precisa ser executado e inspecionado.

## 04/11/2026 — Autodescoberta passiva de funcionalidades

- Criado `FeatureDiscoveryEngine` separado do ciclo de vida BLE. Ele recebe somente o inventário GATT já retornado pelo Android e os eventos de notificação entregues ao callback existente.
- Perfis Bluetooth SIG são apresentados como encontrados, nunca como medições funcionais ou sensores validados apenas pela presença de UUID.
- Características proprietárias são classificadas pela propriedade observada: notificação encontrada, leitura não executada e escrita bloqueada. A descoberta não inscreve novos canais.
- NUS TX continua sendo o único canal de notificação ativo automaticamente, sujeito ao fingerprint GT3 MAX Call já existente. NUS RX e características proprietárias de escrita permanecem bloqueadas.
- O histórico local guarda inventário, UUIDs, propriedades, contagens, tamanho e horário da última notificação e identificadores do quadro AB. Não persiste payload HEX bruto no catálogo.
- O padrão AB 0x91/0x80 gera somente a hipótese de bateria; valores não são enviados ao Health Connect e dependem de comparação com Wearfit Pro.
- Criada tela `Autodescoberta BLE`, acessível pela tela principal e pelo catálogo de funções.
- O armazenamento recusa substituir JSON inválido ou schema futuro. Não foi adicionada permissão Android ou dependência externa.
- Testes unitários do motor de descoberta adicionados; execução local Gradle, CI/build e teste físico Galaxy A06/GT3 MAX: **não validados** nesta alteração.

## 04/11/2026 — Sincronização automática de hora via Bluetooth CTS

- Implementado encoder puro para o formato oficial Current Time (10 octetos), com ano little-endian, data/hora local do Android, dia da semana, Fractions256 e Adjust Reason de atualização por referência externa.
- A sincronização automática é tentada uma vez após a descoberta GATT da sessão, somente quando o serviço CTS 0x1805, a característica 0x2A2B e a propriedade GATT WRITE estão presentes.
- A escrita usa a fila GATT serial existente e aguarda o callback onCharacteristicWrite; não são usados comandos do fabricante, NUS RX ou características proprietárias.
- Quando CTS está ausente (como nas capturas GATT anteriores do GT3 MAX), o app informa que não sincronizou e não tenta um protocolo alternativo.
- O sucesso do callback GATT é descrito como confirmação da escrita no transporte; não comprova que firmware, RTC ou mostrador atualizaram a hora. A validação física permanece necessária.
- Adicionados testes unitários para o payload CTS e identificação do perfil no mecanismo de autodescoberta.
- Build/CI e teste físico Galaxy A06/Android 16 com GT3 MAX: **não validados** nesta fase.


## 04/11/2026 — Exploração GATT ativa e laboratório de experimentos GT3 MAX

- Ligado o planejador `SafeGattExplorationPlan` ao callback de descoberta de serviços. Após inventário GATT, o app executa leituras somente para características SIG explicitamente permitidas, uma por vez na `SerialGattOperationQueue`, aguardando callback de leitura.
- Características SIG com NOTIFY/INDICATE só recebem inscrição quando possuem CCCD padrão 0x2902. O canal proprietário NUS TX existente continua restrito ao fingerprint completo GT3 MAX Call.
- Excluídos explicitamente FE59/DFU, NUS RX, serviços/características proprietários desconhecidos e número de série (2A25) das operações automáticas. Nenhum comando proprietário é transmitido.
- Adicionados callbacks de leitura GATT compatíveis com Android API 33+ e caminho legado, com registro de status, UUID, tamanho e HEX no log de diagnóstico exportável manualmente.
- Tela Autodescoberta BLE ampliada com laboratório guiado: bateria, hora/data, temperatura, frequência cardíaca, SpO2, passos, sono e atividade. Ação manual do usuário é correlacionada com tráfego; não há comando automático de medição.
- Criado teste unitário do allowlist para leitura/notificação padrão, exclusão de DFU/proprietários/NUS RX e exigência de CCCD. Resultado de CI ainda **não validado**.
- Análise do firmware documentada: V1.07 e build SH0050... são metadados informados; binário específico não foi obtido. CTS 0x1805/2A2B e termômetro 0x1809/2A1C ausentes. Hora proprietária e temperatura seguem sem implementação até captura comparativa Wearfit Pro ou firmware correto.
- Teste físico no Galaxy A06/Android 16 com GT3 MAX Call, leitura real de sensores, correção de hora/data e identificação de temperatura: **não validados**.


## 04/11/2026 — Auditoria de continuidade e fechamento das lacunas da exploração GT3 MAX

- Revisão das alterações anteriores identificou que BlePacketFingerprintAnalyzer estava criado, mas sem chamada no fluxo de notificação. Ligado ao callback real; cada pacote agora registra tamanho, prefixo, SHA-256 truncado e repetição no log diagnóstico. Revisão posterior: o prefixo HEX fica oculto fora da captura consentida; tamanho, hash truncado e repetição permanecem disponíveis.
- O parser anterior aceitava exatamente um quadro AB completo por notificação. Criado WearfitAbStreamDecoder com reagrupamento de fragmentos, separação de quadros concatenados, ressincronização e buffer limitado. A interpretação semântica permanece somente para comandos documentados/candidatos.
- O plano GATT automático foi ampliado para mais perfis SIG: bateria, hora, frequência cardíaca, pressão, temperatura/pressão/umidade ambientais, oximetria, composição corporal e cadência. Nenhuma propriedade WRITE é usada pelo plano.
- Criado plano manual aprofundado para características proprietárias que anunciam READ e canais NOTIFY/INDICATE com CCCD. Exige confirmação explícita, limita a 24 operações por sessão e exclui FE59/DFU, NUS RX/TX, serial e identificadores de sistema; nenhuma escrita de payload proprietária é criada.
- Fila GATT recebeu watchdog de 8 segundos e correspondência de callback por serviço/característica/tipo, para que uma resposta atrasada não conclua outra operação.
- Interface ganhou ação explícita para exploração manual e melhor quebra/altura de botões. Health Connect mantém a atualização visual, mas o log não repete o mesmo total de passos a cada consulta.
- A análise continua sem firmware binário específico. Hora/data e temperatura não foram corrigidas por ausência de CTS e de serviço padrão de termômetro na captura; o caminho proprietário segue bloqueado até captura comparativa/artefato original.
- CI do HEAD desta auditoria e testes físicos no Galaxy A06/Android 16 com GT3 MAX Call: **não validados** até nova execução.


## 04/11/2026 — Ajuste de privacidade da exploração aprofundada

- Auditoria de escopo corrigiu o comportamento para não inscrever sensores de saúde automaticamente ao conectar. A exploração automática foi limitada a metadados básicos e bateria em perfis SIG com READ.
- Sensores padronizados (FC, pressão arterial, SpO2, termômetro e sensores ambientais) e canais proprietários só são lidos/inscritos após confirmação explícita do usuário pela ação “Explorar canais adicionais do GT3 MAX”.
- Exploração aprofundada continua limitada a 24 operações, somente READ e CCCD padrão; NUS RX, NUS TX nessa varredura, DFU/FE59 e identificadores seriais permanecem excluídos.
- CI #237 falhou antes da compilação porque o teste Python antigo exigia que não existisse callback `onCharacteristicRead`, incompatível com o novo fluxo de leitura GATT serializado. O contrato do teste foi atualizado para exigir callback e watchdog; novo resultado de CI para o HEAD posterior ainda não foi registrado.
- Nenhum teste físico, sincronização proprietária de hora/data ou leitura de temperatura corporal/clima foi confirmado nesta rodada.

## 04/11/2026 — Auditoria da CI #242 e correção do contrato de testes

- A execução Android CI #242 foi vinculada ao HEAD da PR (`72a64e9e84ed7dbcbde0c888b96ed0df9a83c56d`) e falhou em `testDiagnosticDebugUnitTest`; o restante do job não prosseguiu para guardrails nem publicação do APK.
- Evidências: os 31 testes Python passaram; Android Lint passou nas duas variantes; 75 testes Kotlin foram executados e apenas `standardPlanDetectsEnvironmentalTemperatureAndBloodPressure` falhou na asserção de quantidade.
- Causa: a característica SIG de temperatura ambiental simulada no teste anuncia simultaneamente `READ` e `NOTIFY`, portanto o planejador gera duas operações independentes para ela. A pressão arterial anuncia `INDICATE`, gerando uma terceira operação. O teste esperava incorretamente apenas duas.
- Corrigido o teste para verificar as três operações e os respectivos tipos, sem alterar a política BLE. Nenhum comando de escrita, acesso automático a sensores ou permissão foi acrescentado.
- Resultado do CI após a correção, build das variantes, auditoria dos APKs e publicação do artefato: **aguardando nova execução no novo HEAD**. Teste físico no Galaxy A06/Android 16 com GT3 MAX: **não validado**.
## 04/11/2026 — CI #243 aprovada e observador passivo de tráfego BLE

- CI #243 (`902839c4a0a540c630f707ae947e1eb7fa6106c4`) concluída com sucesso: testes de contrato Python, Android Lint nas duas variantes, testes unitários, builds diagnostic/companion, guardrails de privacidade/dependências, auditoria de manifestos/assinaturas e publicação do APK diagnostic.
- Artefato diagnostic publicado como `relogio-2.0-diagnostic-debug-apk`; relatório de validação também foi publicado. O APK é debug e não constitui release de produção.
- Implementado `PassiveBleObservationTracker`, limitado à sessão, para registrar sequência global, contagem por canal, bytes acumulados, intervalo entre notificações e quantidade de fingerprints distintos.
- O observador recebe somente UUIDs, tamanho, timestamp e fingerprint calculado. Não armazena payload BLE, não cria gravação persistente e não envia comandos ao relógio.
- Integrado ao callback de notificação existente; o diagnóstico local agora distingue cada evento e permite observar periodicidade e estabilidade de cada canal.
- Adicionados testes unitários para sequência, intervalos, contadores, limites de memória e reinício de sessão.
- A captura HEX já existente continua restrita ao relatório diagnóstico local/exportação manual; não foi adicionada transmissão de dados ou coleta em rede.
- O próximo passo é comparar uma sessão sem Wearfit Pro com uma sessão iniciada pelo Wearfit Pro, mantendo a observação passiva e correlacionando horários. Nenhum protocolo proprietário, semântica de sensor ou bateria foi promovido a validado.
- A CI dos novos commits deste observador ainda precisa executar. Teste físico no Galaxy A06/Android 16 com GT3 MAX: **não validado nesta alteração**.
## 04/11/2026 — Simplificação do layout, contador de teste e biblioteca local

- Revisão de código encontrou excesso de ações no mesmo fluxo vertical: conexão, passos, notificações, autodescoberta, exploração manual, catálogo de funções, privacidade e exportação/log. O roteiro de experimentos também era exibido junto ao inventário, sem hierarquia clara.
- Navegação reorganizada em “Laboratório e registros” e “Mais recursos”; atalho para biblioteca local adicionado. Diagnóstico técnico e exportação TXT foram agrupados e recolhidos por padrão.
- Laboratório ganhou contador regressivo com opções de 1, 2, 5 e 10 minutos, progresso, quantidade de leituras/notificações e encerramento manual/automático. O prazo da sessão é persistido; voltar ao painel não reinicia o contador.
- Criada biblioteca interna em arquivo privado do app, codec binário schema 1 e gravação com AtomicFile. O limite é 1.000 registros e 16 MiB; payload HEX por registro fica limitado a 1.024 bytes.
- Inventários GATT são salvos como descobertas. Leituras e notificações com HEX só são armazenadas enquanto uma sessão de captura, iniciada após confirmação, estiver ativa. Os dados não saem do dispositivo e não são escritos no Health Connect.
- Adicionadas telas de pesquisa/filtros e testes unitários do codec, do schema futuro/corrompido e do cálculo do contador.
- Não foi adicionada permissão, dependência externa, serviço Charming remoto ou chamada de rede. A implementação não adiciona criptografia própria ao arquivo local.
- CI #248 foi enfileirada antes desta implementação e não valida os commits novos. Compilação, lint, testes, auditoria dos APKs, screenshot em emulador/aparelho e validação física: **aguardando CI/testes**.
- MagicPath indisponível por cota de chamadas externas esgotada; ADB/emulador não disponível no ambiente de execução. A revisão do layout foi estrutural, pelo código, e não uma auditoria visual baseada em screenshots.

### Registro técnico da implementação — 04/11/2026

- Commit de implementação: `2725d61c59eb45622ea50b8669570347fa36127c`.
- O contador utiliza o prazo absoluto da sessão e não reinicia quando a tela é fechada; a tela mostra 00:00 e o resumo quando a duração termina.
- O contador apenas acompanha a sessão. A captura BLE continua dependendo dos callbacks e operações GATT já autorizados.
- O inventário GATT e os quadros AB reconhecidos geram registros de descoberta. HEX de notificações e leituras GATT só é guardado durante sessão de captura confirmada.
- A interface da biblioteca tem busca e filtros; os registros ficam privados no aparelho e não são sincronizados externamente.
- A CI precisa validar o HEAD com esta entrega; o run #248 foi criado antes da implementação e não deve ser usado como prova de validação desta fase.

- O inventário GATT extenso foi colocado em painel recolhido por padrão, evitando que UUIDs e propriedades técnicas dominem a tela principal.

- Corrigido o estado visual do contador: o resumo `00:00` só aparece quando a sessão recém-encerrada corresponde ao identificador ativo da tela. Ao preparar a próxima sessão, volta a aparecer a duração selecionada.

- Ajustada a ordem da página principal: conexão e identidade, laboratório/biblioteca, depois Saúde e integração e, por fim, mais recursos. Isso prioriza a tarefa BLE sem remover os dados agregados do Health Connect.

- Adicionado ao painel principal um indicador de captura BLE ativa com contador regressivo e quantidade de registros. O indicador é atualizado pelo ticker da tela; a validade da captura continua protegida pelo deadline persistido no armazenamento.

### Correção de compilação da biblioteca interna

- A CI #253 no commit `cd82b8b3b1a62f826f6eef4091464887e7fcfa73` identificou erro Kotlin em `DiscoveryLibraryActivity.kt`: `EditText` não expõe a propriedade gravável `singleLine` usada no inicializador.
- Corrigido para a chamada pública `setSingleLine(true)`. A causa foi sintática e não envolveu BLE, persistência ou layout.
- A CI #253 foi cancelada pela concorrência após identificar a mesma falha nos dois variants. O HEAD final precisará ser recompilado integralmente.


## Auditoria integral e endurecimento — 04/11/2026

- Criada a branch `audit/full-repository-hardening-20261104` a partir do HEAD de trabalho `21e44dd5bace9801b7ced7183ff15b2ca730d588`. Nenhum PR foi criado e nenhum merge foi executado.
- O inventário GATT via SharedPreferences passou a validar estrutura JSON completa e estrita, schema, tipos numéricos, timestamps, cardinalidades, IDs duplicados, UUIDs e faixas de valores. JSON inválido não é convertido em inventário vazio nem sobrescrito.
- O inventário serializa operações entre instâncias e atualiza o SharedPreferences em memória com `apply()`; a gravação em disco é assíncrona e o retorno não confirma durabilidade imediata.
- A biblioteca de descobertas conserva a escrita atômica, mas agora protege transações entre instâncias, verifica tamanho antes da leitura e valida registros e sessões antes de decodificar/gravar.
- Prazos inconsistentes, timestamps negativos, IDs duplicados, tipos desconhecidos e HEX inválido passam a ser recusados.
- O botão principal de busca fica desabilitado enquanto o usuário entregou a sessão ao Wearfit Pro; a retomada continua sendo uma ação explícita.
- O modelo de passos exige `receivedAt >= intervalEnd`; origem permanece exclusivamente `HEALTH_CONNECT_AGGREGATE`.
- Testes adicionados/estendidos: `FeatureDiscoverySnapshotPolicyTest`, `DiscoveryLibraryCodecTest`, `DiscoveryTestSessionTest`, `StepsSnapshotTest` e `BleExternalHandoffPolicyTest`.
- A CI não foi disparada nem reexecutada. Nenhum teste Kotlin, Lint, build APK, auditoria dos APKs ou validação física foi declarado aprovado nesta revisão.
- Pendências estruturais identificadas: `MainActivity.kt` permanece com responsabilidade excessiva (extração deverá ser incremental); o repositório ainda não inclui Gradle Wrapper; testes em aparelho/emulador e validação de aquisição BLE real continuam pendentes.
- O relatório detalhado está em `docs/AUDITORIA-REPOSITORIO-2026-11-04.md`.



## Ajuste de retenção e transparência de funções — 04/11/2026

- O catálogo de funções não apresenta mais o CTS como mecanismo que será automaticamente usado. A identificação 0x1805/0x2A2B é informativa; escrita física de hora permanece suspensa.
- Desativar captura de notificações (inclusive em consequência de revogação de acesso) limpa imediatamente as prévias em memória; teste de limpeza adicionado.
- README e tela de privacidade esclarecem retenção: HEX é capturado somente em sessão consentida, mas permanece no arquivo privado depois da sessão, limitado a 1.000 registros; não existe exclusão individual na UI. Remover os dados do app no Android apaga o arquivo.


## Barreira contra callbacks GATT tardios e retenção local — 04/11/2026

- As chaves de operações GATT que expiraram ou estavam em execução quando o Android notificou Service Changed passam a ser invalidadas até uma nova instância `BluetoothGatt`. A fila não repete aquela chave dentro da mesma conexão; evita que callback atrasado da operação antiga seja confundido com uma nova tentativa idêntica.
- Callbacks de notificação agora exigem que a referência GATT seja atual, a sessão esteja conectada e não esteja em encerramento. Notificações tardias após o início da desconexão são ignoradas.
- Quando a fila de persistência está cheia, o log informa separadamente que o READ ou valor decodificado foi recebido, mas não foi enfileirado no histórico. Isso evita sugerir gravação de HEX que não foi aceita.
- A retenção do HEX local foi explicitada na tela da biblioteca, na tela de privacidade e no README: captura apenas durante sessão consentida, retenção posterior até o limite de 1.000 registros, sem exclusão individual disponível na UI.
- Pausar a ponte de notificações limpa imediatamente os dados voláteis; novo teste cobre `NotificationCaptureStore.clear()`.
- Relatório diagnóstico mascara MAC e não exporta texto das notificações Android; o fingerprint passivo guarda metadados e hash truncado, sem reter payload bruto.


## Serialização final do consentimento de notificações — 04/11/2026

- A captura de prévias foi protegida contra uma corrida: callback onNotificationPosted podia checar consentimento, processar extras/label e só depois inserir; se o usuário pausasse nesse intervalo, uma prévia poderia reaparecer após o clear.
- Agora a inserção executa uma verificação final de captura ativa e pacote permitido dentro do mesmo lock usado nas mudanças de configuração e na limpeza.
- Alterar a allow-list remove também qualquer evento de pacote que não permaneça permitido.
- Testes unitários cobrem a barreira final de admissão permitida/negada.


## 05/11/2026 — Controle BLE exclusivo pelo Relógio 2.0

- Decisão operacional: remover a entrega da sessão BLE ao Wearfit Pro. O Relógio 2.0 passa a manter o controle das buscas, conexões GATT, callbacks, recuperação e redescoberta de serviços.
- Removido o botão de handoff e os estados/bloqueios associados na tela principal, nos caminhos de scan/conexão, no timeout GATT, no callback de conexão, no tratamento de Service Changed e na reconexão automática.
- Na inicialização, a preferência antiga `wearfit_handoff_active` é apagada para que usuários que atualizaram a versão não permaneçam com o app bloqueado no modo de liberação.
- A ação de conexão do usuário agora habilita a política de reconexão progressiva antes da primeira conexão bem-sucedida. Desconexão manual continua desligando a recuperação.
- Removidos a política de handoff externa e seus testes. Atualizados os testes de reconexão e redescoberta GATT.
- O aplicativo não possui privilégio Android para desativar, encerrar ou revogar permissões de outro pacote. Se Wearfit Pro ainda estiver conectado ao GT3 MAX, a desconexão/força de parada deverá ser feita manualmente nas configurações do Android.
- Testes Kotlin, Lint, build, CI e validação física no Galaxy A06 / Android 16 com GT3 MAX: **não validados**. Nenhuma CI foi disparada e nenhuma PR foi criada.

## 04/11/2026 — Auditoria de obsolescência e conformidade dos decoders GATT

- Revisão executada na branch `work/disable-wearfit-app-ownership-20261105`, HEAD de trabalho após as alterações desta rodada: os artefatos e políticas de handoff removidos não devem ser tratados como funcionalidade atual.
- O documento `docs/COMPARTILHAMENTO-CONEXAO-WEARFIT-PRO.md` foi marcado como histórico/obsoleto. Seus passos de “Liberar conexão” e “Retomar conexão” não se aplicam ao app atual.
- Os parsers `WearfitAbFrameParser` e `WearfitAbStreamDecoder` foram mantidos: eles tratam framing BLE/NUS candidato observado no relógio; não são integração operacional com o aplicativo Wearfit Pro nem justificam manter o handoff.
- Defeito encontrado em `GattValueDecoder`: a característica Bluetooth SIG Pressure (0x2A6D) estava sendo interpretada como SFLOAT de 2 bytes, embora seja `uint32` de 4 bytes com resolução de 0,1 Pa. A característica Temperature (0x2A6E) estava sendo interpretada como SFLOAT; o contrato define `sint16` de 2 bytes, resolução de 0,01 °C, faixa de -273,15 °C a 327,67 °C e `0x8000` como “valor desconhecido”.
- Corrigidos os decoders 0x2A6D/0x2A6E e substituídos os testes que reproduziam o formato incorreto. A decodificação de umidade 0x2A6F foi mantida (uint16 em centésimos de %).
- Fonte técnica: Bluetooth SIG, *GATT Specification Supplement*, versão datada de 2026-02-05, tabelas das características Pressure e Temperature: https://btprodspecificationrefs.blob.core.windows.net/gatt-specification-supplement/GATT_Specification_Supplement.pdf
- Ponto legado ainda pendente: `syncClockUsingCurrentTimeService()`, `BluetoothCurrentTimePayload` e o callback de escrita CTS permanecem como implementação preparada, mas a rotina não possui chamada no fluxo atual. O callback de descoberta registra que a sincronização está suspensa. Portanto, a sincronização de hora do GT3 MAX **não está implementada/validada operacionalmente**; nenhum sucesso foi presumido.
- Dívida estrutural mantida: `MainActivity.kt` ainda concentra UI, permissões, scan, conexão, callbacks, fila, reconexão e exploração GATT. A extração deve ser incremental, sem reescrita ampla nem mudança de protocolo durante esta auditoria.
- Não removidos os documentos de pesquisa Wearfit Pro, o catálogo de funções, o plano de substituição ou o perfil/parser AB: são referência histórica/técnica e não código de handoff ativo.
- Testes Kotlin, Lint e builds **não executados**; CI **não disparada**; validação física no Galaxy A06 / GT3 MAX **não realizada**; PR e merge **não criados/executados** nesta rodada.
- Limpeza confirmada: removidas de `SafeGattExplorationPlan` duas constantes NUS RX/TX que não eram referenciadas; a constante NUS Service foi mantida porque exclui corretamente esse serviço da exploração genérica.
- Simplificação: removida a variável redundante `displayed` no decoder de Temperature Measurement; o valor MEDFLOAT permanece inalterado e a unidade continua dependendo do flag Celsius/Fahrenheit.
- O campo Pressure 0x2A6D não recebeu filtro arbitrário para zero: a tabela consultada define uint32/resolução, mas não declara sentinela de valor desconhecido; o valor zero é decodificado como 0,0 Pa. Teste correspondente foi incluído.

## 04/11/2026 — Revisão complementar de persistência e variante companion

- A persistência versionada do inventário GATT (`FeatureDiscoveryStore`) e a biblioteca local com `AtomicFile` (`DiscoveryLibraryStore`) continuam ativas e protegidas contra schema futuro/corrompido; não foram identificados arquivos de armazenamento obsoletos que devam ser removidos.
- A biblioteca limita registros, tamanho do arquivo e HEX; a fila serial permite descarregar gravações pendentes. A revisão foi estática, sem testes de recuperação de processo ou falha de disco nesta rodada.
- A variante `companion` mantém o serviço de captura de notificações com permissão `BIND_NOTIFICATION_LISTENER_SERVICE`, allow-list, limite de 30 prévias e retenção de 15 minutos; as prévias são limpas quando o listener é desconectado/destruído. A variante `diagnostic` usa uma tela substituta sem serviço de captura. As duas classes `NotificationCenterActivity` são específicas de flavors e não são duplicação removível.
- O serviço de notificação permanece com `android:exported="false"` e a permissão de binding protegida, alinhado ao exemplo oficial do Android. Nenhuma alteração de manifesto foi necessária.
- A estrutura BLE ainda concentra responsabilidades na `MainActivity`; essa dívida foi mantida como extração incremental, não tratada como código obsoleto. O helper CTS permanece dormente para não habilitar escrita de hora sem confirmação do GT3 MAX.

## Extração incremental do coordenador de reconexão BLE

- Criado `device/ble/core/BleReconnectCoordinator.kt`, Kotlin puro, para decidir se a recuperação está desativada, deve aguardar o primeiro plano, está bloqueada por sessão GATT/scan ativo, deve agendar imediatamente/com backoff ou pode iniciar scan após validar permissões.
- `MainActivity.scheduleAutoReconnect()` delega as decisões ao coordenador. A Activity ainda possui e controla `Handler`, `Runnable`, timers, estado de conexão e chamadas Android; o transporte BLE não foi movido nesta etapa.
- A verificação de permissão é passada como lambda para não ser executada quando primeiro plano, conexão ou scan já bloqueiam a tentativa.
- Adicionado `BleReconnectCoordinatorTest.kt` com cenários de política desativada, pausa/retomada, GATT ativo, scan ativo, tentativa imediata, preservação do backoff e validação tardia das permissões.
- Esta etapa preserva os intervalos existentes de 1, 2, 5, 10, 20 e 30 segundos e o cancelamento manual da reconexão.
- Os testes foram adicionados, mas **não executados**. Lint/build e validação física **não validados**. CI não disparada; nenhuma PR criada; nenhum merge.


## 04/11/2026 — Sincronização manual e segura do horário via CTS

- A tela principal ganhou a ação **Sincronizar hora do relógio**, inicialmente desabilitada.
- Após a descoberta GATT, o app verifica o serviço Bluetooth SIG Current Time Service (0x1805), a característica Current Time (0x2A2B) e a propriedade WRITE. O botão só é habilitado quando os três requisitos são atendidos na sessão GATT atual.
- A escrita continua manual: nenhuma hora é enviada ao conectar ou ao redescobrir serviços. O usuário precisa tocar no botão.
- A tentativa usa o payload CTS de 10 bytes já coberto por testes unitários e a fila GATT serializada. É permitida uma tentativa por sessão; reconectar libera uma nova tentativa.
- Se CTS/Current Time estiver ausente ou sem WRITE, a UI informa o motivo e mantém a investigação passiva. Nenhum comando proprietário, NUS RX, OTA/DFU ou fallback desconhecido é enviado.
- O callback GATT de sucesso é descrito como confirmação da escrita no transporte. Não é tratado como prova de que o firmware atualizou o RTC ou o mostrador.
- Extraída a política pura `BluetoothCurrentTimeCapabilityPolicy`, com cinco testes para sessão desconectada, CTS ausente, característica ausente, WRITE ausente e elegibilidade completa. A ação manual também revalida a identidade/estado da sessão GATT imediatamente antes de enfileirar a escrita.
- A captura GATT disponível de 03/11/2026 não confirmou CTS no GT3 MAX. Até uma nova captura física mostrar `0x1805/0x2A2B` com WRITE, espera-se que o botão permaneça desabilitado; a implementação não presume compatibilidade.
- README e arquitetura foram atualizados para refletir a nova fronteira de segurança e remover a afirmação antiga de que toda escrita de característica está suspensa.
- Implementação no commit `8a4c397ca25122f966c7920015ec8d7952ebaef9`; documentação nos commits subsequentes da mesma branch.
- Testes Kotlin, Lint, build Android e teste físico no Galaxy A06/Android 16 com GT3 MAX: **não executados/não validados nesta etapa**. CI não disparada; PR e merge não criados.
