# Changelog

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
- A interpretação AB 0x91/0x80 permanece como candidata de bateria até correlação com Wearfit Pro; quadros desconhecidos não recebem semântica inventada.
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

## Não lançado — investigação do canal de parceiros Wearfit Pro

- Analisada a declaração Weike atualizada em 04/08/2023: ela prevê acesso por aplicativos terceiros autorizados a identificador aleatório do dispositivo, estado, notificações e, mediante autorização expressa, dados de sensores, sono, exercício, uso, logs e localização.
- Documentada a distinção entre a declaração de privacidade e uma API/SDK: o texto não fornece artefato Android, contratos, callbacks, exemplos ou esquema de dados.
- Registrada a dúvida de elegibilidade: o canal descrito aplica-se a vestíveis Weike pareados; ainda não há confirmação de que o GT3 MAX Call esteja habilitado nesse programa.
- Prioridade dada à obtenção do SDK/API oficial e confirmação do modelo; sem leitura de armazenamento privado, credenciais, tráfego autenticado ou comandos BLE especulativos.
- Integração de parceiro ainda não implementada; CI e teste físico não validados por esta documentação.

## Não lançado — perfil de acesso GT3 MAX Call

- Adicionado perfil de dispositivo com metadados informados pelo proprietário: GT3 MAX Call, firmware V1.07 e build SH0050-1_1.1.3_1.23_1.18_1.1. Esses campos não são apresentados como leitura autenticada do GATT.
- Compara o nome anunciado e o fingerprint completo dos serviços/características já capturados para identificar compatibilidade com a unidade.
- Após fingerprint completo, inscreve automaticamente somente a característica NUS TX em notificações, usando CCCD padrão; não envia payload à característica NUS RX.
- Exibe a interpretação AB 0x91/0x80 como bateria candidata, sem apresentar o valor como validado até correlação com Wearfit Pro em diferentes níveis de carga.
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

- Catálogo de 33 funções fornecidas do Wearfit Pro organizado em matriz de compatibilidade, sem presumir suporte do GT3 MAX.
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
- Quadro 0x91/0x80 pode indicar bateria; 0x0C sugere 12%, ainda **não validado** por comparação simultânea com Wearfit Pro.
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
- Inventário de funções do Wearfit Pro, com prioridade, origem e estado de validação.
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
