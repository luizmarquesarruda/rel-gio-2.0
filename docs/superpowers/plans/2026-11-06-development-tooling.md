# Plano — ferramentas de diagnóstico, cadeia de dependências e desempenho

## Objetivo
Adicionar LeakCanary, Gradle Dependency Verification, Renovate e AndroidX Macrobenchmark sem alterar a lógica BLE/GATT, o armazenamento de dados de saúde ou as políticas de privacidade do aplicativo.

## Implementação
- [x] LeakCanary 2.14 somente em `diagnosticDebug`.
- [x] Guardrails para impedir que LeakCanary apareça em companion/release/benchmark.
- [x] Variante `diagnosticBenchmark` não depurável e profileable somente para shell.
- [x] Módulo `:macrobenchmark` com medição cold-start.
- [x] Renovate com atualizações agrupadas, cooldown de três dias, Dependency Dashboard e automerge desativado.
- [x] Etapa de bootstrap SHA-256 Gradle que publica metadados como artefato temporário.
- [ ] Revisar o artefato real de bootstrap e versionar `gradle/verification-metadata.xml`.
- [ ] Executar CI após todas as alterações e corrigir eventuais incompatibilidades de AGP/Kotlin.
- [ ] Executar Macrobenchmark e LeakCanary em aparelho físico; resultados físicos ainda não validados.

## Limites
- O bootstrap de checksum não comprova autenticidade no primeiro download; revisar as origens e os componentes antes de versionar.
- Configuração Renovate não instala o GitHub App nem concede permissões; precisa estar habilitado na conta/repositório.
- CI compila Macrobenchmark, mas não mede desempenho sem um dispositivo físico adequado.
- Nenhuma alteração no protocolo do GT3 MAX, em dados de saúde, nas permissões BLE ou em integrações com WearFit Pro.
- Não criar PR adicional nem realizar merge nesta etapa; as alterações são adicionadas à branch de trabalho existente da PR #20.
