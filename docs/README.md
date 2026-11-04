# Documentação do Relógio 2.0

Este índice separa decisões de arquitetura, integrações, evidências do dispositivo e governança.

## Visão geral e arquitetura
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
- [Identificação do relógio](IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil GT3 Maxx](PERFIL-GT3-MAXX.md)
- [Captura BLE de 03/11/2026](CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Perfil BLE candidato NUS/AB](PROTOCOLO-BLE-CANDIDATO.md)

## Governança e versões
- [Diário de bordo](DIARIO-DE-BORDO.md)
- [Changelog](../CHANGELOG.md)
- [Versão](../VERSION)

“Implementado” significa que há código no branch. “Validado” exige CI verde e/ou teste físico conforme o requisito.
