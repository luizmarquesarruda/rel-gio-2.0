# Mapa de módulos

## Estado estrutural observado

A base é um aplicativo Android nativo Kotlin. UI, ciclo de vida BLE e leitura Health Connect ainda estão parcialmente concentrados em MainActivity.kt; a separação é incremental, não concluída.

### Componentes implementados

| Arquivo | Módulo | Responsabilidade |
|---|---|---|
| domain/health/HealthMetricSample.kt | Domain / Health | Tipo, unidade, origem, timestamps e estado de validação sem dependência Android/BLE. |
| device/ble/gatt/SerialGattOperationQueue.kt | BLE transport / GATT | Serializar operações GATT assíncronas. Não interpreta payload. |
| device/gt3max/protocol/WearfitAbFrameParser.kt | Protocolo do dispositivo | Perfil NUS/AB candidato e parser restrito. Não controla conexão. |
| notifications/NotificationCaptureService.kt | Platform / Notifications | Captura consentida na variante companion. |
| notifications/NotificationCaptureStore.kt | Data / Notifications | Cache volátil, filtros e preferências. |
| notifications/NotificationCenterActivity.kt | UI / Notifications | Consentimento, seleção e prévias. |
| HealthPrivacyActivity.kt | UI / Privacy | Transparência e controles de dados de saúde. |
| MainActivity.kt | App / composição provisória | Ainda mantém UI, permissões, GATT e Health Connect. |

Os testes unitários acompanham fila GATT, parser candidato, formatadores e contrato Domain de métricas.

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

## Dívida técnica prioritária

1. Extrair scan/ciclo GATT de MainActivity.kt para device/ble/core, preservando fila e proteção contra callbacks antigos.
2. Extrair a consulta agregada para data/healthconnect, preservando a origem Health Connect.
3. Criar persistência local versionada para HealthMetricSample, com migração e testes de integridade.
4. Mapear quadros de sensores para Domain somente após capturas BLE repetíveis e validação física.
5. Separar adaptadores Platform de chamadas, mídia, alarmes e câmera somente quando os recursos forem selecionados.
6. Manter OTA, reset/factory restore, pagamentos e cartões de acesso bloqueados.

## Limite da entrega atual

A fila GATT e o parser candidato foram isolados com testes. O modelo Domain inicial e seus testes foram adicionados. MainActivity.kt ainda não foi dividido; Health Connect ainda não foi extraído. Nenhum sensor do GT3 MAX foi validado fisicamente.
