# Integração Android — WhatsApp e notificações

## Estado da implementação

A captura local foi implementada na branch `feat/android-notification-listener` / PR #4 e recebeu endurecimento de privacidade na PR #5. O código contém `NotificationCaptureService`, `NotificationCenterActivity`, preferências de consentimento/filtro e cache em memória. A compilação e o teste físico desta fase permanecem **não validados** até o CI terminar e a instalação ser testada.

## Objetivo

Receber no Relógio 2.0 eventos de notificações que o Android efetivamente disponibiliza, incluindo WhatsApp, WhatsApp Business, SMS, chamadas e outros aplicativos selecionados pelo usuário.

## API recomendada

Usar `android.service.notification.NotificationListenerService`.

O serviço recebe callbacks quando uma notificação é publicada, removida ou reclassificada pelo sistema. O app deve:
1. Declarar o serviço no AndroidManifest com `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` e a ação `android.service.notification.NotificationListenerService`.
2. Orientar o usuário a conceder acesso na tela de Acesso a notificações do Android (`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS`).
3. Aguardar `onListenerConnected()` antes de consultar notificações ativas ou processar callbacks.
4. Receber `onNotificationPosted(StatusBarNotification)` e `onNotificationRemoved(...)`.
5. Extrair apenas campos realmente disponíveis em `Notification.extras`, como título e texto; não assumir que todo aplicativo publica o corpo completo.
6. Aplicar filtro por pacote/categoria e uma política explícita de retenção.
7. Entregar eventos normalizados ao centro de notificações local.

O serviço não deve declarar `BIND_NOTIFICATION_LISTENER_SERVICE` como permissão de uso normal. Essa permissão protege o serviço e o Android controla quais listeners podem ser vinculados.

## WhatsApp

Pacotes Android normalmente usados:
- WhatsApp Messenger: `com.whatsapp`
- WhatsApp Business: `com.whatsapp.w4b`

O listener pode receber notificações desses pacotes quando elas forem publicadas pelo Android e o usuário autorizar o acesso. A disponibilidade de título, remetente, prévia e mensagens agrupadas depende das configurações de privacidade do WhatsApp, do canal de notificação, da tela bloqueada e da versão do Android.

Não acessar banco privado do WhatsApp, arquivos internos, chaves, sessão, conteúdo criptografado ou APIs não documentadas. Não contornar prévias desativadas. O app só pode apresentar o conteúdo que o Android entregar ao listener.

## Fluxo de dados

```text
WhatsApp / outros apps
        ↓ notificação publicada
Android NotificationManager
        ↓ acesso autorizado pelo usuário
NotificationListenerService
        ↓ filtro por pacote e política de privacidade
NotificationNormalizer
        ↓ evento em memória / UI local
NotificationTransport (futuro)
        ↓ somente após validar protocolo
GT3 MAX
```

A última etapa está bloqueada: o protocolo proprietário de notificação do GT3 MAX não foi identificado. A recepção Android não prova que o relógio aceite os mesmos bytes, formato, codificação ou comandos de qualquer aplicativo externo.

## Configuração implementada nesta fase

- Tela própria que explica o acesso e abre as configurações especiais do Android; o usuário concede o acesso manualmente.
- Lista os aplicativos de usuário com atividade lançadora instalada; o usuário seleciona cada pacote a ser capturado. WhatsApp e WhatsApp Business aparecem se estiverem instalados e visíveis ao PackageManager.
- Opção de prévia do conteúdo separada da opção de exibir somente nome do app e horário.
- Controles para pausar a captura e limpar as prévias temporárias.
- O conteúdo fica apenas em memória do processo, sem envio à nuvem, persistência em banco/arquivo ou inclusão nos logs de diagnóstico.
- Limite implementado: 30 eventos em memória e retenção máxima de 15 minutos, medida por relógio monotônico. A limpeza por tempo acontece ao inserir ou consultar o cache.
- O callback `onNotificationRemoved` remove a prévia pela chave fornecida pelo Android. Desmarcar um aplicativo também apaga as prévias já armazenadas daquele pacote. A desconexão ou destruição do listener limpa todo o cache.
- A expiração temporal de 15 minutos já está implementada; entradas expiradas são removidas na próxima inserção ou leitura do cache.
- Nunca registrar corpo de mensagens em logs do CI, crash reports ou relatórios públicos.

## Limitações e segurança

- Acesso é concedido pelo usuário nas configurações especiais do Android; não existe uma permissão runtime comum que substitua essa ação.
- O usuário pode revogar o acesso a qualquer momento.
- Android 15 aplica proteção adicional a conteúdo sensível, incluindo ocultação/redação de OTPs para listeners não confiáveis. O app deve aceitar o conteúdo redigido, sem procurar contornar a proteção.
- Notificações silenciosas, agrupadas, ocultas na tela bloqueada, suprimidas por canais ou não publicadas não podem ser recuperadas por este mecanismo.
- O listener não equivale a permissão para ler SMS diretamente. Não solicitar `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS`, Accessibility Service ou root para esta funcionalidade.
- Ações de resposta devem ser uma fase separada, usando somente ações públicas `RemoteInput` quando presentes e após desenho de consentimento. Não enviar mensagens automaticamente.

## Independência do aplicativo

O Relógio 2.0 não se integra, não pareia, não compartilha conexão e não sincroniza com aplicativos companheiros externos. A captura de notificações Android é local e independente; o encaminhamento ao GT3 MAX permanece bloqueado até que exista protocolo BLE documentado e validado.

## Não usar APIs de Wear OS como substituto

O GT3 MAX foi identificado como relógio BLE/GATT genérico, não como dispositivo Wear OS. As APIs oficiais de Notification Bridging e Data Layer do Wear OS não são automaticamente compatíveis com ele. A ponte BLE depende do firmware/protocolo real do fabricante.

## Estado e critérios de aceite

Código adicionado não significa integração validada. A próxima validação física deverá confirmar:


1. Acesso não é ativado sem ação explícita do usuário.
2. App consegue detectar se o listener está conectado e orientar a reativação se revogado.
3. Eventos de WhatsApp, WhatsApp Business e apps selecionados aparecem com origem e horário.
4. Notificação sem prévia mostra apenas os campos que o Android disponibilizar.
5. Conteúdo não aparece em logs persistentes nem é enviado à rede.
6. Pausar/desativar a captura interrompe a coleta.
7. O callback de remoção, a revogação de um pacote e a desconexão/destruição do listener limpam as prévias correspondentes; o restante expira em até 15 minutos.
8. Nenhum payload é enviado ao relógio antes de validar o protocolo.

## Fontes oficiais

- Android API — NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android 15 — proteções de conteúdo sensível e notification listeners: https://developer.android.com/about/versions/15/behavior-changes-all
- Wear OS notifications (referência apenas para dispositivos Wear OS): https://developer.android.com/training/wearables/notifications
