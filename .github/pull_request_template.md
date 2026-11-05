# Pull Request — Relógio 2.0

## Descrição

### Problema / objetivo
Descreva o problema que esta alteração resolve.

### Alterações
-
-
-

### Pacotes e source sets afetados
-

## Tipo de alteração

- [ ] Correção
- [ ] Funcionalidade
- [ ] Refatoração / reorganização estrutural
- [ ] UI Android Views
- [ ] BLE / GATT
- [ ] Health Connect / saúde
- [ ] Persistência local
- [ ] Segurança / privacidade
- [ ] Build / documentação

## Arquitetura

- [ ] Responsabilidade de cada pacote permanece clara.
- [ ] Não foram criadas camadas genéricas sem uso concreto.
- [ ] Não foram introduzidas dependências circulares.
- [ ] Imports, Manifesto e referências foram atualizados.
- [ ] Source sets `main`, `diagnostic` e `companion` foram preservados.
- [ ] Nenhuma mudança funcional foi misturada à movimentação sem justificativa.

## Persistência e integridade de dados

- [ ] Nenhum nome, formato ou caminho de arquivo persistido foi alterado sem migração.
- [ ] Estado corrompido ou schema futuro continua preservado.
- [ ] Escritas atômicas, locks, fila e limites de retenção foram preservados.
- [ ] Não foi introduzido Room/SQLite sem plano e aprovação específicos.
- [ ] Se Room/SQLite existir no escopo, tabelas, versão, migrations e schemas exportados foram comparados.
- [ ] Testes de codec, armazenamento e migração foram executados quando aplicáveis.

## Bluetooth / GATT

- [ ] Interfaces e assinaturas públicas foram preservadas.
- [ ] UUIDs e propriedades GATT não foram alterados.
- [ ] Callbacks continuam vinculados à conexão/operação atual.
- [ ] Ciclo de vida, timeout, reconexão e fechamento foram preservados.
- [ ] Permissões do AndroidManifest.xml foram revisadas por versão Android.
- [ ] NUS RX, comandos proprietários, DFU/OTA, reset e calibração continuam bloqueados.
- [ ] Leituras e notificações não foram apresentadas como sensores validados sem evidência.
- [ ] Testes BLE disponíveis foram executados.

## Variantes e privacidade

- [ ] Notification Listener continua restrito à variante `companion`.
- [ ] A variante `diagnostic` não recebeu permissão/serviço de notificação.
- [ ] Relatórios não expõem endereços Bluetooth completos.
- [ ] Logs não expõem conteúdo de notificações ou dados pessoais.
- [ ] Nenhuma credencial, keystore ou chave privada foi adicionada.

## Testes e build

- [ ] `python3 -m unittest discover -s scripts/tests -v`
- [ ] `gradle --no-daemon lintDiagnosticDebug lintCompanionDebug`
- [ ] `gradle --no-daemon testDiagnosticDebugUnitTest testCompanionDebugUnitTest`
- [ ] `gradle --no-daemon assembleDiagnosticDebug assembleCompanionDebug`
- [ ] `git diff --check`

### Resultado e ambiente

```text
JDK:
Gradle:
Android SDK:
Dispositivo/emulador:
Comandos executados:
Resultado:
Falhas conhecidas:
Teste físico: validado / não validado / não aplicável
```

## Interface — capturas

### Antes

<!-- Inserir captura ou justificar N/A -->

### Depois

<!-- Inserir captura ou justificar N/A -->

## Riscos, limitações e plano de reversão

Descreva riscos conhecidos, validações pendentes e como reverter a alteração.

-
