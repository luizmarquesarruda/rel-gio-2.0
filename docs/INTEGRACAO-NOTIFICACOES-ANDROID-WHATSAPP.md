# Integração Android — WhatsApp e notificações

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

A última etapa está bloqueada: o protocolo proprietário de notificação do GT3 MAX não foi identificado. A recepção Android não prova que o relógio aceite os mesmos bytes, formato, codificação ou comandos usados pelo Wearfit Pro.

## Configuração proposta

- Tela de consentimento explicando que o acesso permite ver notificações de vários aplicativos.
- Lista de aplicativos permitidos; WhatsApp e WhatsApp Business podem ser sugeridos, mas a seleção deve ser do usuário.
- Opção de prévia do conteúdo separada da opção de exibir somente nome do app e horário.
- Botão para pausar e botão para apagar o histórico temporário.
- Por padrão, conteúdo de notificação apenas em memória; sem envio à nuvem e sem inclusão em logs de diagnóstico.
- Limite inicial sugerido: até 30 eventos em memória, substituindo os mais antigos. Ajustar após teste de consumo de memória.
- Remover conteúdo da memória quando a notificação é removida pelo Android, quando possível; expiração adicional definida no produto.
- Nunca registrar corpo de mensagens em logs do CI, crash reports ou relatórios públicos.

## Limitações e segurança

- Acesso é concedido pelo usuário nas configurações especiais do Android; não existe uma permissão runtime comum que substitua essa ação.
- O usuário pode revogar o acesso a qualquer momento.
- Android 15 aplica proteção adicional a conteúdo sensível, incluindo ocultação/redação de OTPs para listeners não confiáveis. O app deve aceitar o conteúdo redigido, sem procurar contornar a proteção.
- Notificações silenciosas, agrupadas, ocultas na tela bloqueada, suprimidas por canais ou não publicadas não podem ser recuperadas por este mecanismo.
- O listener não equivale a permissão para ler SMS diretamente. Não solicitar `READ_SMS`, `READ_CALL_LOG`, `READ_CONTACTS`, Accessibility Service ou root para esta funcionalidade.
- Ações de resposta devem ser uma fase separada, usando somente ações públicas `RemoteInput` quando presentes e após desenho de consentimento. Não enviar mensagens automaticamente.

## Wearfit Pro versus app próprio

A ajuda pública do Wearfit Pro orienta ativar Device → Message notification e lista telefone, SMS, WeChat, QQ e Weibo. WhatsApp não aparece nessa lista pública. No GT3 MAX, testar primeiro o recurso integrado do Wearfit Pro; não é possível garantir compatibilidade do modelo ou da versão instalada.

O app próprio pode observar notificações Android, mas enviá-las ao GT3 MAX requer protocolo BLE de notificação validado. O serviço Android e o driver BLE devem permanecer desacoplados.

## Não usar APIs de Wear OS como substituto

O GT3 MAX foi identificado como relógio genérico associado ao Wearfit Pro, não como dispositivo Wear OS. As APIs oficiais de Notification Bridging e Data Layer do Wear OS não são automaticamente compatíveis com ele. A ponte BLE depende do firmware/protocolo real do fabricante.

## Critérios de aceite

1. Acesso não é ativado sem ação explícita do usuário.
2. App consegue detectar se o listener está conectado e orientar a reativação se revogado.
3. Eventos de WhatsApp, WhatsApp Business e apps selecionados aparecem com origem e horário.
4. Notificação sem prévia mostra apenas os campos que o Android disponibilizar.
5. Conteúdo não aparece em logs persistentes nem é enviado à rede.
6. Pausar/desativar a captura interrompe a coleta.
7. Remoção/expiração limpa o cache temporário.
8. Nenhum payload é enviado ao relógio antes de validar o protocolo.

## Fontes oficiais

- Android API — NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android 15 — proteções de conteúdo sensível e notification listeners: https://developer.android.com/about/versions/15/behavior-changes-all
- Wearfit Pro — Message notification: https://wearfit.app/message-notification/
- Wear OS notifications (referência apenas para dispositivos Wear OS): https://developer.android.com/training/wearables/notifications
