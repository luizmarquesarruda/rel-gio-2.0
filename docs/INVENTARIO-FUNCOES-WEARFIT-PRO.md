# Inventário de funções de referência — Wearfit Pro

## Objetivo

Usar o Wearfit Pro como referência funcional para o Relógio 2.0, sem copiar pressupostos de implementação nem assumir que existe SDK público. A documentação de privacidade do fornecedor descreve categorias de dados e permissões; não é uma especificação de integração.

## Catálogo funcional

| Área | Função de referência | Fonte/canal possível | Prioridade | Situação no Relógio 2.0 |
|---|---|---|---|---|
| Dispositivo | Pareamento, estado conectado e bateria/carga | Wearfit Pro ou BLE validado | P0 | BLE GATT provado; bateria ainda não identificada |
| Atividade | Passos diários | Health Connect ou driver BLE | P0 | Health Connect em PR #2; origem GT3 não confirmada |
| Atividade | Histórico de passos e tendências | Health Connect/local | P1 | Planejado |
| Saúde | Frequência cardíaca | Wearfit Pro/Health Connect/BLE | P1 | Sem parser GT3 validado |
| Saúde | Sono e fases | Wearfit Pro/Health Connect/BLE | P1 | Não implementado |
| Saúde | Exercícios e duração | Wearfit Pro/Health Connect/BLE | P1 | Não implementado |
| Saúde | SpO2 e pressão estimada | Wearfit Pro/BLE | P2 | Não implementado; não apresentar como diagnóstico |
| Relógio | Hora/data e sincronização | Android + comando proprietário | P0 | Hora do telefone em PR #2; ajuste do relógio pendente |
| Alertas | Chamadas e chamadas perdidas | Notificação Android / integração do fabricante | P0 | Pesquisa; encaminhamento ao relógio não validado |
| Alertas | SMS | Notificação Android / integração do fabricante | P1 | Pesquisa; não pedir READ_SMS nesta abordagem |
| Alertas | WhatsApp e WhatsApp Business | NotificationListenerService | P0 | Planejado nesta documentação; não implementado |
| Alertas | Outros apps | NotificationListenerService + filtros | P1 | Planejado |
| Recursos | Lembrete sedentário, alarme, encontrar telefone/relógio | BLE/firmware específico | P2 | Sem protocolo comprovado |
| Ambiente | Clima e localização | Serviço online e localização Android | P3 | Fora do escopo atual; dados sensíveis e dependência de rede |

## Evidência pública do Wearfit Pro

A página de ajuda “Message notification”, datada de 21/01/2025, ensina a ativar notificações em Dispositivo → Message notification e lista telefone, SMS, WeChat, QQ e Weibo. A página não lista WhatsApp nessa relação. Isso não prova que nenhuma versão/modelo suporte WhatsApp; significa apenas que a documentação pública consultada não confirma esse suporte.

A declaração de privacidade do Wearfit Pro menciona que aplicações de terceiros, com autorização expressa, podem interagir com o dispositivo pareado e acessar determinadas categorias de dados. Ela não fornece nome de SDK, coordenadas Maven, métodos, contratos, UUIDs ou protocolo de mensagens.

## Decisão de produto

- Primeiro entregar as funções Android que têm API documentada e consentimento explícito.
- Usar Health Connect para dados de passos agregados; não atribuir o total ao GT3 MAX sem identificar a fonte.
- Usar NotificationListenerService para ler somente o conteúdo que o Android expõe nas notificações e para os apps autorizados pelo usuário.
- Manter Wearfit Pro como referência e aplicativo companheiro até haver SDK/API oficial verificável ou protocolo BLE reproduzível.
- Não implementar ações como responder, marcar como lida, rejeitar chamada, enviar SMS, ajustar relógio ou atualizar firmware sem fluxo e protocolo documentados.

## Fontes

- Wearfit Pro — Message notification: https://wearfit.app/message-notification/
- Wearfit Pro — Privacy statement: https://wearfit.app/statement-on-wearfit-pro-and-privacy/
- Android — Health Connect / Fit migration: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Android — Steps with Health Connect: https://developer.android.com/health-and-fitness/health-connect/features/steps
- Android — NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
