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

- MainActivity.kt ainda concentra apresentação, permissões BLE, conexão/ciclo GATT e leitura agregada do Health Connect.
- domain/health/HealthMetricSample.kt foi criado como modelo Kotlin puro para tipo, unidade, origem, timestamps e estado de validação. Não recebe bytes BLE nem grava dados.
- O contrato exige unidade compatível, valores finitos/não negativos, timestamp de recebimento e origem explícita; passos são inteiros e SpO₂ fica no intervalo de 0 a 100.
- METT tem unidade MET e não é convertido automaticamente em calorias. Pressão arterial/SpO₂ não são tratados como medições clínicas.
- SerialGattOperationQueue foi isolada em device/ble/gatt.
- WearfitProtocolProfile, WearfitAbFrame e WearfitAbFrameParser foram isolados em device/gt3max/protocol. O nome do pacote não confirma chipset nem identidade do dispositivo.
- Os testes acompanham os pacotes correspondentes; os testes do contrato Domain verificam unidades, limites e preservação de origem.
- Notification Listener permanece na variante companion; diagnostic não deve declarar essa capacidade.
- Health Connect ainda é chamado diretamente por MainActivity; extração para data/healthconnect não foi feita.
- Não foram adicionados serviços, permissões ou escritas BLE proprietárias.

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

1. Isolar utilitários puros e testes sem mudar comportamento (concluído parcialmente).
2. Extrair scan/máquina de estados GATT de MainActivity para device/ble/core, preservando fila e callbacks antigos.
3. Extrair Health Connect para data/healthconnect.
4. Criar persistência local versionada e repositório para o modelo Domain.
5. Implementar parsers somente após evidência repetível por métrica.
6. Extrair adaptadores Platform quando os recursos Android forem priorizados.

Cada extração deve ser pequena, testada e reversível. Não misturar refatoração com engenharia reversa de protocolo.
