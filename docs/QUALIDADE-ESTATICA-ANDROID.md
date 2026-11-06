# Qualidade estática e diagnóstico Android

## Estado validado

A execução Android CI #430 foi concluída com sucesso no commit `7e959270612824b2af43bf64760e5fae0eb8d91c`. KtLint, Android Lint nas variantes `diagnosticDebug` e `companionDebug`, testes unitários, builds dos dois sabores, guardrails de dependências/permissões, auditoria dos manifests mesclados, assinatura v2 e SHA-256 passaram. O artefato publicado foi somente o APK diagnóstico de depuração.

O relatório Lint da CI #430 não contém erros:
- `diagnosticDebug`: 119 avisos — 118 `SetTextI18n` e 1 `OldTargetApi`.
- `companionDebug`: 124 avisos — 123 `SetTextI18n` e 1 `OldTargetApi`.
- Os avisos `UseKtx` caíram de 11/13 para zero após migrar para `SharedPreferences.edit { }` (persistência assíncrona equivalente a `apply()`) e `String.toUri()`.

KtLint foi executado sem bloquear a CI e identificou 3.596 apontamentos de estilo: main 2.685; testes 749; companion 132; testCompanion 26; diagnostic 3; scripts Gradle 1. Os principais grupos são vírgulas finais, wrapping de expressões/listas/chains e assinaturas multilinha. Os relatórios plain, Checkstyle e SARIF foram arquivados; a etapa segue consultiva enquanto a dívida é tratada por lotes.

## Gates e ferramentas

- Android Lint é bloqueante para erros (`abortOnError = true`), com relatórios HTML/XML/SARIF. Alertas continuam como warnings durante a triagem da dívida legada. Não há baseline que esconda alertas e não há `checkOnly` restringindo as regras.
- KtLint é executado em CI com plugin Gradle `14.2.0` e engine fixa `1.8.0`. A implantação inicial é consultiva (`ignoreFailures = true` e etapa com tolerância de falha), com relatórios plain, Checkstyle e SARIF. Não executa formatação automática nem gera baseline.
- StrictMode é ativado somente em builds debug: detecta leituras/escritas de disco, rede na thread principal e vazamentos de recursos/registro. Usa apenas `penaltyLog()`; não encerra o processo. Builds release não recebem essa política.
- O `.editorconfig` estabelece UTF-8, LF, newline final, indentação de quatro espaços e limite de linha de 140 colunas para Kotlin.

## Dívida conhecida e decisões de segurança

### SetTextI18n

Os avisos `SetTextI18n` permanecem. A maior parte está em telas programáticas que combinam textos e valores diretamente. A correção adequada é extrair os textos para recursos Android com placeholders tipados, plurais e contexto de acessibilidade. Isso exige migração gradual e teste visual; não suprimir a regra nem converter tudo mecanicamente, pois isso pode quebrar pluralização, formatação numérica e conteúdo acessível.

### OldTargetApi / targetSdk

O projeto permanece em `compileSdk 36` / `targetSdk 36`. O aviso `OldTargetApi` fica visível para revisão. Desde 31/08/2026, o Google Play exige API 36 ou superior para novos apps e atualizações de apps móveis; API 36 atende o mínimo vigente. API 37 deve ser uma migração separada, após atualizar compile tools/AGP e testar as mudanças de comportamento do Android 17. Não elevar targetSdk apenas para zerar o aviso.

### Ferramentas não adicionadas

- Detekt 2.x não foi introduzido: a matriz de compatibilidade publicada para a linha alpha não coincide com a combinação atual AGP/Kotlin/Gradle. Não incorporar pré-lançamento como gate principal.
- LeakCanary não foi adicionado. Heap dumps podem conter dados de saúde, payloads BLE e conteúdo de notificações. Exige política explícita de armazenamento, acesso e descarte.
- Testes instrumentados AndroidX e testes BLE de hardware continuam pendentes. Testes unitários/emulador não comprovam aquisição física de dados do GT3 MAXX.
- Perfetto e Android Studio Profiler permanecem ferramentas de diagnóstico em sessões medidas; não são dependências de runtime nem substituem medição de bateria no aparelho.

## Próximas fases

1. Tratar os 3.596 apontamentos KtLint por grupos pequenos, começando pelos arquivos com maior densidade, sem formatter global nem baseline.
2. Migrar gradualmente os 241 avisos `SetTextI18n` restantes para `strings.xml`, com placeholders e plurais.
3. Adicionar testes instrumentados para inicialização, ciclo de vida, permissões e persistência; manter BLE real como validação em aparelho.
4. Avaliar targetSdk 37 em branch dedicada, com teste em Android 16 e Android 17.
5. Medir StrictMode, consumo de bateria e rastros Perfetto no Galaxy A06 sem habilitar logging sensível em builds distribuídas.

## Referências oficiais

- Android Lint: https://developer.android.com/studio/write/lint
- KtLint Gradle plugin: https://github.com/JLLeitschuh/ktlint-gradle
- KtLint releases: https://github.com/ktlint/ktlint/releases
- Android StrictMode: https://developer.android.com/reference/android/os/StrictMode
- Requisitos de target API no Google Play: https://developer.android.com/google/play/requirements/target-sdk
