# Estrutura de pacotes — Relógio 2.0

## Estado implementado

O aplicativo continua em um único módulo Gradle Android, `:app`, com Kotlin nativo e Activities/Views. Esta documentação registra a organização de pacotes aplicada na branch `refactor/package-layer-boundaries-20261105`.

```text
com.luizmarquesarruda.relogio20/
├── ui/
│   ├── screens/
│   │   ├── MainActivity.kt
│   │   ├── FeatureHubActivity.kt
│   │   ├── FeatureDiscoveryActivity.kt
│   │   ├── DiscoveryLibraryActivity.kt
│   │   └── HealthPrivacyActivity.kt
│   └── formatters/
│       └── StepsCountFormatter.kt
├── domain/
│   ├── diagnostics/
│   │   └── BleDiagnosticReport.kt
│   └── health/
│       ├── HealthMetricSample.kt
│       └── StepsSnapshot.kt
├── device/
│   ├── ble/
│   │   ├── core/
│   │   │   ├── BleDeviceIdentity.kt
│   │   │   ├── BleScanCandidatePolicy.kt
│   │   │   ├── BleValueFormatter.kt
│   │   │   ├── BleReconnectPolicy.kt
│   │   │   ├── BleReconnectCoordinator.kt
│   │   │   └── GattCallbackSafetyPolicy.kt
│   │   └── gatt/
│   │       ├── SerialGattOperationQueue.kt
│   │       └── BluetoothCurrentTimePayload.kt
│   ├── discovery/
│   └── gt3max/protocol/
├── data/
│   ├── discovery/
│   └── healthconnect/
└── notifications/
    # componentes específicos dos flavors companion/diagnostic
```

## Responsabilidades e dependências

| Pacote | Responsabilidade | Limite |
|---|---|---|
| `ui.screens` | Activities, navegação e apresentação | Não deve decodificar payloads nem decidir semântica de métricas. |
| `ui.formatters` | Formatação localizada para exibição | Não contém regras de domínio nem acesso Android Bluetooth. |
| `domain.health` | Modelos tipados, unidade, origem e qualidade | Kotlin puro; não conhece Android, GATT ou Health Connect. |
| `domain.diagnostics` | Relatórios locais sanitizados | Não exporta texto de notificações nem endereço Bluetooth integral. |
| `device.ble.core` | Identidade/pistas, política de reconexão e segurança de callbacks | Não interpreta métricas do relógio. |
| `device.ble.gatt` | Fila serial e estruturas Bluetooth SIG | Operações permanecem condicionadas à capacidade anunciada e autorização explícita existente. |
| `device.discovery` | Inventário, fingerprint e exploração GATT | Ausência de UUID/retorno não prova ausência física de sensor. |
| `device.gt3max.protocol` | Perfil candidato e framing específico | Sem semântica proprietária inferida sem capturas repetíveis. |
| `data.discovery` | Inventário e biblioteca local | Preservar integridade de schema, dados corrompidos e escrita atômica. |
| `data.healthconnect` | Consulta agregada e permissões | Não atribuir agregado multiorigem ao GT3 MAX. |
| `notifications` | Captura consentida na variante companion | Conteúdo restrito, volátil e não transmitido. |

## Limite desta refatoração

Esta alteração é um reposicionamento de classes e imports, com alinhamento do Manifesto e dos testes de caminho. Não cria módulos Gradle, não introduz Jetpack Compose e não altera deliberadamente comportamento BLE, callbacks, reconexão, CTS, decoders, armazenamento ou Health Connect.

`MainActivity` continua sendo uma classe grande e mantém responsabilidades de UI, permissões, scan, conexão GATT, callbacks, exploração e integração com Health Connect. A próxima etapa correta é extrair o ciclo BLE em unidades com sessão/epoch explícita, fila serial e cancelamento seguro, preservando a Activity apenas como camada de apresentação/composição. Não executar essa extração sem testes de regressão de reconexão, callback atrasado, Service Changed, desconexão e retomada do app.

## Validação

- Alterações de pacote e imports: preparadas nesta branch.
- Testes Kotlin/Python, Lint e build: **não executados** nesta etapa.
- CI: **não disparada**.
- Galaxy A06 / Android 16 / GT3 MAX: **não validado fisicamente**.
