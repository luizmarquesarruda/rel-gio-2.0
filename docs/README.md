# Documentação do Relógio 2.0

Este índice separa decisões de arquitetura, integrações, evidências do dispositivo e governança do projeto.

## Visão geral e arquitetura
- [Arquitetura](ARQUITETURA.md) — camadas, contrato de dados e regras de integração.
- [Mapa de módulos](MAPA-DE-MODULOS.md) — responsabilidades atuais e estrutura-alvo do código.
- [Inventário de funções do Wearfit Pro](INVENTARIO-FUNCOES-WEARFIT-PRO.md) — funcionalidades de referência e prioridade.

## Integrações Android
- [Alinhamento com Google Android/Wear OS](ALINHAMENTO-GOOGLE-ANDROID-WEAROS.md) — decisões de compatibilidade, BLE, notificações, Health Connect e limites do GT3 Maxx.
- [Passos e referência de horário](INTEGRACAO-HEALTH-CONNECT-PASSOS-HORARIO.md) — Health Connect e limite da sincronização do relógio.
- [Notificações, WhatsApp e Android](INTEGRACAO-NOTIFICACOES-ANDROID-WHATSAPP.md) — NotificationListenerService, consentimento e ponte BLE pendente.
- [Pesquisa Wearfit Pro](PESQUISA-WEARFIT-PRO.md) — documentação pública, lacunas de SDK/API e referências independentes.

## Dispositivo e evidências
- [Identificação do relógio](IDENTIFICACAO-DO-RELOGIO.md)
- [Perfil GT3 Maxx](PERFIL-GT3-MAXX.md)
- [Captura BLE de 03/11/2026](CAPTURA-BLE-GT3-MAX-2026-11-03.md)
- [Perfil BLE candidato NUS/AB](PROTOCOLO-BLE-CANDIDATO.md) — comparação de UUIDs, fixture pública, parser e limites de validação.

## Governança e versões
- [Diário de bordo](DIARIO-DE-BORDO.md) — decisões, entregas, riscos e validações.
- [Changelog](../CHANGELOG.md) — alterações por versão.
- [Versão atual de desenvolvimento](../VERSION).

## Regra de leitura
“Implementado” significa que existe código no branch indicado. “Validado” exige CI verde e/ou teste físico conforme o requisito. “Não validado” não pode ser anunciado como função pronta.
