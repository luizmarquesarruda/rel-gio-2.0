# Arquitetura do Relógio 2.0

## Plataforma atual

A implementação em andamento é um protótipo Android nativo em Kotlin. Não há uma aplicação React Native nesta fase. O GT3 MAX é tratado como dispositivo BLE/GATT genérico; não presumir Wear OS, chipset ou SDK proprietário.

## Camadas

1. **UI / Presentation** — telas, estado visual, acessibilidade e ações do usuário. Não conhece UUIDs nem payloads proprietários.
2. **Domain** — modelos de métricas/sessões, unidades, proveniência, qualidade, metas e casos de uso. Não depende de Android, BLE ou Wearfit Pro.
3. **Device / BLE transport** — scan, conexão GATT, callbacks, fila serial, descoberta, reconexão e encerramento. Não interpreta métricas.
4. **Device / GT3 MAX protocol** — perfil candidato, framing, parser e correlação de comandos/respostas. Só promove semântica após capturas repetíveis.
5. **Data / Local storage** — histórico e preferências locais, esquema versionado, origem e timestamps.
6. **Data / Health Connect** — adaptador separado para leitura/escrita, permissões por tipo, consentimento e idempotência.
7. **Platform** — APIs Android para Bluetooth Classic, mídia, chamadas, alarmes, notificações, câmera e permissões.
8. **Privacy & diagnostics** — consentimento, minimização, retenção e logs sem conteúdo sensível.

## Estado real nesta PR

- `ui/screens/MainActivity.kt` ainda concentra apresentação, permissões BLE e conexão/ciclo GATT. A consulta agregada de passos do Health Connect foi extraída para data/healthconnect.
- domain/health/HealthMetricSample.kt foi criado como modelo Kotlin puro para tipo, unidade, origem, timestamps e estado de validação. Não recebe bytes BLE nem grava dados.
- O contrato exige unidade compatível, valores finitos/não negativos, timestamp de recebimento e origem explícita; passos são inteiros e SpO₂ fica no intervalo de 0 a 100.
- METT tem unidade MET e não é convertido automaticamente em calorias. Pressão arterial/SpO₂ não são tratados como medições clínicas.
- SerialGattOperationQueue foi isolada em device/ble/gatt.
- GattCallbackSafetyPolicy isola as regras de callbacks: só aceita a conexão atual e a operação ativa; Service Changed invalida a tabela anterior e solicita redescoberta quando permitido.
- A biblioteca local usa cache e fila de escrita serial para não fazer I/O de arquivo dentro dos callbacks BLE; o conteúdo continua local e bounded.
- WearfitProtocolProfile, WearfitAbFrame e WearfitAbFrameParser foram isolados em device/gt3max/protocol. O nome do pacote não confirma chipset nem identidade do dispositivo.
- Os testes acompanham os pacotes correspondentes; os testes do contrato Domain verificam unidades, limites e preservação de origem.
- Notification Listener permanece na variante companion; diagnostic não deve declarar essa capacidade.
- Health Connect é acessado pelo HealthConnectStepsRepository em data/healthconnect; `ui/screens/MainActivity` consome resultado tipado e não monta consultas Health Connect.
- `FeatureDiscoveryStore` aplica schema estrito e bloqueio compartilhado entre instâncias; corrompido/futuro é preservado sem substituição. A atualização usa `SharedPreferences.apply()` no callback BLE para evitar I/O síncrono; sua confirmação é somente de atualização em memória/agendamento de disco.
- `DiscoveryLibraryStore` mantém `AtomicFile`, bloqueio compartilhado e cache de leitura; gravações de telemetria são serializadas em fila limitada fora do thread de callbacks. Sessões e exportações fazem flush antes das transições.
- `GattCallbackSafetyPolicy` aceita apenas callbacks da conexão atual e chave de operação ativa; `onServiceChanged` invalida a tabela antiga e pede redescoberta quando a sessão pertence ao Relógio 2.0.
- `DiscoveryLibraryStore` mantém `AtomicFile`, com bloqueio compartilhado no processo e validação do codec/sessões antes de aceitar registros.
- A validação de origem e intervalo de Health Connect permanece separada de qualquer futura amostra BLE do GT3 MAX.
- Não foram adicionados serviços, permissões ou escritas BLE proprietárias.

## Organização física aplicada

- Activities de interface agrupadas em `ui/screens`; a UI segue nativa (Android Views/Activities), sem Jetpack Compose nesta versão.
- Identidade, seleção de candidato e formatação hexadecimal BLE ficam em `device/ble/core`.
- Composição e sanitização do relatório diagnóstico ficam em `domain/diagnostics`.
- Formatação localizada do agregado de passos fica em `ui/formatters`; `domain/health` permanece com modelos e contratos.
- Health Connect e persistência da biblioteca continuam em `data/healthconnect` e `data/discovery`.
- Source root permanece `app/src/main/java` para manter os source sets atuais sem mudança de Gradle.
- Não foram introduzidos Room, SQLite, migrações SQL, Compose, permissões ou operações GATT adicionais.
- A separação interna de `MainActivity` continua pendente; sua movimentação de pacote não equivale à extração do ciclo BLE.

## Regras de dependência

- UI → Domain; UI não importa UUIDs ou parser.
- BLE transport → Android Bluetooth; não depende da camada de saúde.
- Driver/protocolo → Kotlin puro e bytes; não depende de Android UI.
- Domain → Kotlin/JDK; sem Android SDK.
- Health Connect adapter → Domain + API Health Connect; nunca atribui um agregado multiorigem ao GT3 MAX.
- Storage → Domain; conserva origem, timestamps, qualidade e versão do esquema.
- Platform adapters → APIs Android com permissões mínimas.

## Contrato mínimo de métrica

O contrato implementado está em [CONTRATO-METRICAS-SAUDE.md](CONTRATO-METRICAS-SAUDE.md). Ele registra tipo, valor, unidade, origem, instante de recebimento, instante da medição quando disponível e estado de validação. Ausência não é zero; estimativa não é medição confirmada; bytes desconhecidos permanecem brutos.

## Segurança de comandos

- Manter somente leituras GATT e CCCD padrão nas ações explícitas existentes.
- Sem NUS RX, comando proprietário, calibração, lock/unlock, vibração, mudança de idioma/hora, reset, factory restore ou OTA nesta fase.
- Chamadas e áudio Classic são canais separados do BLE de sensores.
- Qualquer permissão nova exige recurso funcional, finalidade clara, consentimento e teste de revogação.

## Sequência de extração

1. Agrupar Activities em ui/screens e mover políticas/formatadores puros para device/ble/core e domain (concluído nesta fase).
2. Extrair scan/máquina de estados GATT de `ui/screens/MainActivity` para `device/ble/core`, preservando fila e callbacks.
3. Manter a consulta Health Connect isolada em `data/healthconnect` (já implementada).
4. Evoluir armazenamento local versionado de amostras e sessões, com deduplicação e migrações testadas.
5. Implementar parsers somente após evidência repetível por métrica.
6. Extrair adaptadores Platform quando os recursos Android forem priorizados.
7. Validar cada extração em teste, lint e aparelho antes de avançar.

Cada extração deve ser pequena, testada e reversível. Não misturar refatoração com engenharia reversa de protocolo.
