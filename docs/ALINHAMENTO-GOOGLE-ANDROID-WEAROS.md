# Alinhamento técnico com a plataforma Google — Android, Wear OS e smartwatch

**Data da revisão:** 03/11/2026  
**Aplicação:** Relógio 2.0 (Android/Kotlin)  
**Dispositivo de referência:** GT3 Maxx, conectado diretamente pelo Relógio 2.0  
**Estado:** diretrizes analisadas; validação física ainda pendente.

## 1. Decisão de plataforma

O app Google Pixel Watch é documentado pelo Google como aplicativo complementar para configurar e parear relógios Wear OS. Ele não é um SDK universal para smartwatches Bluetooth.

O GT3 Maxx deste projeto não foi identificado como dispositivo Wear OS. A presença de BLE/GATT não demonstra que execute Wear OS nem que tenha Google Play Services.

**Decisão:** manter o Relógio 2.0 como aplicativo Android nativo que conversa com o GT3 Maxx por BLE/GATT, usando APIs Android públicas. Não declarar compatibilidade com Google Pixel Watch/Wear OS e não adicionar dependências Wear OS ao build do GT3 Maxx sem evidência de compatibilidade.

Referência:
- https://developer.android.com/training/wearables/get-started/connect-phone

## 2. Comunicação com o relógio

### Wearable Data Layer

A Wearable Data Layer é uma API Google Play Services para comunicação entre um app instalado em um telefone Android e o mesmo app instalado em um relógio Wear OS. A documentação exige dispositivo/emulador Wear OS e Google Play Services; a comunicação protegida exige o mesmo nome de pacote e a mesma assinatura nos dois lados.

**Não aplicável ao GT3 Maxx enquanto não houver confirmação de Wear OS.** Não adicionar `play-services-wearable` nem substituir GATT por Data Layer no produto atual.

Para o dispositivo atual:
- BLE/GATT permanece o transporte.
- Descoberta, conexão, leitura e inscrição em notificações seguem permissões Android apropriadas.
- Nenhum comando proprietário ou payload de configuração é permitido sem especificação e validação física.
- O parser NUS/AB permanece candidato; não identifica definitivamente chipset/modelo.

Referências:
- https://developer.android.com/training/wearables/data/overview
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions

## 3. Permissões e pareamento Bluetooth

Para apps direcionados ao Android 12 ou superior:
- `BLUETOOTH_SCAN` é necessário para procurar dispositivos.
- `BLUETOOTH_CONNECT` é necessário para conversar com dispositivos já pareados.
- `BLUETOOTH_ADVERTISE` não deve ser solicitado se o telefone não anuncia como periférico.
- Permissões Bluetooth legadas devem ficar limitadas a API 30.
- `neverForLocation` só pode ser declarado quando os resultados BLE realmente não forem usados para derivar localização física.

O manifesto revisado desta branch usa `BLUETOOTH_SCAN` com `neverForLocation`, `BLUETOOTH_CONNECT` e permissões legadas limitadas a API 30. Essa configuração é coerente com o fluxo descrito, desde que a implementação nunca derive localização a partir do scan.

O Companion Device Manager (CDM) oferece uma alternativa Android para associação/seleção de dispositivos próximos e pode evitar a necessidade de permissão de localização. Deve ser avaliado como experimento isolado, não introduzido como troca automática: é necessário confirmar que o filtro de associação funciona com o anúncio BLE real do GT3 Maxx e que não quebra a conexão GATT e a seleção atual.

Referências:
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- https://developer.android.com/guide/topics/connectivity/companion-device-pairing
- https://developer.android.com/reference/android/companion/CompanionDeviceManager

## 4. Notificações

Em Wear OS, notificações geradas pelo app no telefone podem ser encaminhadas automaticamente pelo próprio sistema ao relógio pareado. O modelo oficial não exige que o app complementar leia todas as notificações do telefone para reenviá-las ao relógio.

O GT3 Maxx não é um dispositivo Wear OS confirmado. A recepção de notificações Android e o protocolo proprietário necessário para enviar mensagens por GATT são questões distintas. A recepção Android não comprova que o firmware do GT3 aceite os mesmos dados.

Diretrizes para o Relógio 2.0:
- Não adicionar envio BLE de mensagens até validar formato, limites, codificação e comportamento do relógio por capturas controladas.
- O `NotificationListenerService` permanece uma função opcional e sensível, desativada por padrão, com autorização especial explícita e seleção de pacotes.
- Não solicitar `READ_SMS`, permissões de acessibilidade ou outras permissões não necessárias para tentar contornar limitações de notificações.
- Manter o cache volátil limitado a 30 itens e 15 minutos, com limpeza na remoção, revogação, desconexão e desativação.
- Validar no Galaxy A06 se o serviço é vinculado pelo Android após a autorização especial. CI de compilação não comprova a vinculação no sistema.

Referências:
- https://developer.android.com/training/wearables/notifications
- https://developer.android.com/reference/android/service/notification/NotificationListenerService
- https://developer.android.com/reference/android/Manifest.permission#BIND_NOTIFICATION_LISTENER_SERVICE

## 5. Saúde e atividade

No telefone Android, Health Connect é a integração pública apropriada para ler os registros de saúde que as fontes autorizadas compartilham. O Relógio 2.0 declara apenas `READ_STEPS`; não deve gravar passos, frequência cardíaca ou outras métricas sem uma necessidade e autorização explícitas.

Regras:
- Verificar disponibilidade do Health Connect e permissões antes de cada leitura.
- Preservar origem e intervalo temporal dos registros; um agregado não deve ser rotulado como dado exclusivo do GT3 Maxx.
- Diferenciar ausência de registros de valor zero.
- Não inventar frequência cardíaca, sono, SpO2, pressão ou passos quando a fonte não os disponibilizar.
- No Android 14 ou superior, Health Connect integra a plataforma; em versões anteriores, depende do app Health Connect instalado/atualizado.

Health Services (Wear OS) é uma API para sensores e experiências de saúde executadas em relógios Wear OS. Não é uma API para ler sensores internos do GT3 Maxx genérico.

Referências:
- https://developer.android.com/health-and-fitness/health-connect
- https://developer.android.com/health-and-fitness/health-connect/features/steps
- https://developer.android.com/health-and-fitness/health-services
- https://developer.android.com/health-and-fitness/health-services/compatibility

## 6. Arquitetura e qualidade a seguir

A arquitetura deve continuar separando:
1. **UI Android:** apresenta estado, origem, idade e indisponibilidade sem inventar valores.
2. **Domínio:** regras de interpretação, unidades, validação e confiança dos dados.
3. **Transporte BLE:** scan, GATT, fila, permissões e ciclo de vida, sem regras de apresentação.
4. **Perfil de protocolo:** UUIDs e parsers isolados por evidência/modelo, com estado candidato/não validado.
5. **Health Connect:** adaptador separado; dados Android não devem ser confundidos com telemetria proprietária do relógio.
6. **Notificações:** módulo isolado, opt-in, sem persistência ou rede.

A implementação atual ainda concentra mais de uma responsabilidade em `MainActivity.kt`; isso permanece dívida arquitetural e não deve ser considerado resolvido só por documentação.

As listas de qualidade Wear OS são requisitos para apps de relógio publicados como apps Wear OS. Elas não transformam o app Android de telefone em app Wear OS. Para o Relógio 2.0 atual, priorizar qualidade Android móvel: instalação e inicialização estáveis, permissões mínimas, tratamento de revogação, acessibilidade, consumo de bateria, testes offline e validação física.

Referências:
- https://developer.android.com/docs/quality-guidelines/wear-app-quality
- https://developer.android.com/guide/topics/architecture

## 7. Checklist de alinhamento da branch

| Área | Decisão | Estado |
|---|---|---|
| Wear OS / Pixel Watch | Não declarar compatibilidade sem comprovação de sistema | Aplicado à documentação |
| Wearable Data Layer | Não usar com GT3 Maxx não Wear OS | Não aplicável ao hardware atual |
| BLE Android | SCAN/CONNECT e legado limitado a API 30 | Manifesto alinhado; teste físico pendente |
| Companion Device Manager | Avaliar em protótipo separado | Pendente |
| Health Connect | Leitura de passos com permissão explícita e ausência ≠ zero | Código presente; teste físico pendente |
| Health Services | Não usar em relógio genérico sem Wear OS | Não aplicável ao hardware atual |
| Notificações | Opt-in, pacote permitido, cache volátil com expiração | Implementado nesta branch; teste físico pendente |
| Escritas BLE proprietárias | Bloqueadas até protocolo validado | Regra mantida |
| Galaxy A06 / Android 16 | Validar permissões, BLE, Health Connect e listener | **Não validado** |
| Play Protect | Não desativar nem contornar; investigar APK/certificado/alerta | **Não validado** |

## 8. Referências oficiais do Google/Android

- Pixel Watch como app complementar Wear OS: https://developer.android.com/training/wearables/get-started/connect-phone
- Wearable Data Layer: https://developer.android.com/training/wearables/data/overview
- Permissões Bluetooth: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- Companion Device Manager: https://developer.android.com/guide/topics/connectivity/companion-device-pairing
- Notificações Wear OS: https://developer.android.com/training/wearables/notifications
- NotificationListenerService: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Health Connect: https://developer.android.com/health-and-fitness/health-connect
- Health Services: https://developer.android.com/health-and-fitness/health-services
- Qualidade Wear OS: https://developer.android.com/docs/quality-guidelines/wear-app-quality
