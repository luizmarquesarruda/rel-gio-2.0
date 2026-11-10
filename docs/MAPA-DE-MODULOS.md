# Mapa de módulos

## Estado estrutural observado

A base é um aplicativo Android nativo Kotlin. O adaptador de consulta agregada e a política de estado da permissão Health Connect foram extraídos para data/healthconnect; MainActivity coordena o ciclo de vida da permissão e a UI, enquanto o ciclo de vida BLE ainda está parcialmente concentrado no mesmo ponto. A separação é incremental, não concluída.

### Componentes implementados

| Arquivo | Módulo | Responsabilidade |
|---|---|---|
| domain/health/HealthMetricSample.kt | Domain / Health | Tipo, unidade, origem, timestamps e estado de validação sem dependência Android/BLE. |
| domain/health/StepsSnapshot.kt | Domain / Activity | Snapshot do agregado de passos; diferencia zero de ausência e preserva intervalo, recebimento e origem Health Connect. |
| data/healthconnect/HealthConnectStepsRepository.kt | Data / Health Connect | Disponibilidade do provider, permissão READ_STEPS e consulta agregada diária, sem atribuir a origem ao GT3 MAX. |
| data/healthconnect/HealthConnectPermissionState.kt | Data / Health Connect | Estado local de solicitação, recusa, concessão e revogação; política pura, testável e sem substituir a autorização real do provider. |
| data/healthconnect/HealthConnectDayWindow.kt | Data / Health Connect | Calcula o início do dia local a partir de Instant + ZoneId com compatibilidade com minSdk 26. |
| device/ble/gatt/SerialGattOperationQueue.kt | BLE transport / GATT | Serializar operações GATT assíncronas. Não interpreta payload. |
| device/ble/gatt/BluetoothCurrentTimePayload.kt | BLE / Bluetooth SIG CTS | Codificar os 10 bytes da característica Current Time (0x2A2B); não executa conexão nem escrita. |
| device/ble/core/BleReconnectPolicy.kt | BLE lifecycle | Política pura de backoff para retentativas; não executa scan nem conexão. |
| device/gt3max/protocol/Gt3MaxCallDeviceProfile.kt | Perfil do dispositivo | Identificadores GT3 MAX Call informados e fingerprint GATT da captura; sem autenticação de hardware/firmware. |
| device/gt3max/protocol/WearfitAbFrameParser.kt | Protocolo do dispositivo | Perfil NUS/AB candidato e parser restrito. Não controla conexão. |
| device/discovery/FeatureDiscoveryEngine.kt | Descoberta de funcionalidades | Classifica inventário GATT e metadados de notificações sem inferir semântica de payload desconhecido. |
| data/discovery/FeatureDiscoveryStore.kt | Persistência do inventário GATT | Persiste inventário, descobertas e contagens com schema versionado; não grava payload HEX bruto e preserva conteúdo inválido/futuro. |
| data/discovery/DiscoveryLibraryStore.kt | Biblioteca interna de diagnóstico | Guarda descobertas e leituras capturadas durante teste cronometrado em arquivo privado com escrita atômica, limite de registros e recusa de schema corrompido/futuro. HEX é salvo somente durante sessão explicitamente iniciada. |
| data/discovery/DiscoveryLibraryCodec.kt | Formato da biblioteca | Codec binário versionado e testável sem Android; valida magic, schema, tamanho e contagem de registros. |
| FeatureDiscoveryActivity.kt | UI / Laboratório BLE | Agrupa descobertas GATT, roteiro de testes, contador regressivo de captura e atalho à biblioteca. |
| DiscoveryLibraryActivity.kt | UI / Biblioteca | Pesquisa e filtra descobertas, sessões, notificações e leituras locais. |
| notifications/NotificationCaptureService.kt | Platform / Notifications | Captura consentida na variante companion. |
| notifications/NotificationCaptureStore.kt | Data / Notifications | Cache volátil, filtros e preferências. |
| notifications/NotificationCenterActivity.kt | UI / Notifications | Consentimento, seleção e prévias. |
| HealthPrivacyActivity.kt | UI / Privacy | Transparência e controles de dados de saúde. |
| MainActivity.kt | App / composição provisória | Mantém UI, permissões e ciclo de vida GATT; inventário GATT é passivo e operações são limitadas à inscrição NUS TX após fingerprint completo; delega a consulta de passos ao repositório Health Connect. |

Os testes unitários acompanham fila GATT, parser candidato, motor de autodescoberta, perfil GATT GT3 MAX Call, janela diária Health Connect, metadados do APK, formatadores e contratos Domain. A inscrição automática NUS TX exige nome reconhecido e fingerprint GATT completo da captura. A tela não disponibiliza leitura nem inscrição manual de características arbitrárias; NUS RX permanece sem escrita.

## Estrutura-alvo

```text
com.luizmarquesarruda.relogio20/
├── app/
├── ui/
├── domain/
│   ├── health/                  # modelo inicial implementado
│   ├── activity/
│   ├── goals/
│   └── device/
├── device/
│   ├── ble/{core,gatt}/
│   └── gt3max/{protocol,metrics}/
├── data/{healthconnect,local,notifications}/
├── platform/
└── common/
```

Diretórios ainda não criados são estrutura-alvo, não módulos implementados.

## Contratos

| Fronteira | Responsabilidade | Proibição |
|---|---|---|
| UI ↔ Domain | apresentar estado e acionar casos de uso | UI não conhece UUIDs/payload |
| BLE Core ↔ protocolo | transportar eventos/bytes | transporte não interpreta saúde |
| Protocolo ↔ Domain | converter quadros comprovados em amostras | não adivinhar semântica |
| Domain ↔ Storage | persistir métrica e proveniência | ausência não vira zero |
| Domain ↔ Health Connect | compartilhar tipos compatíveis após autorização | não atribuir agregado ao GT3 |
| Platform ↔ Device | integrar APIs/perfis Android corretos | não confundir BLE e Classic |
| Privacy ↔ todos | consentimento, minimização e retenção | não transmitir conteúdo sem propósito |

## Matriz de recursos

A [matriz de compatibilidade](MATRIZ-COMPATIBILIDADE-GT3-MAXX.md) registra 33 entradas do catálogo, módulo responsável, dependências, risco e estado de validação.

## Integração oficial Wearfit Pro — pendente de fornecedor

A declaração de privacidade prevê acesso por aplicativos de terceiros autorizados a determinados dados e funções do wearable. Ela não documenta o SDK, o contrato Android nem a elegibilidade do GT3 MAX Call. O módulo futuro `data/wearfit` deverá ser implementado apenas após receber SDK/API e amostra oficial; o adapter não será simulado. Ver [INTEGRACAO-WEARFIT-PARTNER-SDK.md](INTEGRACAO-WEARFIT-PARTNER-SDK.md).

## Dívida técnica prioritária

1. Extrair scan/ciclo GATT de MainActivity.kt para device/ble/core, preservando fila e proteção contra callbacks antigos.
2. Criar persistência local versionada para HealthMetricSample e StepsSnapshot, com migração e testes de integridade.
3. Definir contratos de ingestão para rotas de exercício e sinais vitais sem habilitar leitura/escrita sem origem comprovada.
4. Mapear quadros de sensores para Domain somente após capturas BLE repetíveis e validação física.
5. Separar adaptadores Platform de chamadas, mídia, alarmes e câmera somente quando os recursos forem selecionados.
6. Manter OTA, reset/factory restore, pagamentos e cartões de acesso bloqueados.

## Limite da entrega atual

A fila GATT, o parser candidato e o adaptador Health Connect de passos foram isolados com testes. O modelo Domain inicial e o snapshot de passos agregados têm testes unitários. MainActivity.kt ainda coordena o transporte BLE e a política de reconexão; a extração do ciclo de vida BLE permanece pendente. Health Connect segue somente leitura de passos agregados; nenhum sensor do GT3 MAX foi validado fisicamente.

## Biblioteca local e laboratório de captura — 04/11/2026

- O painel inicial separa conexão, laboratório, biblioteca e demais recursos; o registro técnico fica recolhido por padrão.
- O laboratório oferece sessões de 1, 2, 5 ou 10 minutos, com deadline persistido, contador regressivo, barra de progresso e quantidade de leituras/notificações recebidas.
- A biblioteca usa arquivo privado do app (discovery-library-v1.bin) e AtomicFile; o codec tem schema 1, limite de 1.000 registros e proteção contra sobrescrita de conteúdo corrompido ou schema futuro.
- Inventários GATT são salvos como descobertas. Notificações e leituras com HEX são guardadas somente enquanto uma sessão cronometrada, iniciada com confirmação, estiver ativa.
- A biblioteca permite pesquisa e filtros por descobertas, leituras e testes. Nenhum dado é enviado à rede e nenhuma permissão adicional foi criada.
- O arquivo é local ao aplicativo; a implementação não declara criptografia própria. Payloads HEX ainda podem conter dados sensíveis e são mantidos no sandbox privado do Android.
\n| `domain/intelligence/DailyActivityObservation.kt` | Domain / Intelligence | Agregado diário de passos com proveniência e validação. |\n| `domain/intelligence/ActivityTrendEngine.kt` | Domain / Intelligence | Detecta variação entre duas janelas completas de sete dias; não acessa Android, BLE ou rede. |\n