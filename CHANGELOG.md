## Não lançado — remoção integral da dependência de aplicativo companheiro — 05/11/2026

- Confirmado o fluxo exclusivo GT3 MAX → BLE/GATT → Relógio 2.0; nenhuma conexão, pareamento compartilhado, sincronização, handoff, SDK ou integração com aplicativo externo.
- Renomeados os componentes de framing NUS/AB para `Gt3MaxProtocolProfile`, `Gt3MaxAbFrame`, `Gt3MaxAbFrameParser` e `Gt3MaxAbStreamDecoder`. O parser permanece restrito ao framing observado no GT3 MAX; a hipótese de bateria continua não validada.
- Removida a limpeza/migração de estado legado de handoff do código de inicialização. Não há mais chave de preferência ou política de liberação no fluxo ativo.
- Removidos do índice e do repositório os documentos específicos de pesquisa, catálogo, substituição, handoff e integração de terceiros do aplicativo externo.
- Documentação ativa do README, arquitetura, perfil do relógio, identificação, protocolo e mapa de módulos alinhada à operação independente.
- Testes, Lint, builds, auditoria de APK e validação física: **aguardando CI e teste no Galaxy A06 / Android 16 + GT3 MAXX**.

## 0.4.0-alpha.4 — 05/11/2026

### Hora e clima independentes do aplicativo companheiro externo
- Adicionada tela Hora e clima com hora civil do Android e indicação das opções de data/hora automática e fuso automático do sistema.
- Adicionada geocodificação manual de cidades e previsão Open-Meteo (condição atual, sensação térmica, umidade, precipitação, probabilidade de chuva, vento, mín./máx. e nascer/pôr do sol).
- Cache meteorológico local privado; atualização ao abrir quando o cache excede 30 minutos, atualização manual e agendamento periódico aproximado de seis horas por WorkManager com rede disponível.
- Nenhuma permissão de GPS/localização foi adicionada. A consulta envia somente o nome digitado da cidade e as coordenadas da cidade selecionada; nenhum identificador, dado de saúde ou conteúdo de notificação é enviado.
- Manifesto passa a declarar INTERNET e ACCESS_NETWORK_STATE para a função meteorológica. O auditor exige exatamente uma INTERNET nas duas variantes; os demais bloqueios de permissões sensíveis são preservados.
- Sincronização da hora Android pelo CTS passa a ser automática uma vez por sessão, mas somente se 0x1805/0x2A2B e propriedade WRITE forem anunciados. O GT3 MAXX capturado não anuncia CTS, portanto a sincronização física permanece não validada.
- Nenhum payload de clima foi enviado ao GT3 MAXX: o protocolo de clima do firmware ainda não foi identificado.
- Open-Meteo: uso gratuito não comercial e dados CC BY 4.0; atribuição incluída na tela. Distribuição comercial exige revisão dos termos.
- Teste unitário de rótulos meteorológicos adicionado; auditor de manifesto ganhou cenários de INTERNET única, ausente e duplicada.
- Build, Lint, testes Kotlin, guardrails, APK e teste físico Galaxy A06/GT3 MAXX: **não validados** nesta alteração; CI não foi disparada.
## Não lançado — corrigir contrato do teste de assinatura GATT — 05/11/2026

- O run #419 confirmou que os pacotes reorganizados compilam e o Lint passa nas variantes diagnostic/companion.
- O teste `FeatureDiscoveryEngineTest.inventoryStoresStructuralGattSignatureWithoutDeviceIdentifiers` falhava porque procurava a palavra “MAC” e “serial” na evidência, embora a implementação use essas palavras para documentar explicitamente que os valores foram excluídos.
- Ajustada a asserção para exigir o aviso de privacidade e rejeitar padrões concretos de endereço MAC e serial, em vez de rejeitar os nomes das categorias.
- O teste mantém o contrato: a assinatura estrutural contém somente UUIDs, propriedades e descritores GATT; não recebe endereço, serial ou payload.
- A CI do run #419 ficou vermelha por essa única falha unitária. Build APK e auditoria de manifestos/assinaturas foram ignorados; somente `android-validation-reports` foi publicado.
- Nova CI será executada na atualização desta PR. Nenhum APK foi disponibilizado nesta execução.

## Não lançado — organização de pacotes e fronteiras Android — 05/11/2026

- Movidas as Activities para `ui/screens`, utilitários BLE puros para `device/ble/core`, relatório para `domain/diagnostics` e formatador de passos para `ui/formatters`.
- Atualizados os nomes completos das Activities no Manifesto, os imports Kotlin, testes de helpers e o teste de contrato que inspeciona o caminho de `MainActivity`.
- Adicionado `.gitignore` para artefatos Gradle/Android, configuração local, chaves de assinatura, arquivos de ambiente, logs e saídas de teste.
- Documentadas as fronteiras reais de pacotes e a dívida restante: `MainActivity` ainda coordena grande parte do ciclo BLE/GATT.
- Nenhuma alteração intencional no comportamento BLE, nos comandos CTS, nos decoders, na persistência ou no Health Connect.
- Testes unitários/Kotlin, lint e build: **não executados** nesta etapa. CI: **não disparada**. Validação física no Galaxy A06 / GT3 MAX: **não validada**.

## Não lançado — matriz de recursos e assinatura estrutural do GT3 MAX — 05/11/2026

- Adicionada matriz passiva de onze grupos Bluetooth SIG para bateria, identificação do dispositivo, hora CTS, frequência cardíaca, pressão, SpO₂, temperatura, ambiente, composição corporal e cadência.
- A matriz diferencia serviço não anunciado, serviço sem característica esperada, READ, NOTIFY/INDICATE com CCCD e tráfego observado. A ausência de UUID padrão não é interpretada como ausência física do sensor.
- Adicionada assinatura estrutural GATT SHA-256 com UUIDs de serviços/características/descritores e propriedades; não inclui MAC, serial, valores do fabricante ou payload de saúde.
- O motor de descoberta compara inventários anteriores e registra baseline, estrutura inalterada ou estrutura alterada sem afirmar atualização de firmware.
- A tela de descobertas exibe a matriz separadamente de outras evidências e mantém dados semânticos como não validados.
- Adicionados testes unitários para a matriz, assinatura e integração ao motor; **não executados** nesta rodada.
- Criado `docs/ESTUDO-FIRMWARE-GT3-MAXX.md` com evidências, prioridades, limitações e roteiro de validação física.
- Build, Lint, testes Kotlin, CI e teste físico: **não validados**. Nenhuma CI foi disparada; nenhuma PR foi criada e nenhum merge foi realizado.

## Não lançado — controle BLE exclusivo pelo Relógio 2.0 — 05/11/2026

- Removido da interface o botão de handoff/liberação para aplicativo companheiro externo; o app não pausa mais sua conexão para entregar o relógio a outro aplicativo.
- Removidos os bloqueios de busca, conexão, callbacks GATT, Service Changed e reconexão que dependiam do estado de handoff.
- A preferência antiga `estado legado de handoff` é apagada na inicialização, evitando que instalações atualizadas permaneçam presas no estado de liberação.
- Uma solicitação explícita de conexão habilita a política de reconexão progressiva antes mesmo da primeira conexão bem-sucedida; desconexão manual continua desabilitando as tentativas.
- Excluídos `BleExternalHandoffPolicy` e seus testes obsoletos; atualizados testes da política de reconexão e do callback Service Changed.
- O app não pode desativar/encerrar outro pacote Android. Se aplicativo companheiro externo ainda mantiver GATT ativo, o usuário deverá encerrar essa conexão ou forçar a parada manualmente.
- Testes Kotlin, Lint, build, CI e teste físico Galaxy A06 / GT3 MAX: **não validados nesta alteração**. Nenhuma CI foi disparada.

## Não lançado — exclusão individual do histórico BLE — 04/11/2026

- A biblioteca agora permite excluir cada registro local individualmente, incluindo o HEX capturado, com confirmação explícita.
- A remoção é serializada na fila de escrita e persiste por substituição atômica do snapshot; outros registros e o estado da sessão ativa são preservados.
- Testes unitários cobrem remoção exata, preservação da sessão e registro inexistente.
- Testes, lint, build e validação física: **não executados/não validados**. CI não disparada por solicitação do usuário.
## Não lançado — ferramentas de diagnóstico, integridade e desempenho — 06/11/2026

- LeakCanary 2.14 adicionado exclusivamente à variante `diagnosticDebug`; guardrails impedem a presença em variantes companion, release e benchmark.
- Criada a variante não depurável `diagnosticBenchmark`, com profiling via shell restrito ao manifesto dessa variante e ProfileInstaller 1.4.1.
- Adicionado módulo `:macrobenchmark` com medição de inicialização fria (StartupTimingMetric, 10 iterações). A CI compila o módulo; a medição física continua pendente.
- Configurado Renovate para atualizar dependências Gradle, plugins e GitHub Actions, com cooldown, Dependency Dashboard e automerge desativado. A ativação do bot depende de habilitação no GitHub.
- Incluído bootstrap de Gradle Dependency Verification SHA-256: a CI gera um artefato temporário que precisa ser revisado antes de entrar no repositório. A verificação persistente ainda depende do versionamento desse arquivo.
- Nenhuma alteração no protocolo BLE/GATT, permissões Bluetooth, coleta de saúde ou independência do WearFit Pro.

# Changelog

## Não lançado — serialização do consentimento de notificações — 04/11/2026

- A gravação de prévias agora revalida consentimento e allow-list sob o mesmo lock usado para pausar captura ou alterar aplicativos permitidos.
- Callbacks de notificação já iniciados não podem repopular a memória depois da pausa/revogação.
- Remover um aplicativo da lista permitida também purga prévias já capturadas desse pacote.
- Testes cobrem admissão negada e permitida pela barreira final.


## Não lançado — retenção explícita de histórico BLE e limpeza de prévias — 04/11/2026

- A tela de funções não promete sincronização CTS: a característica pode aparecer no inventário, mas a escrita de hora continua suspensa e não validada.
- Pausar a captura de notificações ou desativá-la após revogação de acesso limpa imediatamente as prévias em memória. Callbacks de notificação recebidos após desconexão/início do encerramento são ignorados. O limite de 30 eventos e a expiração de 15 minutos permanecem.
- A documentação agora distingue claramente captura consentida de retenção posterior do histórico BLE: registros HEX já capturados permanecem no armazenamento privado local, até o limite de 1.000 registros; não há exclusão individual na UI atual.
- Teste do armazenamento volátil de notificações adicionado/atualizado.


## Não lançado — integridade de armazenamento, sessões e handoff BLE — 04/11/2026

- O inventário GATT agora rejeita campos ausentes, tipos incorretos, valores fora de faixa, arrays incompletos, duplicatas e JSON acima do limite; dados corrompidos ou de schema futuro permanecem preservados.
- As operações de leitura-modificação-gravação do inventário usam bloqueio compartilhado entre instâncias; a atualização em memória usa `apply()` e a gravação em disco é assíncrona, sem confirmação imediata de durabilidade.
- A biblioteca binária usa bloqueio compartilhado entre telas, limita leitura antes de alocar o arquivo, valida prazos de sessões, identificadores, tipos e HEX, e recusa timestamps inválidos.
- A interface não oferece busca BLE enquanto o controle estiver liberado para aplicativo companheiro externo.
- O snapshot de passos exige que o recebimento ocorra após o fim do intervalo consultado.
- Foram adicionados testes de regressão para integridade do snapshot, sessão, codec, intervalo de passos e bloqueio do handoff.
- Nenhuma escrita proprietária, NUS RX, CTS write, OTA, reset, calibração ou publicação Health Connect foi habilitada.
- CI, build local, lint e teste físico Galaxy A06 / GT3 MAX / aplicativo companheiro externo: **não validados** (CI não disparada por solicitação do usuário).


## Não lançado — liberação BLE para aplicativo companheiro externo — 04/11/2026

- Adicionada ação explícita para liberar o GT3 MAX ao aplicativo companheiro externo: encerra a sessão GATT própria, cancela scans/tentativas e persiste a pausa da reconexão automática.
- A liberação sobrevive à reabertura do Relógio 2.0; a reconexão só é retomada por ação manual do usuário.
- Adicionados testes unitários para impedir reconexão e descoberta inicial durante a liberação.
- Nenhuma escrita GATT, comando proprietário, alteração de pareamento Android ou controle Bluetooth Classic foi introduzido.
- CI desta branch e teste físico Galaxy A06 / GT3 MAX / aplicativo companheiro externo: **não validados**.

## Não lançado — watchdog e rastreabilidade da descoberta GATT — 04/11/2026

- Persistido o estado de solicitação, callback concluído, falha ou timeout da descoberta de serviços GATT, evitando confundir inventário antigo com a conexão atual.
- Adicionado watchdog de 12 segundos. Recusa da API, erro de callback ou ausência de callback encerram a conexão GATT de forma controlada e liberam a política de reconexão automática.
- Filas e inscrições pendentes são limpas; operações não executadas permanecem distintas de falhas de sensores.
- Desconexão manual ou encerramento do aplicativo antes do callback registra uma interrupção explícita, em vez de deixar somente o último inventário persistido.
- Teste unitário cobre a decisão de recuperação do watchdog.
- CI e teste físico no Galaxy A06 / GT3 MAX: **não validados nesta revisão**.


## Não lançado — ocultação do prefixo HEX fora de captura — 04/11/2026

- O log do fingerprint BLE não exibe mais os quatro primeiros bytes do payload fora de uma sessão consentida.
- Tamanho, SHA-256 truncado e repetição permanecem disponíveis; HEX completo e prefixo só aparecem durante captura autorizada.
- Documento técnico e diário de bordo atualizados.
- CI e teste físico: **não validados nesta revisão**.

## Não lançado — precisão dos contadores do progresso GATT — 04/11/2026

- Itens interrompidos/deixados na fila agora são contados como não executados, separados das operações finalizadas com resposta, confirmação, falha ou timeout.
- Percentual indica operações tratadas e não taxa de sucesso do sensor.
- Testes de regressão adicionados para interrupção por desconexão.

## Não lançado — prevenção de leitura GATT redundante — 04/11/2026

- Removida a repetição de READ da bateria 0x2A19: a leitura continua no plano básico e o aprofundamento mantém somente a inscrição CCCD opcional.
- Removida duplicação da entrada Body Composition Feature 0x2A9B no catálogo interno.
- Operações READ canceladas antes do início e características que desapareçam do inventário são classificadas como não executadas no progresso, não como falha do sensor.
- README, documento técnico, diário e teste de regressão atualizados.
- CI e validação física no GT3 MAX/Galaxy A06: **não validadas nesta revisão**.

## Não lançado — painel de progresso e correlação temporal BLE — 04/11/2026

- Adicionado painel na tela principal com progresso da exploração por operação READ/CCCD, incluindo concluídas, respondidas, inscrições confirmadas, notificações, decodificações, falhas e timeouts.
- Canais são classificados somente conforme evidências observadas; inscrição sem tráfego fica como "inscrito, aguardando dados", sem inferir defeito ou ausência do sensor.
- Desconexão durante a exploração classifica itens pendentes como não executados.
- Adicionado marcador temporal manual para ações realizadas no relógio; os eventos são incluídos no TXT como marcadores de usuário, separados de medições e dados do sensor.
- Testes de unidade adicionados para estados e contadores do progresso GATT.
- CI e teste físico no GT3 MAX/Galaxy A06: **não validados nesta revisão**.


## Não lançado — rastreabilidade de valores decodificados — 04/11/2026

- Registros decodificados por READ/NOTIFY Bluetooth SIG agora recebem o `sessionId` da captura ativa. Valores de sensores deixam de ser gravados como descobertas sem vínculo à sessão.
- Sem sessão consentida válida, o método de persistência de valores decodificados recusa a gravação; sessões vencidas são finalizadas sem aceitar novos valores.
- Teste de codec adicionado para preservar o vínculo entre descoberta decodificada e sessão durante a serialização.
- CI e teste físico GT3 MAX/Galaxy A06: **não validados nesta revisão**.

## Não lançado — testes GATT automáticos e persistência de resultados — 04/11/2026

- Adicionado modo de testes automáticos com ativação local única. Ao conectar, o app exige o fingerprint GATT completo do GT3 MAX Call antes de iniciar exploração aprofundada.
- O executor agora dispara tanto operações Bluetooth SIG padronizadas quanto leituras proprietárias somente READ e inscrições NOTIFY/INDICATE com CCCD 0x2902. NUS RX, escritas de payload, DFU/OTA e reset continuam bloqueados.
- Ampliados os decoders Bluetooth SIG: bateria, Device Information, frequência cardíaca/localização do sensor, pressão arterial/intermediária/recursos, termômetro, pressão/temperatura/umidade ambientais, oximetria/recursos, RSC e CSC. Body Composition 2A9B informa capacidades/resoluções; 2A9C permanece em HEX sem conversão semântica nesta revisão.
- Decoders agora rejeitam flags reservadas e bytes extras fora dos layouts definidos, com regressões específicas para respostas truncadas/malformadas.
- A coleta automática distingue sessões que criou de sessões manuais reutilizadas: pausar o modo automático não encerra uma captura manual que já estava ativa.
- “Testes e descobertas” mostra se a automação está ativa e recarrega o inventário ao voltar ao primeiro plano; a biblioteca detalha serviço e característica.
- Leituras e notificações recebidas são registradas na biblioteca privada durante sessões consentidas de até 10 minutos, renovadas enquanto o modo automático permanece ativo e conectado. HEX fora da sessão não é incluído no log/exportação.
- Resultados distinguem solicitação, callback de leitura, inscrição CCCD e decodificação; formatos proprietários desconhecidos permanecem sem semântica.
- Pausar encerra captura e tenta cancelar inscrições automáticas por CCCD padrão. A leitura não força medições; o firmware precisa emitir os dados.
- Testes Kotlin adicionados para decoders, política de consentimento e campos de sensores; CI e teste físico GT3 MAX/Galaxy A06 aguardam validação.


## Não lançado — separação dos artefatos da CI — 04/11/2026

- Corrigido o empacotamento do GitHub Actions: o artefato `relogio-2.0-diagnostic-debug-apk` agora recebe diretamente somente `app-diagnostic-debug.apk`.
- Removidos do pacote instalável `BUILD-INFO.txt`, `SIGNATURE.txt` e `SHA256SUMS`; metadados, hash e certificado continuam disponíveis no artefato separado `android-validation-reports`.
- Mantidos exatamente dois artefatos, com papéis separados: um APK instalável e um pacote de evidências/relatórios técnicos.
- Concorrência CI alinhada pelo nome da branch para que eventos de push e atualização da mesma PR compartilhem o mesmo grupo e não mantenham duas compilações redundantes em paralelo.
- CI da correção: aguardando novo run no HEAD atualizado. APK ainda não validado nesta revisão.


- UI BLE: a tela Testes e descobertas ganhou atalho para solicitar exploração GATT aprofundada pelo painel principal, mantendo confirmação explícita.

- BLE: exploração aprofundada agora percorre todos os canais GATT elegíveis, sem o truncamento fixo anterior de 24 operações; somente READ e CCCD padrão após confirmação explícita.

## Não lançado — descoberta BLE geral e TXT completo — 04/11/2026

- Busca BLE geral iniciada automaticamente ao abrir o aplicativo; não exige digitar o endereço MAC.
- Endereço salvo deixou de ser filtro exclusivo: nome GT3 MAX e serviço NUS anunciado também são pistas; dispositivos sem nome são listados para validação manual.
- Nova identidade só é salva após fingerprint GATT completo; candidatos incompatíveis são desconectados antes de leituras ou inscrições.
- Sincronização CTS por escrita de característica suspensa; permanece somente leitura GATT e configuração padrão de CCCD para notificações/indicações.
- TXT inclui descobertas persistidas da biblioteca local, além dos 250 eventos mais recentes; endereços MAC continuam mascarados.
- Testes adicionados para classificação BLE, fingerprint sem nome e exportação das descobertas.
- CI e teste físico Galaxy A06/Android 16: **não validados**.

## Não lançado — laboratório de captura e biblioteca BLE — 04/11/2026

- Painel principal reorganizado para priorizar conexão GT3 MAX, laboratório/biblioteca e depois Saúde e integração.
- Inventário GATT completo e log técnico/exportação TXT recolhidos por padrão.
- Sessões de captura de 1, 2, 5 ou 10 minutos, com contagem regressiva, progresso e contagem de leituras/notificações; indicador de captura ativa também aparece no painel principal.
- Biblioteca local privada com busca e filtros para descobertas, leituras/notificações e sessões. Inventários GATT e quadros AB reconhecidos são registrados como descobertas.
- Payload HEX de leituras/notificações só é armazenado durante captura cronometrada confirmada; nenhum dado é enviado à rede ou escrito no Health Connect.
- Codec binário versionado com AtomicFile, limite de registros/arquivo e preservação de conteúdo corrompido ou schema futuro.
- Corrigida chamada EditText para setSingleLine(true) após erro de compilação localizado na CI #253.
- CI sobre o HEAD que contém todas estas mudanças: **ainda não validada**.

## Em desenvolvimento — 04/11/2026

- Exploração GATT automática de perfis Bluetooth SIG padronizados, sem executar escritas de payload.
- Exploração manual aprofundada de canais customizados somente READ e NOTIFY/INDICATE via CCCD, com confirmação, limite de operações e bloqueio de DFU/NUS RX.
- Fingerprints sobre notificações BLE e decodificador de quadros AB fragmentados/concatenados com buffer limitado.
- Watchdog e correlação de callbacks para operações GATT.
- Roteiro de experimentos para bateria, hora/data, temperatura, frequência cardíaca, SpO2, passos, sono e atividade.
- Deduplicação de linhas repetidas do Health Connect e ajuste de altura/quebra de texto dos botões.
- Hora/data proprietárias e temperatura do GT3 MAX continuam não validadas; firmware binário V1.07 específico ainda não foi obtido.
- CI e validação física desta rodada: **não validados**.

# Changelog

## Não lançado — recuperação explícita da permissão Health Connect

- Registrados os intents de justificativa de privacidade exigidos pela Conexão Saúde no Android 13 e anteriores e no Android 14+; o alias moderno é protegido por START_VIEW_PERMISSION_USAGE.
- Auditoria do manifesto APK e testes de política passam a verificar ambos os pontos de entrada.

- Atualizar atividade não solicita nem reabre permissões; sem acesso, exibe estado claro e mantém a leitura suspensa.
- Estado local diferencia primeira solicitação, recusa, concessão e revogação; a autorização efetiva sempre é reconferida no Health Connect.
- Após recusa ou revogação, a interface oferece acesso direto às configurações do sistema e nova solicitação somente mediante ação explícita.
- O retorno ao app revalida READ_STEPS e retoma a leitura agregada sem apresentar um novo diálogo.
- Adicionados testes unitários da política de transição dos estados de permissão.
- CI e teste físico Galaxy A06/Android 16: **não validados**.


## Não lançado — sincronização automática de hora via Bluetooth CTS

- Adicionado encoder do Current Time do Bluetooth SIG (CTS 0x1805 / característica 0x2A2B), com payload de 10 bytes e horário local do Android.
- Após cada descoberta GATT, a sincronização é tentada automaticamente uma vez por sessão, mas somente quando o serviço padrão existe e a característica anuncia WRITE.
- A escrita é serializada pela fila GATT existente e seu callback é apresentado na interface sem afirmar que o relógio físico atualizou o RTC/visor.
- CTS ausente ou sem WRITE resulta em estado pendente; nenhuma escrita proprietária, NUS RX, OTA, DFU ou reset é tentada.
- Autodescoberta passa a listar o serviço CTS e a capacidade de escrita para ajuste de hora.
- Testes do encoder e da identificação CTS adicionados. Build/CI e validação física: **não validados**.


## Não lançado — autodescoberta passiva de funcionalidades GT3 MAX

- Criado mecanismo de inventário GATT automático após a descoberta de serviços pelo Android.
- Catálogo identifica perfis Bluetooth SIG conhecidos (quando serviço e característica correspondem), canais proprietários, propriedades READ/NOTIFY/INDICATE/WRITE e tráfego efetivamente recebido.
- O catálogo diferencia perfil encontrado, tráfego observado, hipótese de protocolo e dado ainda não validado; UUID ou propriedade GATT não são tratados como prova de função de usuário.
- Notificações recebidas atualizam contadores e metadados. O conteúdo bruto HEX não é copiado para o histórico da autodescoberta.
- A interpretação AB 0x91/0x80 permanece como candidata de bateria até correlação com aplicativo companheiro externo; quadros desconhecidos não recebem semântica inventada.
- Nenhuma leitura arbitrária, nova inscrição de notificação, escrita proprietária, NUS RX, OTA/DFU ou reset foi habilitado.
- O catálogo local usa schema versionado e recusa sobrescrever conteúdo inválido ou de versão mais nova.
- Testes unitários do motor de descoberta adicionados. Build/CI e teste físico desta fase: **não validados**.


## Não lançado — reconexão automática BLE

- Após uma conexão GATT bem-sucedida, o app persiste localmente a opção de reconexão para a identidade GT3 MAX já configurada.
- Perdas inesperadas GATT e falhas de conexão/busca iniciam tentativas com intervalos de 1, 2, 5, 10, 20 e 30 segundos (limite de 30 s).
- Tentativas automáticas usam scan BLE de baixa potência e filtram o endereço salvo; não solicitam permissão automaticamente.
- A ação Parar reconexão cancela tarefas pendentes e desativa a preferência persistida.
- Recuperação fica limitada ao primeiro plano/retomada do app; recuperação após force-stop ou continuamente em segundo plano não está implementada.
- Preservados inventário GATT passivo, NUS TX Notify/CCCD e bloqueio de payload NUS RX/características desconhecidas.
- Testes físicos de desconexão/reconexão no Galaxy A06/Android 16 com GT3 MAX Call: **não validados** até execução no aparelho.


## Não lançado — atualização compatível do toolchain e limpeza Lint

- Atualizados Kotlin Gradle Plugin para 2.4.21 e Gradle para 9.5.0, mantendo Android Gradle Plugin 8.13.2 dentro da matriz oficial de compatibilidade.
- Substituído TargetApi por RequiresApi no helper de API33+ e aplicado SharedPreferences KTX para remover avisos locais.
- README passa a descrever somente a conexão ao endereço GT3 MAX configurado localmente.
- AGP9 não foi incluído por exigir migração separada da integração Kotlin. Gradle 9.7 foi rejeitado pela API interna removida no Gradle 9.6; ajustado para Gradle 9.5 com AGP8.13.2.
- CI da nova combinação ainda precisa concluir.


## Não lançado — auditoria do manifesto compilado

- Auditor de APK atualizado para aceitar referências de recursos compiladas (@ref) em dataExtractionRules/fullBackupContent, mantendo a exigência de ambos os atributos.
- Testes adicionados para a forma compilada e para ausência de cada política; testes de código-fonte preservam nomes e exclusões exatos.
- README corrigido para refletir conexão automática restrita ao endereço local configurado, sem seleção genérica manual.
- Run #207: Lint, testes, build e guardrails passaram; auditoria de APK falhou apenas no formato textual das referências de recursos. A nova execução deverá confirmar a correção e a publicação.


## Não lançado — segunda rodada de auditoria e correção de obsolescência

- Atualizadas AndroidX Activity KTX para 1.13.0 e coroutines Android para 1.11.0 após alertas de versão; Health Connect 1.1.0 foi mantido.
- Mantidas as permissões Bluetooth no elemento padrão uses-permission, com tools:targetApi=s no atributo neverForLocation; o auditor rejeita o elemento não suportado uses-permission-sdk-31.
- Adicionado ícone vetorial próprio e regras explícitas de exclusão para backup/extração em nuvem, transferência entre dispositivos e backup legado; allowBackup continua false.
- Removidos callbacks GATT de leitura e função handleRead que ficaram sem chamadores. O fluxo CCCD separa a chamada moderna API33+ do caminho Boolean legado.
- O artefato passa a identificar separadamente o SHA fonte da PR e o SHA efetivamente testado pelo GitHub Actions.
- README alinhado com identificação local do relógio, conexão automática, inventário GATT passivo e versão canônica VERSION.
- targetSdk 36 mantido por atender ao requisito atual do Google Play; API37 fica pendente de validação específica no Android 17.
- Testes de regressão dos contratos atuais adicionados; CI no novo HEAD ainda precisa concluir.


## Não lançado — auditoria modular e hardening

- Corrigida compatibilidade da janela de passos diários do Health Connect com Android API 26; adicionados testes para fusos horários com datas locais distintas.
- Removida da tela GATT a leitura e inscrição manual de características arbitrárias. A única inscrição automática disponível continua condicionada ao fingerprint completo do GT3 MAX Call e limitada à NUS TX via CCCD padrão.
- Metadados do APK diagnostic passam a ser derivados de `VERSION`, com verificação contra `versionName` do Gradle; removida a versão antiga fixa do `BUILD-INFO.txt`.
- Adicionados testes Python de consistência dos metadados do build e atualização do mapa de módulos/diário.
- Lint, CI no HEAD final e teste físico: **aguardando validação**.

## Não lançado — investigação do canal de parceiros aplicativo companheiro externo

- Analisada a declaração Weike atualizada em 04/08/2023: ela prevê acesso por aplicativos terceiros autorizados a identificador aleatório do dispositivo, estado, notificações e, mediante autorização expressa, dados de sensores, sono, exercício, uso, logs e localização.
- Documentada a distinção entre a declaração de privacidade e uma API/SDK: o texto não fornece artefato Android, contratos, callbacks, exemplos ou esquema de dados.
- Registrada a dúvida de elegibilidade: o canal descrito aplica-se a vestíveis Weike pareados; ainda não há confirmação de que o GT3 MAX Call esteja habilitado nesse programa.
- Prioridade dada à obtenção do SDK/API oficial e confirmação do modelo; sem leitura de armazenamento privado, credenciais, tráfego autenticado ou comandos BLE especulativos.
- Integração de parceiro ainda não implementada; CI e teste físico não validados por esta documentação.

## Não lançado — perfil de acesso GT3 MAX Call

- Adicionado perfil de dispositivo com metadados informados pelo proprietário: GT3 MAX Call, firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1. Esses campos não são apresentados como leitura autenticada do GATT.
- Compara o nome anunciado e o fingerprint completo dos serviços/características já capturados para identificar compatibilidade com a unidade.
- Após fingerprint completo, inscreve automaticamente somente a característica NUS TX em notificações, usando CCCD padrão; não envia payload à característica NUS RX.
- Exibe a interpretação AB 0x91/0x80 como bateria candidata, sem apresentar o valor como validado até correlação com aplicativo companheiro externo em diferentes níveis de carga.
- Adicionada referência de bateria candidata no catálogo de funções.
- Serial, código de pareamento e MAC não foram incluídos no código/documentação pública; a identidade Bluetooth continua configurada localmente no aplicativo.
- Nenhum sensor de saúde, comando proprietário, perfil Bluetooth Classic, OTA/DFU ou reset foi habilitado.
- CI e teste físico no Galaxy A06/GT3 MAX Call: **não validados**.

## Não lançado — fase de extração Health Connect e contrato para rotas

- Extraído o adaptador de leitura agregada de passos para data/healthconnect, retirando do MainActivity a dependência direta de HealthConnectClient/StepsRecord/AggregateRequest.
- Criado StepsSnapshot com origem e intervalo explícitos; zero real é distinto de ausência de registros.
- Adicionados testes para origem, intervalo, valores negativos e estado sem dados.
- Documentados os limites entre dados agregados do Health Connect, medições diretas do GT3 MAX, rotas GPS do telefone e rotas importadas do wearable.
- Nenhuma permissão de localização, Health Connect WRITE, sensor extra ou comando BLE foi adicionada.
- CI e teste físico desta fase: **não validados** até a execução e validação no aparelho.

## Não lançado — auditoria Play Protect e integridade dos APKs

- Inspecionado o APK diagnostic publicado no GitHub Actions: o ZIP corresponde ao SHA-256 do digest do GitHub e o manifesto binário não contém INTERNET, SMS, Accessibility ou Notification Listener.
- Reforçada a política local-first com `usesCleartextTraffic=false` e um auditor independente do manifesto mesclado, cobrindo pacote por variante, permissões sensíveis, localização legada, backup e isolamento do Notification Listener.
- CI passa a executar testes Python de política, auditoria de ambos os APKs via `apkanalyzer`, verificação criptográfica com `apksigner`, dependências runtime e SHA-256.
- Artefato diagnostic passa a incluir APK, checksum, metadados do build e certificado; relatórios Android são separados.
- A causa do bloqueio Play Protect no Galaxy A06, assinatura de release e instalação física continuam **não validadas**.

## Não lançado — matriz GT3 MAX e alinhamento de módulos

- Catálogo de 33 funções fornecidas do aplicativo companheiro externo organizado em matriz de compatibilidade, sem presumir suporte do GT3 MAX.
- Fila GATT e parser BLE candidato movidos para pacotes próprios, com testes correspondentes e sem alteração de comportamento/protocolo.
- Arquitetura, mapa de módulos, índice documental e caminho do artefato diagnostic no README alinhados.
- CI deste conjunto e teste físico: **aguardando validação**. A versão `0.4.0-alpha.1` não foi promovida por esta alteração.

## 0.4.0-alpha.1 — 04/11/2026

### Direção Saúde & Fitness
- Produto reposicionado como companheiro de Saúde e Fitness, com o relógio como fonte principal das medições e Conexão Saúde como camada Android de compartilhamento.
- Interface principal orientada a atividade diária, smartwatch e Conexão Saúde.
- Categoria da ficha Play Store preparada como Saúde e fitness; o manifesto não define `appCategory=fitness`, pois esse valor não existe entre as categorias Android válidas.
- Mantida a ponte de notificações como função de companion: leitura consentida de notificações selecionadas, sem responder ou alterar o app de origem.
- O Google lista a retransmissão de notificações para dispositivos vestíveis próprios como caso permitido para apps de Saúde e Fitness; a retransmissão física ao GT3 Maxx segue pendente de protocolo validado.
- Não foi adicionada permissão WRITE_STEPS: o driver atual ainda não fornece amostras de passos do GT3 Maxx comprovadas. Não publicar dados simulados nem dados agregados do telefone como se viessem do relógio.
- Health Connect permanece limitado à leitura agregada de passos; escrita e métricas adicionais dependem de amostras reais, consentimento por tipo e validação.
- Teste físico no Galaxy A06/GT3 Maxx e aceitação pelo Play Protect: **não validados**.


As alterações relevantes do Relógio 2.0 são registradas aqui. Segue SemVer: MAJOR.MINOR.PATCH; versões alpha/beta indicam desenvolvimento e não equivalem a release estável.

## 0.3.0-alpha.4 — 03/11/2026

### Segurança e privacidade de notificações
- Pré-visualizações permanecem somente em memória, com limite de 30 itens e expiração automática após 15 minutos baseada em relógio monotônico.
- Cache é limpo quando o NotificationListenerService desconecta ou é destruído.
- A tela informa, antes de conceder o acesso, quais dados podem ser lidos, como são filtrados e por quanto tempo permanecem na memória.
- A captura exige acesso especial autorizado pelo Android, ativação explícita e ao menos um aplicativo selecionado.
- Nenhuma permissão SMS/acessibilidade, transmissão de conteúdo ou escrita BLE foi adicionada.

### Testes e validação
- Teste unitário cobre expiração das prévias no limite de 15 minutos.
- Lint, testes e APK desta versão: aguardando CI após o commit.
- Play Protect no Galaxy A06: **não validado**; esta alteração reduz retenção e melhora transparência, mas não comprova a causa nem garante remoção do alerta.
- Teste físico com GT3 Maxx: **não validado**.

## 0.3.0-alpha.3 — 03/11/2026

### Corrigido e aprimorado
- Verificação explícita da permissão BLUETOOTH_CONNECT antes de consultar nome/endereço BLE; callbacks de scan tratam revogação de permissão e falha de estado do scanner.
- Callbacks GATT são entregues pelo Handler principal, evitando concorrência com a fila de operações.
- Novas operações BLE são bloqueadas durante a desconexão; falhas de conexão limpam estado e fila.
- Lista de aplicativos de notificações ganhou área rolável própria.
- CI separado em Lint e testes/build; relatório Lint é publicado e o APK debug pode ser preservado quando build/testes passam, mesmo se Lint falhar.

### Protocolo BLE — análise de evidência
- UUIDs NUS dcca9e e quadro recebido AB 00 05 FF 91 80 00 0C comparados com fixture pública do projeto fbiego/dt78.
- Perfil NUS_AB_FRAME_COMPATIBLE_CANDIDATE e parser offline implementados.
- Quadro 0x91/0x80 pode indicar bateria; 0x0C sugere 12%, ainda **não validado** por comparação simultânea com aplicativo companheiro externo.
- Nenhum payload foi escrito no relógio; RX, Secure DFU/OTA, reset e comandos de configuração continuam bloqueados.

### Testes adicionados
- Parser AB: captura local, fixture pública 80%, cabeçalho/tamanho inválidos, comando desconhecido e quadro incompleto.

### Validação
- Lint, testes e APK desta versão: aguardando CI.
- Teste físico Galaxy A06/GT3 Maxx: **não validado**.
## 0.3.0-alpha.2 — 03/11/2026

### Corrigido
- Cancelamento do timeout do scan BLE ao parar a busca, conectar ou receber falha; callbacks antigos são ignorados.
- Fila serial para leituras GATT e escritas do descritor CCCD; inscrições só entram em estado ativo após confirmação do callback.
- Callbacks GATT de conexões antigas não alteram o estado da conexão atual.
- Health Connect distingue ausência de registros de um total válido igual a zero e não reabre o pedido de permissão a cada atualização periódica.
- Cache de notificações é atualizado quando o Android remove uma notificação e quando o usuário desmarca um aplicativo.

### Testes adicionados
- Ordenação, recusa imediata e reset da fila GATT.
- Remoção por chave/pacote e limite de 30 notificações.
- Diferença entre passos sem registros, zero e valores positivos.

### Validação
- CI, lint e testes desta versão: **não validados** até o workflow concluir.
- Teste físico no Galaxy A06 e no GT3 MAX: **não validado**.
- Nenhuma escrita proprietária foi enviada ao relógio; o protocolo BLE permanece somente leitura.

## 0.3.0-alpha.1 — 03/11/2026

### Incluído
- Serviço Android NotificationListenerService.
- Tela de acesso a notificações com autorização especial, controle de pausa e seleção por aplicativo.
- Suporte a prévias publicadas pelo Android, incluindo WhatsApp/WhatsApp Business quando instalados, autorizados e selecionados.
- Cache temporário limitado às 30 notificações mais recentes em memória; sem persistência, logs ou tráfego de rede.
- Testes unitários da normalização de texto.

### Limitações
- Recepção de notificações pelo app ainda aguarda CI e teste físico.
- Não há encaminhamento de notificações ao GT3 MAX: protocolo proprietário não validado.
- Conteúdo só é capturado se o Android o expuser ao listener; textos ocultos/redigidos não são contornados.
- Build debug alpha; não é release assinada de produção.

## 0.2.0-alpha.1 — 03/11/2026

### Incluído
- Documentação centralizada e mapa de módulos.
- Inventário de funções do aplicativo companheiro externo, com prioridade, origem e estado de validação.
- Especificação da integração Android NotificationListenerService para WhatsApp e notificações de outros apps.
- Diário de bordo e política de versionamento.

### Corrigido nesta linha de desenvolvimento
- Comparação do retorno booleano da leitura de característica BLE.
- Assinatura do callback de permissões Android na implementação Health Connect.

### Ainda não validado
- CI e geração do APK após as correções.
- Teste físico das permissões Health Connect no Galaxy A06.
- Captura de notificações WhatsApp pelo serviço próprio.
- Entrega de notificações ao GT3 MAX via BLE.
- Leitura de passos diretamente do GT3 MAX e ajuste do horário do relógio.

## 0.1.0 — protótipo inicial

- Prova de conceito Android/Kotlin para descoberta BLE/GATT em modo de diagnóstico somente leitura.
- Inventário de serviços, características e notificações GATT.
- Integração inicial de leitura agregada de passos por Health Connect na PR #2.
- Exibição da hora local do telefone; sem ajuste do relógio físico.

- A atualização periódica do Health Connect é suspensa quando READ_STEPS não foi concedida ou foi revogada.

- Janela diária calculada a partir do instante da consulta; cancelamento é propagado e polling é suspenso ao perder o provider.


## Validação — segunda auditoria (run #213)

- CI verde no SHA `246bc75fb3a23d6327a2c74acd69915263bdff16` com Gradle 9.5.0, KGP 2.4.21 e AGP 8.13.2.
- Lint, testes Kotlin, builds de ambas as variantes, guardrails, auditoria de manifestos, assinatura APK v2 e publicação concluíram com sucesso.
- Artefato diagnostic `0.4.0-alpha.3-diagnostic` publicado; SHA-256 `27e4bcc623be0e76c29ccf7f16dbf33ff7be0e5ade22e2fdc200adee9813f24b`.
- Permanecem 31/38 avisos Lint nas variantes diagnostic/companion (principalmente SetTextI18n e OldTargetApi). Testes físicos não realizados.
