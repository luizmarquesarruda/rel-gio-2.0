# Estrutura de pacotes e módulos — Relógio 2.0

## Estado implementado

O repositório contém dois módulos Gradle:

- `:app`: aplicativo Android nativo em Kotlin, Activities e Views.
- `:macrobenchmark`: módulo de teste de macrobenchmark, direcionado à variante `diagnostic`.

O módulo `:app` possui os flavors `diagnostic` e `companion`. A variante `companion` declara o serviço local de captura de notificações; a variante `diagnostic` substitui a tela por uma explicação e não declara `NotificationListenerService`.

Não há Jetpack Compose, Hilt ou Room configurados. A persistência atual é baseada em `SharedPreferences` e arquivos privados com `AtomicFile`. Room permanece uma decisão futura, sujeita a medição no dispositivo e aprovação específica.

## Organização de pacotes

```text
com.luizmarquesarruda.relogio20/
├── RelogioApplication.kt
├── ui/
│   ├── RelogioUi.kt
│   ├── formatters/
│   └── screens/
│       ├── DashboardActivity.kt
│       ├── MainActivity.kt
│       ├── HealthOverviewActivity.kt
│       ├── ActivityOverviewActivity.kt
│       ├── SleepOverviewActivity.kt
│       ├── HistoryActivity.kt
│       ├── MoreResourcesActivity.kt
│       ├── SettingsActivity.kt
│       ├── FeatureDiscoveryActivity.kt
│       ├── DiscoveryLibraryActivity.kt
│       ├── FeatureHubActivity.kt
│       ├── LocalToolsActivity.kt
│       ├── TimeWeatherActivity.kt
│       └── HealthPrivacyActivity.kt
├── domain/
│   ├── activity/
│   ├── diagnostics/
│   ├── health/
│   ├── session/
│   ├── telemetry/
│   └── weather/
├── device/
│   ├── ble/
│   │   ├── core/
│   │   └── gatt/
│   ├── discovery/
│   └── gt3max/protocol/
├── data/
│   ├── discovery/
│   ├── healthconnect/
│   ├── preferences/
│   └── weather/
└── notifications/
    ├── app/src/companion/java/  (listener e captura local)
    └── app/src/diagnostic/java/ (tela informativa sem listener)
```

## Responsabilidades e limites

| Pacote | Responsabilidade | Limite |
|---|---|---|
| `ui` | Navegação, componentes visuais, Activities e apresentação | Não deve decidir semântica de payloads nem atribuir métricas a uma fonte não validada. |
| `domain.health` | Modelos tipados, unidades, origem e validação | Kotlin puro; não conhece Android, GATT ou Health Connect. |
| `domain.session` / `domain.telemetry` | Estado de sessões, watchdog e telemetria | Não inicia operações Android diretamente. |
| `device.ble.core` | Identidade/pistas, reconexão e segurança de callbacks | Não interpreta métricas proprietárias. |
| `device.ble.gatt` | Fila serial, política de acesso e contratos Bluetooth SIG | Escritas dependem de capacidade anunciada e consentimento explícito. |
| `device.discovery` | Inventário, fingerprint, plano e decodificação SIG | Ausência de UUID ou retorno não prova ausência física de sensor. |
| `device.gt3max.protocol` | Perfil candidato e framing AB/NUS | Sem semântica proprietária inferida sem capturas repetíveis. NUS RX e DFU/OTA continuam bloqueados. |
| `data.discovery` | Catálogo GATT e biblioteca de captura local | Preservar integridade de schema, conteúdo corrompido e escrita atômica; expor falhas do writer. |
| `data.healthconnect` | Consulta agregada e permissões Health Connect | Não atribuir agregado multiorigem ao GT3 MAX. |
| `data.preferences` | Preferências compartilhadas do aplicativo | Uma única fonte para o escopo de origem do Health Connect. |
| `data.weather` | Geocodificação/consulta Open-Meteo e cache | Enviar somente localidade selecionada manualmente; não transmitir identificador do relógio ou dados de saúde. |
| `notifications` | Captura consentida e volátil na variante companion | Conteúdo restrito, sem persistência em arquivo ou transmissão ao relógio nesta versão. |

## Navegação

O painel principal apresenta seis áreas secundárias:

1. Conexão e dispositivo.
2. Saúde.
3. Atividade.
4. Sono.
5. Histórico.
6. Mais recursos.

Configurações é acessada por Mais recursos, não como uma sétima área de primeiro nível. O laboratório BLE, a biblioteca, o catálogo, as ferramentas locais, clima, notificações (companion) e privacidade são recursos subordinados.

## Limites arquiteturais conhecidos

`MainActivity` ainda é uma classe grande e concentra UI, permissões, scan, conexão GATT, callbacks, exploração, reconexão, telemetria, captura e parte da integração com Health Connect. A próxima refatoração arquitetural deverá extrair o ciclo BLE em um orquestrador com sessão/epoch explícita, fila serial e cancelamento seguro, preservando a Activity apenas como composição/apresentação.

As Activities de atividade e histórico ainda instanciam o adapter Health Connect diretamente. Uma etapa posterior deve introduzir um contrato de domínio e migrar a dependência para UI → domínio → data, sem alterar as consultas ou a atribuição de origem.

Não iniciar extração ampla sem testes de regressão para callback atrasado, Service Changed, timeout, reconexão, desconexão, retomada do app e cancelamento.

## Validação

- Build/testes unitários das variantes debug e compilação de macrobenchmark: realizados na CI de referência `42230653007`, antes das correções atuais.
- Android Lint e guardrail de permissões naquela execução: falharam; correções incluídas na branch e aguardam nova CI.
- KtLint: baseline de legado em preparação; após aprovação e commit do baseline, verificações serão bloqueantes para novos desvios.
- Galaxy A06 / Android 16 / GT3 MAX: **não validado fisicamente**.
- Medição real do macrobenchmark: **não executada**.
- Métricas fisiológicas do GT3 MAX: **não validadas semanticamente nem fisicamente**.
