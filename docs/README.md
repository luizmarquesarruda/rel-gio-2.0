# Documentação do Relógio 2.0

Este índice separa decisões de arquitetura, integrações, evidências do dispositivo e governança.

## Visão geral e arquitetura
- [Referências técnicas Android BLE e Bluetooth SIG](REFERENCIAS-TECNICAS-ANDROID-BLE-2026-11-04.md) — fontes oficiais usadas para os ajustes dos callbacks GATT, armazenamento e decoders.
- [Auditoria geral do repositório — 04/11/2026](AUDITORIA-REPOSITORIO-2026-11-04.md) — escopo, defeitos corrigidos e validações pendentes.
- [Arquitetura](ARQUITETURA.md) — camadas, contratos e regras de integração.
- [Mapa de módulos](MAPA-DE-MODULOS.md) — estado real, estrutura-alvo e dívida técnica.
- [Navegação e layout](NAVEGACAO-E-LAYOUT.md) — áreas, telas secundárias, estados e limites visuais.
- [Inventário de funções Wearfit Pro](INVENTARIO-FUNCOES-WEARFIT-PRO.md) — catálogo resumido.
- [Matriz de compatibilidade GT3 MAX](MATRIZ-COMPATIBILIDADE-GT3-MAXX.md) — 33 entradas, módulos responsáveis, riscos e validação.
- [Contrato de métricas de saúde](CONTRATO-METRICAS-SAUDE.md) — tipos, unidades, origem, timestamps e portão de validação.
- [Plano de substituição progressiva Wearfit Pro](PLANO-SUBSTITUICAO-WEARFIT.md) — fases, evidências e critérios.

## Integrações Android
- [Alinhamento Google Android/Wear OS](ALINHAMENTO-GOOGLE-ANDROID-WEAROS.md)
- [Play Protect e distribuição segura](ALINHAMENTO-PLAY-PROTECT-DISTRIBUICAO.md)
- [Investigação do bloqueio B do Play Protect](INVESTIGACAO-BLOQUEIO-PLAY-PROTECT-2026-11-04.md) — classificação recebida, evidências por variante e próximos passos.
- [Arquitetura Saúde & Fitness](SAUDE-FITNESS-ARQUITETURA.md)
- [Passos e referência de horário](INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md)
- [Plano de mapas, rotas e sinais vitais](PLANO-MAPAS-E-ROTAS-HEALTH-CONNECT.md)
- [Notificações, WhatsApp e Android](INTEGRACAO-NOTIFICACOES-ANDROID-WHATSAPP.md)
- [Pesquisa Wearfit Pro](PESQUISA-WEARFIT-PRO.md)

## Dispositivo e evidências
- [Referências técnicas Android BLE e Bluetooth SIG](REFERENCIAS-TECNICAS-ANDROID-BLE-2026-11-04.md)
- [Identificação do relógio](IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil GT3 Maxx](PERFIL-GT3-MAXX.md)
- [Estudo técnico do firmware GT3 MAX Call](ESTUDO-FIRMWARE-GT3-MAXX.md) — evidências GATT, matriz de recursos, assinatura estrutural e limitações de análise estática.
- [Perfil de acesso GT3 MAX Call](PERFIL-ACESSO-GT3-MAX-CALL.md) — identificação de software informada, fingerprint GATT e escopo de leitura segura.
- [Integração de parceiro Wearfit Pro](INTEGRACAO-WEARFIT-PARTNER-SDK.md) — autorização de terceiros, limites da API e elegibilidade do GT3 MAX Call.
- [Análise de firmware OTA do Wearfit Pro](ANALISE-FIRMWARE-OTA-WEARFIT-PRO.md) — identidade regulatória, serviço Nordic Secure DFU e procedimento seguro para obter o pacote.
- [Captura BLE de 03/11/2026](CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Automação de testes GATT e coleta local](AUTOMACAO-TESTES-GATT-GT3MAX.md) — consentimento, leituras, notificações, decoders e limites.
- [Perfil BLE candidato NUS/AB](PROTOCOLO-BLE-CANDIDATO.md)

## Governança e versões
- [Diário de bordo](DIARIO-DE-BORDO.md)
- [Changelog](../CHANGELOG.md)
- [Versão](../VERSION)

“Implementado” significa que há código no branch. “Validado” exige CI verde e/ou teste físico conforme o requisito.
