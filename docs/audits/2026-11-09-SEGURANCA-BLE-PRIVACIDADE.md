# Auditoria técnica — segurança BLE, privacidade e persistência

**Data:** 09/11/2026  
**Base:** PR #21, commit abe8b3bac19325da4cdac50e00c81087c26f5b22  
**Correções:** PR #22, branch fix/security-privacy-gatt-20261109

## Achados confirmados

| Prioridade | Achado | Correção nesta branch |
|---|---|---|
| P0 | CTS 0x1805/0x2A2B permitia escrita em característica após confirmação do usuário. Isso contrariava a política somente leitura. | Removidos botão, método de escrita e callback. AccessPolicy falha fechado para qualquer escrita em característica. |
| P0 | TXT de diagnóstico incluía nome de dispositivo, MAC parcialmente mascarado, logs livres, valores decodificados e HEX bruto. | Relatório omite identificadores e conteúdo livre; registra apenas informações gerais e contagens. |
| P1 | Flush de até 10 s podia ser aguardado na Activity, inclusive em callbacks e onDestroy. | Exportação faz flush em Dispatchers.IO; transições de sessão e marcadores usam fila serial; sem espera no onDestroy ou no callback de interrupção. |
| P1 | Retorno esperado “sem sessão ativa” era contabilizado como falha de persistência. | Resultado nulo representa skip; somente falha real incrementa failedWrites. Amostras fora da captura não entram na fila. |
| P1 | Desconexão espontânea podia cancelar o watchdog de descoberta sem registrar a interrupção. | Callback registra a interrupção antes de cancelar o timeout, com persistência assíncrona. |

## Contrato BLE preservado

- Nenhuma escrita em característica GATT, inclusive CTS, NUS RX, DFU/OTA ou canais proprietários.
- Escrita em descritor somente para CCCD padrão 0x2902 e apenas para NOTIFY/INDICATE.
- Leituras GATT e notificações/indicações continuam permitidas segundo o plano de exploração.
- Sem SQLite, sem alteração de schema e sem mudança de origem Health Connect.

## CI e validação

- A execução anterior #468 falhou no gate explícito de revisão/commit da KtLint baseline; o bootstrap Gradle havia concluído lint, testes e assemblies. Não classificar essa falha de política como falha de compilação.
- A execução TDD #469 reproduziu quatro falhas nos testes de contrato antes da implementação. O passo KtLint também executou com Gradle 9.8.2 porque o setup-gradle foi ignorado após a falha antecipada dos testes; a condição do workflow deve impedir esse falso diagnóstico.
- A próxima CI deve executar testes Python, KtLint, Android Lint, testes Kotlin e os dois APKs. APK publicável depende dos gates já existentes.
- Teste físico no Galaxy A06 com GT3 MAX, precisão de sensores, sincronização do RTC e comportamento em segundo plano: **não validado**.

## Pendência de produto

A solicitação anterior definiu um Painel principal e seis telas secundárias. A PR #21 atualmente contém sete áreas secundárias. Isso não foi alterado nesta correção de segurança; precisa ser resolvido em uma etapa de UX separada antes da consolidação do layout.

## Estado

Correções implementadas nesta branch. Nenhum merge ou release realizado. A validação de CI pós-correção ainda está pendente.
