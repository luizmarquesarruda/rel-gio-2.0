# Plano de auditoria e endurecimento do repositório — 04/11/2026

**Objetivo:** revisar o repositório Relógio 2.0, corrigir falhas de integridade e concorrência identificadas e manter o comportamento seguro de BLE/GATT.

## Restrições

- Não disparar, repetir nem cancelar CI.
- Não fazer merge ou abrir PR durante a auditoria.
- Não permitir escrita proprietária, NUS RX, CTS, OTA, reset ou calibração.
- Não alterar permissões/dependências de rede sem justificativa e aprovação.
- Não declarar validação física sem o Galaxy A06/GT3 MAX.

## Execução

- [x] Inventariar branches/PRs e preservar a cadeia atual.
- [x] Revisar workflow, toolchain, manifests, documentação e distribuição de artefatos.
- [x] Auditar persistência do inventário GATT e corrigir aceitação permissiva de JSON incompleto.
- [x] Serializar o inventário entre instâncias e verificar resultado de gravação.
- [x] Auditar biblioteca de descobertas e ampliar proteção concorrente/validação de registros.
- [x] Endurecer invariantes de sessão e timestamps.
- [x] Alinhar interface e política de scan ao handoff de Wearfit Pro.
- [x] Reforçar consistência temporal de snapshots Health Connect.
- [x] Adicionar testes de regressão.
- [x] Atualizar documentação, changelog e diário.

## Verificação pendente por decisão/ambiente

- [ ] Executar testes Kotlin e Android Lint.
- [ ] Construir variantes diagnostic e companion.
- [ ] Auditar APKs compilados, assinatura e permissões mescladas.
- [ ] Testar handoff, reconexão e sensores no Galaxy A06/GT3 MAX.
- [ ] Introduzir Gradle Wrapper oficial e validar seu uso.
- [ ] Extrair ciclo BLE de MainActivity em fases pequenas, após testes de regressão.

A CI foi deliberadamente deixada intocada por solicitação expressa do usuário. Não interpretar os itens de verificação pendentes como aprovados.
