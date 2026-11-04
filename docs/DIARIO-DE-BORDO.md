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
