# Auditoria de módulos e correções — 06/11/2026

## Escopo

Auditoria da PR #21, branch `feat/ui-8-screen-layout-20261106`, abrangendo `:app`, `:macrobenchmark`, flavors `diagnostic` e `companion`, UI, BLE/GATT, perfil GT3 MAX, descoberta, persistência, Health Connect, clima, notificações, manifesto, scripts, CI e documentação.

## Correções aplicadas nesta branch

1. **Consentimento CTS:** a conexão GATT não dispara mais a escrita do Current Time automaticamente. A ação agora exige toque do usuário e confirmação em diálogo. A política continua limitada ao CTS padrão `0x1805/0x2A2B`, com fingerprint completo e propriedade WRITE anunciada.
2. **Insets / edge-to-edge:** `RelogioUi.applyEdgeToEdge` centraliza `WindowCompat`, `WindowInsetsCompat` e insets de barras/recorte. A função foi conectada à tela BLE e às telas manuais de clima, ferramentas e notificações; as telas que usam `RelogioUi.install` recebem o mesmo tratamento.
3. **Verificação Gradle / Android Lint:** o bootstrap `--write-verification-metadata` agora inclui `lintDiagnosticDebug` e `lintCompanionDebug`, antes da etapa de Lint bloqueante.
4. **Permissões diagnostic:** `app/src/diagnostic/AndroidManifest.xml` remove, na mesclagem, `READ_EXTERNAL_STORAGE` e `WRITE_EXTERNAL_STORAGE` herdadas do LeakCanary debug-only. O guardrail permanece ativo.
5. **Health Connect:** `AppPreferences` é a fonte única para incluir origens externas. A primeira leitura migra o valor salvo anteriormente em `local_feature_tools/include_external_health_origins`.
6. **Writer da biblioteca BLE:** a persistência assíncrona contabiliza tarefas aceitas, falhas, rejeições e pendências. O resumo é incluído no relatório TXT de diagnóstico.
7. **Lembrete diário:** o agendamento local usa o mesmo PendingIntent e `setInexactRepeating`; o receiver restaura a agenda em `BOOT_COMPLETED` e `MY_PACKAGE_REPLACED`. Não foi solicitada permissão de alarmes exatos.
8. **Navegação:** Dashboard passa a apresentar seis áreas secundárias. Configurações está acessível dentro de Mais recursos.
9. **Documentação / KtLint:** a estrutura de módulos, flavors, pacotes e limites conhecidos foi atualizada. KtLint passou a falhar por padrão e usa baseline em `app/config/ktlint/baseline.xml`; a geração inicial é publicada pela CI para revisão antes de ser versionada.

## Limites preservados

- Não foi introduzido Compose, Hilt ou Room.
- Não foram adicionadas escritas NUS RX, comandos proprietários, DFU, OTA ou reset.
- A captura de notificações continua local e exclusiva da variante companion.
- Métricas GT3 MAX permanecem candidatas até validação semântica e física.
- A navegação mantém seis áreas secundárias, mas atividades de apoio continuam existindo como rotas subordinadas.
- `MainActivity` continua concentrando o ciclo BLE; extração do orquestrador BLE e contrato de domínio para Health Connect são refatorações posteriores que exigem testes de regressão.

## Validação

- CI anterior #462: testes unitários e compilação dos APKs debug/macrobenchmark passaram; Android Lint e o guardrail de permissões falharam. Essa execução é evidência histórica, não valida as correções desta revisão.
- A CI desta revisão foi acionada pelos commits da branch; o resultado final deve ser conferido no SHA mais recente.
- A primeira execução sem baseline gera e publica `ktlint-baseline-review`, mas falha deliberadamente e bloqueia a publicação do APK. Após revisar e commitar `app/config/ktlint/baseline.xml`, a CI executa KtLint como gate bloqueante para novos desvios.
- Galaxy A06 / Android 16 / GT3 MAX: **não validado fisicamente**.
- Macrobenchmark no dispositivo: **não executado**.
- Não houve merge da PR #21.
