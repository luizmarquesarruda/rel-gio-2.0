# Mapa de módulos

## Estado estrutural observado

A base é um aplicativo Android nativo Kotlin. O adaptador de consulta agregada de passos do Health Connect foi extraído para data/healthconnect; UI e ciclo de vida BLE ainda estão parcialmente concentrados em MainActivity.kt. A separação é incremental, não concluída.

### Componentes implementados

| Arquivo | Módulo | Responsabilidade |
|---|---|---|
| domain/health/HealthMetricSample.kt | Domain / Health | Tipo, unidade, origem, timestamps e estado de validação sem dependência Android/BLE. |
| domain/health/StepsSnapshot.kt | Domain / Activity | Snapshot do agregado de passos; diferencia zero de ausência e preserva intervalo, recebimento e origem Health Connect. |
| data/healthconnect/HealthConnectStepsRepository.kt | Data / Health Connect | Disponibilidade do provider, permissão READ_STEPS e consulta agregada diária, sem atribuir a origem ao GT3 MAX. |
| device/ble/gatt/SerialGattOperationQueue.kt | BLE transport / GATT | Serializar operações GATT assíncronas. Não interpreta payload. |
| device/gt3max/protocol/Gt3MaxCallDeviceProfile.kt | Perfil do dispositivo | Identificadores GT3 MAX Call informados e fingerprint GATT da captura; sem autenticação de hardware/firmware. |
| device/gt3max/protocol/WearfitAbFrameParser.kt | Protocolo do dispositivo | Perfil NUS/AB candidato e parser restrito. Não controla conexão. |
| notifications/NotificationCaptureService.kt | Platform / Notifications | Captura consentida na variante companion. |
| notifications/NotificationCaptureStore.kt | Data / Notifications | Cache volátil, filtros e preferências. |
| notifications/NotificationCenterActivity.kt | UI / Notifications | Consentimento, seleção e prévias. |
| HealthPrivacyActivity.kt | UI / Privacy | Transparência e controles de dados de saúde. |
| MainActivity.kt | App / composição provisória | Mantém UI, permissões e ciclo de vida GATT; delega a consulta de passos ao repositório Health Connect. |

Os testes unitários acompanham fila GATT, parser candidato, perfil GATT GT3 MAX Call, formatadores e contrato Domain de métricas. A inscrição automática NUS TX exige nome reconhecido e fingerprint GATT completo da captura; não envia payload à característica NUS RX.

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

A fila GATT, o parser candidato e o adaptador Health Connect de passos foram isolados com testes. O modelo Domain inicial e o snapshot de passos agregados têm testes unitários. MainActivity.kt ainda não foi dividido no transporte BLE. Health Connect segue somente leitura de passos agregados; nenhum sensor do GT3 MAX foi validado fisicamente.
