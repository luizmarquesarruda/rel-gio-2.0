# Arquitetura do Relógio 2.0

## Plataforma e camadas

Aplicativo Android nativo Kotlin. Não há React Native nesta fase. O GT3 MAX é tratado como dispositivo BLE/GATT genérico; não presumir Wear OS, chipset ou SDK proprietário.

1. UI / Presentation — telas, estado visual e ações do usuário.
2. Domain — modelos de métricas, unidades, proveniência e validação; sem Android/BLE.
3. Device / BLE transport — scan, conexão GATT, callbacks, fila serial e reconexão.
4. Device / GT3 MAX protocol — framing e parser; semântica somente após capturas repetíveis.
5. Data / Local storage — histórico e preferências locais, schema versionado.
6. Data / Health Connect — adaptador separado, permissões por tipo e leitura autorizada.
7. Platform — APIs Android e permissões.
8. Privacy & diagnostics — consentimento, minimização, retenção e logs.

## Estado real nesta PR

- MainActivity.kt ainda concentra apresentação, permissões BLE e ciclo GATT.
- HealthConnectStepsRepository encapsula consultas agregadas de passos.
- DailyActivityHistoryRepository persiste observações locais versionadas e idempotentes.
- HealthConnectActivityHistorySync conecta a leitura agregada ao histórico, importando somente o dia anterior completo.
- O agregado mantém origem HEALTH_CONNECT_AGGREGATE e estado UNVALIDATED; não é atribuído ao GT3 MAX.
- Dias sem dados não são convertidos em zero.
- Não foram adicionadas permissões, dependências, rede, LLM ou escritas BLE proprietárias.

## Regras de dependência

- UI → Domain; UI não importa UUIDs ou parser.
- BLE transport → Android Bluetooth; não depende da camada de saúde.
- Domain → Kotlin/JDK; sem Android SDK.
- Health Connect adapter → Domain + API Health Connect; nunca atribui agregado multiorigem ao GT3 MAX.
- Storage → Domain; conserva origem, timestamps, qualidade e versão do esquema.
- Qualquer permissão nova exige finalidade clara, consentimento e teste de revogação.

## Segurança de comandos

- Manter somente leituras GATT e CCCD padrão nas ações explicitamente permitidas.
- Sem NUS RX, comandos proprietários, calibração, reset, factory restore ou OTA.
- Chamadas e áudio Classic são canais separados do BLE de sensores.

## Inteligência local — Fases 1 a 3

- Domain: DailyActivityObservation e ActivityTrendEngine, comparando duas janelas completas de sete dias.
- Data: histórico diário binário schema 1 via AtomicFile, com proteção contra corrupção e schema futuro.
- Integração Health Connect: importa somente o dia local anterior já completo, usando ZoneId e limites reais de calendário.
- O motor de tendências ainda não consome o histórico automaticamente.
- Escopo: IA-LOCAL-FASE-3.md.
