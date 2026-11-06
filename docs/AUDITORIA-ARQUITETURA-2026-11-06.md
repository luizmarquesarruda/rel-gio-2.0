# Auditoria técnica e arquitetural — 06/11/2026

**Escopo:** repositório real `luizmarquesarruda/rel-gio-2.0`, branch `feat/ui-8-screen-layout-20261106`, revisão do commit que implementou o painel 1 + 7 e do CI Android.

## 1. Resultado executivo

A base real é um aplicativo Android nativo em Kotlin, com Views Android, módulo principal `:app` e módulo `:macrobenchmark`. A arquitetura não é React Native, não usa Jetpack Compose na implementação atual e não possui Hilt como contêiner de injeção. O trecho recebido com `com.yourpackage.name`, Compose, Hilt, Lombok e um novo `BleConnectionManager` não pode ser copiado para este repositório sem criar uma segunda arquitetura paralela.

A separação atual já possui componentes úteis e testados: políticas BLE puras, fila serial GATT, parser/protocolo candidato do GT3 MAX, políticas de reconexão, stores locais e adaptador Health Connect de passos. O principal débito arquitetural é a concentração de orquestração em `MainActivity` e o acesso direto de Activities a componentes da camada `data`.

**Nenhum protocolo NUS, UUID de serviço/characteristic, comando de escrita, integração Hilt/Compose, Room ou gravação de frequência cardíaca foi introduzido nesta correção.**

## 2. CI / Gradle — investigação e correção aplicada

### Evidências

- O workflow configurava Gradle **9.5.0**.
- O build raiz declara Android Gradle Plugin **8.13.2**.
- O diário já registrava falha anterior de configuração das dependências de variantes, com mensagem de configuração `diagnosticDebugImplementation` ausente.
- O run #452 falhou durante o bootstrap de verificação de dependências; as etapas de Lint, build, auditoria de APK e publicação foram ignoradas.
- O repositório ainda não continha `gradle/verification-metadata.xml`, portanto o bootstrap executava testes e builds para gerar os checksums iniciais.

A combinação Gradle 9.5.0 / AGP 8.13.2 não é a combinação suportada indicada para essa linha do Android Gradle Plugin. Isso é uma causa provável e tecnicamente consistente com as falhas de configuração observadas; o log detalhado do bootstrap não ficou disponível pela interface de leitura usada nesta auditoria. A próxima execução é necessária para confirmar a correção.

### Correções aplicadas no workflow

1. Gradle foi alinhado para **8.13**, compatível com a linha AGP 8.13.x.
2. Quando o arquivo de verificação ainda não existe, o bootstrap já executa os testes unitários e os builds das duas variantes. A etapa normal de build agora só executa quando o bootstrap foi ignorado por já existir metadata; isso evita repetir o mesmo grafo Gradle no mesmo job.
3. As etapas de guardrails e auditoria de APK aceitam como build válido o sucesso do bootstrap ou da etapa normal de build, sem permitir que uma falha no bootstrap seja mascarada.
4. A geração inicial de metadata continua sendo trust-on-first-use. O artefato deve ser revisado antes de ser versionado; não se deve aceitar checksums sem revisão.

**Estado:** correção enviada à branch; CI posterior ainda precisa confirmar Gradle, dependências, testes, Lint, APK e guardrails.

## 3. Arquitetura encontrada

### Pontos positivos

- `domain/telemetry`, `domain/health` e políticas em `device/ble/core` mantêm regras puras e testáveis.
- `SerialGattOperationQueue` isola serialização de operações GATT.
- `GattCallbackSafetyPolicy` centraliza critérios de aceitação de callbacks.
- Parser e framing candidatos do GT3 MAX ficam separados da UI.
- `data/healthconnect/HealthConnectStepsRepository` concentra consultas Health Connect, verifica disponibilidade/permissão e aplica política de origem.
- A ausência de registros é preservada como ausência; a implementação real não converte indisponibilidade em zero.
- Stores locais e biblioteca de descobertas têm políticas de integridade e persistência próprias.
- O launcher e as sete telas novas não contêm UUIDs nem implementam transporte BLE.

### Débitos arquiteturais prioritários

**P1 — MainActivity com responsabilidades demais.** A Activity ainda concentra ciclo de vida BLE, scan, conexão GATT, callbacks, reconexão, descoberta, telemetria, captura, persistência e partes da apresentação/Health Connect. Isso aumenta acoplamento, dificulta testes de ciclo de vida e torna mudanças de UI arriscadas.

**P1 — UI conhece a camada data.** `ActivityOverviewActivity` e `HistoryActivity` instanciam diretamente `HealthConnectStepsRepository`. A regra documentada é UI → Domain, mas a dependência real está UI → Data. O próximo recorte deve introduzir contratos/casos de uso Kotlin puros no domínio e deixar a composição Android fornecer a implementação, sem levar classes Android para o domínio.

**P2 — Composição e injeção manual.** Os repositórios são criados por `lazy` nas Activities. Não há contêiner de DI no projeto atual. Isso não exige adicionar Hilt: uma fábrica/composição pequena e explícita pode resolver a dependência sem ampliar o grafo de plugins.

**P2 — Activities de tela repetem orquestração de permissão e consulta.** Atividade e Histórico repetem estados de disponibilidade/permissão/erro. A política deve ser compartilhada no domínio/aplicação, enquanto cada tela permanece responsável apenas por renderizar estado e encaminhar ações.

**P2 — MainActivity ainda contém UI e infraestrutura no mesmo arquivo.** A extração deve ocorrer em passos pequenos: primeiro coordenador de sessão BLE, depois gateway/callback GATT, depois casos de uso de descoberta/captura. A Activity deve permanecer adaptadora de ciclo de vida, não ser reescrita de uma vez.

## 4. Avaliação do código enviado

O exemplo enviado não corresponde à estrutura real e apresenta problemas que impedem sua adoção direta:

- `android.bluetooth.BluetoothDevice` no pacote Domain viola a regra de domínio independente de Android.
- O uso fixo de UUIDs Nordic UART Service presume que o relógio oferece NUS; isso não está comprovado para o GT3 MAXX e não deve substituir o protocolo observado no projeto.
- `connect()` retorna `Result.success` logo após solicitar `connectGatt`, antes de receber confirmação de conexão ou descoberta de serviços.
- `safelyDisconnect()` chama `disconnect()` e `close()` imediatamente; fechar o GATT antes do callback de encerramento pode cortar o fluxo de desconexão.
- `setCharacteristicNotification()` e a escrita de CCCD não verificam os resultados nem serializam a operação com outras operações GATT.
- A falha de descoberta de serviços não emite sempre um evento de erro detalhado.
- `HealthConnectAdapter.readAggregatedSteps()` devolve zero quando o provider está indisponível, confundindo ausência/indisponibilidade com zero passos.
- A gravação de frequência cardíaca proposta não comprova origem, timestamp do sensor, consentimento nem permissão WRITE; o projeto real não deve gravar dados de saúde sintéticos.
- `lombok.CustomLog` está sem uso aparente e não corresponde à stack atual.
- `HealthMetricEntity(...)` está incompleto no trecho e não justifica introduzir Room ou outra persistência.

## 5. Direção arquitetural aprovada para a base existente

Manter Kotlin nativo e Views Android nesta etapa. Não adicionar Compose, Hilt, Lombok, Room ou novo módulo por conveniência.

Direção de dependências:

```text
UI Activities
    ↓
Domain / Application contracts + use cases (Kotlin puro)
    ↓
Interfaces de repositório
    ↑
Data adapters (Health Connect, persistência local)
    ↑
Platform adapters (Android APIs)

MainActivity (temporariamente)
    ↓
BLE session coordinator
    ↓
GATT gateway + fila serial
    ↓
GT3 MAX protocol candidate parser
```

Regras obrigatórias:

- Domain não importa Android Bluetooth, Context, Health Connect SDK ou Views.
- BLE transport não decide semântica de métricas.
- O parser não envia comandos proprietários.
- NUS/CTS ou qualquer UUID só pode ser usado quando a capacidade for observada e validada no dispositivo.
- Valores ausentes não são zero; agregados multiorigem não são atribuídos exclusivamente ao GT3 MAXX.
- Não gravar frequência cardíaca ou outras métricas no Health Connect até existir amostra de origem validada e permissão explícita do tipo correspondente.
- Nenhuma alteração no scan, fila GATT, callbacks, reconexão ou protocolo deve ser misturada à refatoração de UI.

## 6. Próximos gates de validação

1. Confirmar a CI após o alinhamento Gradle 8.13 / AGP 8.13.2.
2. Revisar o artefato de bootstrap SHA-256 e versionar metadata somente após revisão dos checksums.
3. Executar KtLint, Android Lint, testes Kotlin, builds diagnostic/companion e módulo Macrobenchmark.
4. Confirmar guardrails de dependências/permissões, manifestos, assinaturas e hashes dos APKs.
5. Validar instalação, navegação, acessibilidade e rotação no Galaxy A06.
6. Validar conexão e telemetria no GT3 MAXX em teste físico separado.

Até esses gates concluírem, build, APK e funcionamento físico permanecem **não validados**.
