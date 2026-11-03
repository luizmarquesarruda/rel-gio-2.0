# Changelog

As alterações relevantes do Relógio 2.0 são registradas aqui. Segue SemVer: MAJOR.MINOR.PATCH; versões alpha/beta indicam desenvolvimento e não equivalem a release estável.

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
