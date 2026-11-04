# Diário de bordo — Relógio 2.0

Este arquivo é o registro cronológico principal. Atualizar em toda PR que altere arquitetura, funções, dependências, versão, dados ou protocolo.

## 03/11/2026 — Alinhamento Play Protect e estratégia de distribuição

- Consultada a orientação oficial Google para avisos do Play Protect. O Google documenta que, em mercados selecionados, instalações por sideload da internet com capacidades sensíveis como NOTIFICATION_LISTENER podem ser bloqueadas.
- O manifesto desta branch declara NotificationCaptureService protegido por BIND_NOTIFICATION_LISTENER_SERVICE e exported=false, sem declarações READ_SMS, RECEIVE_SMS ou Accessibility Service.
- O serviço exige acesso especial, ativação explícita e seleção de aplicativos; as prévias ficam em memória, limitadas a 30 itens/15 minutos, sem gravação ou envio à rede.
- A implementação atual ainda não encaminha notificações ao GT3 Maxx. Não declarar que já cumpre o caso de uso de retransmissão para wearable reconhecido na orientação do Google.
- APK atual é debug e não release assinada. CI aprovado confirma build/testes, não validação Play Protect.
- Decisão proposta: separar variante diagnostic sem Notification Listener e variante companion com o recurso isolado. A distribuição da companion deve usar canal Play de teste/revisão depois de implementar e validar o encaminhamento e a transparência ao usuário.
- Proibido orientar desativação do Play Protect, ocultar capacidades do manifesto ou contornar a análise.
- CI da PR #5: run 41750419237 concluído com sucesso no SHA 9b7c563b206e63f6ba961a2f90ef2d5c1e2cbab2. Artefato debug e relatório Lint publicados; validade informada até 18/11/2026. Isso não significa que o Play Protect aprovou o APK.
- Documento criado: docs/ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md. Bloqueio exato e teste físico no Galaxy A06: **não validados**, pois a mensagem exibida pelo Play Protect não foi fornecida.

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
