# Estrutura de pacotes — Relógio 2.0

## Base validada

Esta organização foi aplicada sobre a branch de hardening existente, não sobre uma estrutura Android genérica. O projeto observado contém um módulo Gradle `:app`, Kotlin nativo, Android Gradle Plugin e Kotlin DSL, com flavors `diagnostic` e `companion`.

O package base real é:

```text
com.luizmarquesarruda.relogio20
```

A interface atual usa Android Activities e Views. Não há Jetpack Compose nesta fase.

## Estrutura aplicada

```text
app/src/main/java/com/luizmarquesarruda/relogio20/
├── ui/screens/
│   ├── MainActivity.kt
│   ├── FeatureHubActivity.kt
│   ├── FeatureDiscoveryActivity.kt
│   ├── DiscoveryLibraryActivity.kt
│   └── HealthPrivacyActivity.kt
├── domain/
│   ├── diagnostics/BleDiagnosticReport.kt
│   └── health/
│       ├── HealthMetricSample.kt
│       ├── StepsSnapshot.kt
│       └── StepsCountFormatter.kt
├── data/
│   ├── discovery/
│   └── healthconnect/
└── device/
    ├── ble/
    │   ├── core/
    │   └── gatt/
    ├── discovery/
    └── gt3max/protocol/
```

As implementações de notificações permanecem em source sets de flavor:

- `app/src/companion/java/.../notifications`: serviço e interface de captura autorizada.
- `app/src/diagnostic/java/.../notifications`: tela informativa sem Notification Listener.

## Justificativas e limites

### UI

Activities foram agrupadas em `ui/screens`. Elas continuam Android Views. Não foram criados `ui/components` ou `ui/theme` artificiais porque não existem composables nem design system Compose no código atual. Tema de plataforma e recursos continuam sob configuração Android.

### BLE

O código BLE permanece em `device/ble` e a exploração GATT em `device/discovery`. O perfil e parser específico permanecem em `device/gt3max/protocol`. Isso evita colocar transporte, política de conexão e protocolo proprietário candidato em um `data/services` genérico.

As movimentações desta fase não alteram contratos, callbacks, UUIDs, permissões ou sequência de GATT. A extração do ciclo BLE ainda concentrado em `ui/screens/MainActivity.kt` é uma etapa posterior e separada.

### Dados

`data/healthconnect` continua isolando o adaptador Health Connect. `data/discovery` continua cuidando da biblioteca local e do inventário GATT.

O módulo atual não declara Room/SQLite, `@Entity`, `@Database` ou `Migration`. A persistência observada usa `AtomicFile` e `SharedPreferences`, com formatos/schemas próprios. Não foram introduzidos bancos relacionais ou migrações.

### Source sets

Os arquivos Kotlin permanecem nos source roots Android `src/main/java`, `src/test/java`, `src/companion/java` e `src/diagnostic/java`. O projeto usa esses diretórios hoje; mudar para `src/main/kotlin` exigiria alteração e validação explícita dos source sets de cada flavor/teste. Essa mudança não foi necessária para organizar os pacotes.

### Pacotes ainda não criados

- `ui/components` e `ui/theme`: dependem de uma futura adoção de Compose ou componentes extraídos.
- `data/services`: integrações existentes já têm fronteiras especializadas; não criar um agrupador genérico.
- `data/config`: criar somente quando houver configuração/infraestrutura real que justifique o pacote.
- `utils`: formatadores foram alocados no domínio ou BLE conforme sua responsabilidade; evitar um pacote sem critério.

## Verificações de regressão exigidas

Antes de promover a branch:

1. Executar testes unitários das duas variantes.
2. Executar Android Lint para `diagnosticDebug` e `companionDebug`.
3. Compilar os dois APKs de depuração.
4. Conferir Manifestos mesclados e manter Notification Listener somente em `companion`.
5. Verificar os contratos de BLE, armazenamento local e Health Connect.
6. Testar em dispositivo real quando a validação física for necessária.

Nenhuma dessas validações é declarada como concluída nesta alteração.
