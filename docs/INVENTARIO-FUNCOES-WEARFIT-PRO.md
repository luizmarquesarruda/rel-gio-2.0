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
| Saúde | Respiração, stress/fadiga e temperatura | Wearfit Pro/BLE | P2 | Não implementado; estimativas exigem validação |
| Saúde | Glicose, MET e ciclo menstrual | Wearfit Pro/app | P3 | Não priorizar; disponibilidade/dispositivo e significado precisam ser comprovados |
| Atividade | Reconhecimento automático e metas | Wearfit Pro/driver | P1 | Planejado |
| Bem-estar | Lembrete de saúde e treino respiratório | Wearfit Pro/driver | P2 | Planejado |
| Controles | Música, câmera remota, cronômetro e timer | Android/BLE | P3 | Não implementado |
| Dispositivo | Encontrar telefone, alarme e configurações de tela | Android/BLE | P2 | Não implementado |
| Ambiente | Clima e localização | Serviço online e localização Android | P3 | Fora do escopo atual; dados sensíveis e dependência de rede |
| Configuração crítica | OTA, reset, restauração de fábrica e ajustes de firmware | BLE proprietário | Fora de escopo | Bloqueado por risco; não implementar |
| Pagamento/identidade | Alipay, cartão de acesso e dados de contato | Firmware/serviço externo | Fora de escopo | Não implementar sem exigência explícita e avaliação de segurança |

## Priorização de engenharia

**P0 — base funcional:** conexão/estado/bateria quando comprovados, passos, referência de horário, notificações Android (WhatsApp e apps escolhidos pelo usuário).

**P1 — saúde e atividade com boa proveniência:** frequência cardíaca, sono, sessões de exercício, histórico e metas. Só exibir quando a origem e o dispositivo produtor estiverem identificados.

**P2 — métricas estimadas e conforto:** SpO2, pressão, temperatura, respiração, stress/fadiga e lembretes. Tratar como estimativas do wearable; nunca diagnóstico ou orientação clínica.

**P3 — conveniência:** música, câmera remota, alarmes, cronômetro, timer, encontrar telefone, mostradores e configurações de tela. Implementar somente se o benefício superar o custo de manter um protocolo proprietário.

**Bloqueado:** OTA/DFU, reset/restauração, configuração de firmware, pagamento/cartões de acesso e qualquer escrita desconhecida. Estes itens não fazem parte do escopo seguro atual.

As funções anunciadas no site são um catálogo de recursos do ecossistema, não uma garantia de que o GT3 MAX tenha todos os sensores ou recursos.

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

- Wearfit Pro — App Function: https://wearfit.app/wearfit-pro-app-function/
- Wearfit Pro — FAQ (ferramentas e configurações): https://wearfit.app/frequently-asked-questions-about-wearfit-pro-app/
- Wearfit Pro — Sleep monitoring: https://wearfit.app/sleep-monitoring/
- Wearfit Pro — Heart rate: https://wearfit.app/measure-heart-rate/
- Wearfit Pro — Message notification: https://wearfit.app/message-notification/
- Wearfit Pro — Privacy statement: https://wearfit.app/statement-on-wearfit-pro-and-privacy/
- Android — Health Connect / Fit migration: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Android — Steps with Health Connect: https://developer.android.com/health-and-fitness/health-connect/features/steps
- Android — NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
