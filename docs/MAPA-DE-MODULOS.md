# Mapa de módulos

## Componentes implementados

| Arquivo | Módulo | Responsabilidade |
|---|---|---|
| domain/health/HealthMetricSample.kt | Domain / Health | Tipo, unidade, origem, timestamps e validação. |
| domain/health/StepsSnapshot.kt | Domain / Activity | Snapshot agregado; diferencia zero de ausência e preserva intervalo/origem. |
| data/healthconnect/HealthConnectStepsRepository.kt | Data / Health Connect | Permissão READ_STEPS e consulta de hoje/dias completos. |
| data/healthconnect/HealthConnectActivityHistorySync.kt | Data / Health Connect | Importa dia local anterior completo para o histórico. |
| data/healthconnect/HealthConnectDayWindow.kt | Data / Health Connect | Calcula intervalos com Instant + ZoneId; recusa dias incompletos. |
| data/intelligence/DailyActivityHistoryRepository.kt | Data / Intelligence | Upsert idempotente por dia/origem e proteção contra sobrescrita antiga. |
| data/intelligence/DailyActivityHistoryCodec.kt | Data / Intelligence | Codec binário schema 1 com validação e limites. |
| data/intelligence/DailyActivityHistoryAtomicFile.kt | Data / Local storage | Persistência privada via AtomicFile. |
| domain/intelligence/ActivityTrendEngine.kt | Domain / Intelligence | Compara janelas completas de sete dias; sem Android/BLE/rede. |
| test/data/healthconnect/HealthConnectActivityHistorySyncTest.kt | Testes / Data | Testa fuso, ausência, dia incompleto, idempotência e falhas. |
| test/data/healthconnect/HealthConnectDayWindowTest.kt | Testes / Data | Testa limites de calendário e transições DST de 23/25 horas. |
| MainActivity.kt | App / composição provisória | Coordena UI, ciclo de vida BLE e sincronização Health Connect em primeiro plano. |

## Limites

MainActivity ainda coordena o ciclo de vida BLE. Health Connect segue somente leitura de agregados de passos. A origem não prova que os passos vieram do GT3 MAX; nenhum sensor do relógio foi validado fisicamente. O motor de tendências ainda não consome o histórico automaticamente.
