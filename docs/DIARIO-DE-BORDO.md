# Diário de bordo — Relógio 2.0

Este arquivo é o registro cronológico principal. Atualizar em toda PR que altere arquitetura, funções, dependências, versão, dados ou protocolo.

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
