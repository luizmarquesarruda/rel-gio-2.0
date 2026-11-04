# Mapa de módulos

## Estado estrutural observado

A base é um aplicativo Android nativo Kotlin. UI, ciclo de vida BLE e leitura Health Connect ainda estão parcialmente concentrados em `MainActivity.kt`; a separação é incremental, não concluída.

### Componentes após esta etapa

| Arquivo | Módulo | Responsabilidade |
|---|---|---|
| `device/ble/gatt/SerialGattOperationQueue.kt` | BLE transport / GATT | Serializar operações GATT assíncronas. Não interpreta payload. |
| `device/gt3max/protocol/WearfitAbFrameParser.kt` | Protocolo do dispositivo | Perfil NUS/AB candidato e parser restrito. Não controla conexão. |
| `notifications/NotificationCaptureService.kt` | Platform / Notifications | Captura consentida na variante companion. |
| `notifications/NotificationCaptureStore.kt` | Data / Notifications | Cache volátil, filtros e preferências. |
| `notifications/NotificationCenterActivity.kt` | UI / Notifications | Consentimento, seleção e prévias. |
| `HealthPrivacyActivity.kt` | UI / Privacy | Transparência e controles de dados de saúde. |
| `MainActivity.kt` | App / composição provisória | Ainda mantém UI, permissões, GATT e Health Connect. |

Os testes de fila e parser acompanham os respectivos pacotes de código.

## Estrutura-alvo

```text
com.luizmarquesarruda.relogio20/
├── app/                         # composição/inicialização
├── ui/                          # dashboard, device, health, history, notifications, settings
├── domain/
│   ├── health/                  # métrica, unidade, origem e qualidade
│   ├── activity/                # sessão/reconhecimento de atividade
│   ├── goals/                   # metas locais
│   └── device/                  # capacidade e estado do relógio
├── device/
│   ├── ble/
│   │   ├── core/                # scan, conexão, lifecycle GATT e reconexão
│   │   └── gatt/                # fila e operações GATT
│   └── gt3max/
│       ├── protocol/            # UUIDs/perfil, framing e parsers candidatos
│       └── metrics/             # mapeamento validado para o Domain
├── data/
│   ├── healthconnect/           # leitura/escrita Health Connect
│   ├── local/                   # persistência local versionada
│   └── notifications/           # retenção e consulta de prévias
├── platform/                    # Bluetooth Classic, mídia, chamadas, alarmes, câmera
└── common/                      # tempo e logging redigido
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

## Dívida técnica prioritária

1. Extrair scan/ciclo GATT de `MainActivity.kt` para `device/ble/core`, preservando fila e proteção contra callbacks antigos.
2. Extrair a consulta agregada para `data/healthconnect`, preservando a origem Health Connect.
3. Introduzir modelo Domain de métrica/proveniência antes de integrar FC, SpO₂, pressão, sono ou METT.
4. Separar adaptadores Platform de chamadas, mídia, alarmes e câmera somente quando os recursos forem selecionados.
5. Manter OTA, reset/factory restore, pagamentos e cartões de acesso bloqueados.

## Limite da entrega atual

Esta etapa move a fila GATT e o parser candidato, junto com os testes. `MainActivity.kt` ainda não foi dividido. Sensores e as demais funções anunciadas **não foram validados fisicamente**.
