# Ferramentas de diagnóstico, integridade e desempenho

## LeakCanary

A dependência `com.squareup.leakcanary:leakcanary-android:2.14` é aplicada somente à variante `diagnosticDebug`. Não deve aparecer em `companionDebug`, em variantes release nem em `diagnosticBenchmark`.

O uso é local e voltado ao diagnóstico de vazamentos de memória em Activities, Views, callbacks e referências retidas após encerramento de telas ou sessões. A biblioteca pode gerar heap dumps; esses arquivos podem conter dados pessoais, dados de saúde, payloads BLE e conteúdo transitório de notificações.

Regras:
- Não anexar heap dumps, logs ou relatórios com conteúdo sensível a issues, PRs ou artefatos públicos.
- Não copiar heap dumps para o repositório ou para serviços externos.
- Descartar dumps locais após a investigação.
- Não usar esta variante como APK de produção.
- O guardrail de dependências confirma a presença em `diagnosticDebug` e a ausência nas demais variantes auditadas.

O arquivo `.gitignore` já ignora `*.hprof`, dumps e logs. Essa proteção não substitui descarte seguro dos arquivos locais.

## Integridade das dependências Gradle

O Gradle Dependency Verification usa `gradle/verification-metadata.xml` e SHA-256. Quando o arquivo é versionado, o Gradle verifica automaticamente artefatos e metadados baixados em todas as tarefas/builds compatíveis.

A primeira CI gera um artefato temporário `gradle-dependency-verification-bootstrap` porque a branch ainda não contém metadados. Esse arquivo deve ser inspecionado, ter seus componentes e checksums revisados e somente então ser versionado em `gradle/verification-metadata.xml`. O bootstrap é trust-on-first-use: a geração, isoladamente, não prova autenticidade do primeiro download.

Após o arquivo revisado entrar no repositório:
1. A etapa de bootstrap deixa de executar.
2. O Gradle passa a aplicar verificação estrita por padrão.
3. Novas dependências/versões sem checksum fazem os builds falharem até que o metadado seja revisado e atualizado.
4. A atualização deve usar `gradle --write-verification-metadata sha256 help`, comparar o diff e validar as novas coordenadas em fontes confiáveis.

Não aceitar metadados vazios, não confiar em padrões amplos de `trusted-artifacts` e não substituir SHA-256 por MD5/SHA-1.

## Renovate

O arquivo raiz `renovate.json` configura o Renovate para:
- Atualizar dependências e plugins Gradle e versões de GitHub Actions.
- Detectar a versão Gradle fixada no workflow por um custom manager.
- Aguardar três dias desde a publicação quando a fonte fornece timestamp.
- Agrupar atualizações patch/minor e exigir aprovação pelo Dependency Dashboard para majors.
- Limitar a três PRs concorrentes e uma PR por hora.
- Manter `automerge=false`: todas as mudanças exigem revisão e CI.

A configuração no repositório não instala nem autentica o Renovate App. A criação automática de PRs só começa quando a aplicação Renovate estiver habilitada para este repositório, ou quando houver uma instância self-hosted autorizada. Até isso ocorrer, o arquivo é configuração preparada, não monitoramento ativo.

O projeto não possui Gradle Wrapper versionado; por isso, Renovate atualiza a versão Gradle do workflow por regex. Criar wrapper é uma tarefa separada, pois exige adicionar e validar os scripts e o JAR oficiais do Gradle Wrapper.

## AndroidX Macrobenchmark

Foi criado o módulo separado `:macrobenchmark`, usando `com.android.test` e AndroidX Macrobenchmark 1.5.0. A primeira medição é `StartupTimingMetric` com inicialização fria, 10 iterações e pacote `com.luizmarquesarruda.relogio20.diagnostic`.

O alvo é `diagnosticBenchmark`, um build não depurável derivado de `release`, assinado localmente com a chave debug. A permissão de profiling `<profileable android:shell="true" />` existe somente em `app/src/diagnosticBenchmark/AndroidManifest.xml`. O alvo inclui ProfileInstaller 1.4.1 somente nessa variante.

Compilar o módulo na CI não executa a medição. Para números úteis, conectar um dispositivo físico Android 14 ou posterior, com bateria suficiente, sem atividade intensa concorrente e com a variante correta instalada. Emuladores não são recomendados para números representativos.

Com o dispositivo conectado e Gradle 9.5.0 disponível:

```bash
gradle :macrobenchmark:connectedCheck
```

Os resultados JSON e traces Perfetto são saídas locais de diagnóstico. Não publicar traces ou dumps sem revisão de privacidade. A CI compila o módulo, mas não executa `connectedCheck` sem hardware físico dedicado.

**Limitação de interpretação:** o build type benchmark replica o release atual, que ainda não possui R8/minificação configurada. Os números iniciais servem para comparar regressões dentro da mesma configuração; não devem ser apresentados como desempenho final de produção.

## Validação

Os testes Python conferem o escopo de LeakCanary, isolamento de profileable, configuração Renovate, módulo Macrobenchmark e estrutura SHA-256 quando o metadado já está versionado. A CI compila o módulo de benchmark. A execução real do benchmark e a validação de leaks no Galaxy A06 permanecem pendentes até execução física.

## Referências oficiais

- LeakCanary: https://github.com/square/leakcanary
- Gradle Dependency Verification: https://docs.gradle.org/current/userguide/dependency_verification.html
- Renovate para Gradle: https://docs.renovatebot.com/java/
- Macrobenchmark: https://developer.android.com/topic/performance/benchmarking/macrobenchmark-overview
- AndroidX Benchmark releases: https://developer.android.com/jetpack/androidx/releases/benchmark
