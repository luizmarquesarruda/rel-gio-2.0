# Matriz de compatibilidade — GT3 MAX / Wearfit Pro

**Projeto:** Relógio 2.0  
**PR:** #5 — aberta, draft e sem merge  
**Revisão:** 04/11/2026  
**Origem do catálogo:** opções e trechos de ajuda do Wearfit Pro fornecidos pelo usuário.

## Leitura correta

A lista contém **33 entradas de menu/recurso** do catálogo Wearfit Pro. Isso não comprova que todas estejam presentes ou habilitadas no GT3 MAX específico. O catálogo é referência funcional, não especificação técnica do firmware.

- **Não validado:** falta evidência física/protocolo suficiente para afirmar suporte do GT3 MAX.
- **Não implementado:** não existe implementação correspondente no Relógio 2.0.
- **Captura local implementada:** existe apenas a função Android local descrita; não implica comunicação completa com o relógio.
- **Bloqueado:** não implementar nesta fase por risco, dependência externa ou falta de especificação.

## Catálogo

| ID | Rótulo de origem | Módulo responsável | Dependência / limite | Prioridade | Estado |
|---|---|---|---|---|---|
| DEV-01 | Device split screen | ui/device | Rótulo ambíguo; não confundir com multi-janela Android. | P3 | Não validado |
| CALL-01 | Call function | platform/calls (Bluetooth Classic/HFP) | Canal Classic separado do BLE de sensores. | P1 | Não validado |
| MEDIA-01 | Music control | platform/media + adapter do relógio | MediaSession Android pode atuar localmente; controle pelo relógio exige protocolo. | P2 | Não validado |
| CAM-01 | Remote control camera | platform/camera | Requer fluxo Android suportado; sem Accessibility. | P3 | Não validado |
| PAY-01 | Alipay | Integração de pagamento | Serviço/certificação externos; fora do escopo inicial. | Bloqueado | Não implementar |
| ALARM-01 | Use alarm clock | platform/alarms + adapter do relógio | Alarme local Android abre o app Relógio para confirmação; sincronização com firmware não implementada. | P2 | Android local implementado; relógio não validado |
| NFC-01 | Analog access control card | platform/nfc / segurança | NFC, elemento seguro e credenciais não confirmados. | Bloqueado | Não implementar |
| UTIL-01 | Stopwatch | domain/utilities + UI | Cronômetro local com iniciar/pausar/zerar; estado não persiste após fechar a tela. | P2 | Android local implementado |
| UTIL-02 | Timer | domain/utilities + platform/alarms | Temporizador local de 1–180 min enquanto a tela permanece aberta; não sincroniza com o relógio. | P2 | Android local implementado |
| FIND-01 | Find phone | platform/notifications + audio + adapter | Direção da ação precisa ser confirmada; BLE ainda não identificado. | P2 | Não validado |
| CONTACT-01 | Bind business card | domain/profile / storage local | Semântica e dados envolvidos incertos; não pedir contatos por padrão. | P3 | Não validado |
| NOTIF-01 | Message notification | platform/notifications + device transport | Captura local consentida existe na companion; retransmissão física pendente. | P1 | Captura local implementada; envio não validado |
| HEALTH-REM-01 | Health reminder | domain/health + platform/notifications | Notificação diária local via AlarmManager inexact; exige POST_NOTIFICATIONS e reativação após reboot. | P2 | Android local implementado |
| QUICK-01 | Quick collection | A definir | Rótulo ambíguo; não inferir pagamento, coleta de dados ou outra ação. | P3 | Sem definição |
| MOTOR-01 | Motor function | device/gt3max/protocol + haptics | Pode significar vibração; comandos/padrões não documentados. | P3 | Não validado |
| GOAL-01 | Target settings | domain/activity + UI | Meta diária local de passos; progresso usa agregado Health Connect filtrado por origem APP_ONLY. | P1 | Android local implementado |
| AI-01 | ChatGPT usage guidance | ui/help / integração externa opcional | Orientação não prova integração oficial com OpenAI nem compatibilidade do GT3 MAX. | P3 | Documentação; integração não validada |
| ACT-01 | Automatic motion recognition | domain/activity + ingestão de sensores | Exige série temporal identificada e algoritmo testado. | P1 | Não validado |
| SPO2-01 | Measurement of blood oxygen | domain/health + parser validado | Validar origem, unidade, timestamp e repetibilidade; não diagnóstico. | P2 | Não validado |
| BP-01 | Blood pressure measurement | domain/health + parser validado | Fabricante limita uso a referência; não substitui aparelho validado. | P2 | Não validado clinicamente/BLE |
| SLEEP-01 | Sleep monitoring | domain/health + parser validado | Validar início/fim, fuso e fases/estimativas. | P1 | Não validado |
| STRESS-01 | Pressure monitoring | domain/wellness + parser | Provável estresse; confirmar rótulo e escala antes de modelar. | P2 | Não validado |
| METT-01 | Mett monitoring / Test METT | domain/activity + health | METT informado como equivalente metabólico; preservar período/escala e não converter direto em calorias. | P2 | Sem parser validado |
| BREATH-01 | Breathing training | domain/wellness + UI/timer | Guia local 4–2–6 no telefone; não mede respiração nem controla o relógio. | P2 | Android local implementado |
| CYCLE-01 | Physiological cycle | domain/health-sensitive + storage | Opcional, consentimento separado e proteção reforçada; sem sincronização automática. | P3 | Não implementado |
| BRIGHT-01 | Brightness adjustment | ui/settings + device adapter | Brilho Android não equivale ao brilho do relógio; comando não validado. | P3 | Não validado |
| BUTTON-01 | Down key customization | device/gt3max/protocol | Documentação diz que alguns dispositivos suportam; modelo não confirmado. | P3 | Não validado |
| LOCK-01 | Lock screen or unlock | device/gt3max/protocol | Ação reversível, com confirmação; sem comando desconhecido. | P3 | Não validado |
| OTA-01 | OTA upgrade equipment | device/firmware | Exige firmware assinado, autenticação, integridade e recuperação. | Bloqueado | OTA/DFU fora da fase |
| TIME-01 | Set language and time | platform/clock + device adapter | Idioma e horário são operações diferentes; escrita BLE não validada. | P1 | Hora do celular exibida; relógio não validado |
| POWER-01 | Switch on/off, restart and restore factory settings | device administration | Desligar/reiniciar/restaurar são ações diferentes; factory reset apaga dados/configurações. | Bloqueado | Bloqueado por segurança |
| AUDIO-01 | Audio Bluetooth settings (I) | platform/bluetooth-classic/audio | Significado desconhecido; não assumir HFP/A2DP. | P2 | Não validado |
| AUDIO-02 | Audio Bluetooth settings (II) | platform/bluetooth-classic/audio | Canal não documentado; identificar por testes de perfis. | P2 | Não validado |

## Implementações Android locais — 05/11/2026

Os recursos abaixo passaram a ter fluxo funcional no aplicativo, sem depender do firmware:

- **UTIL-01:** cronômetro local com iniciar, pausar e zerar. O tempo não é persistido após fechar a tela.
- **UTIL-02:** temporizador local de 1 a 180 minutos. A contagem depende da tela/Activity permanecer ativa; não é alarme em segundo plano.
- **ALARM-01:** encaminha a criação do alarme ao aplicativo Relógio do Android, com confirmação do usuário. Não envia configuração ao GT3 MAX.
- **GOAL-01:** meta diária de passos configurável (100–100.000), com progresso baseado no agregado Health Connect quando a permissão existe. A política APP_ONLY pode retornar ausência enquanto o app não for produtor dos registros; ausência não é exibida como zero.
- **BREATH-01:** guia respiratório local 4–2–6; não mede sinais fisiológicos e não substitui orientação clínica.
- **HEALTH-REM-01:** lembrete diário local por notificação inexacta. Requer permissão de notificações e deve ser reativado após reinicialização do telefone.
- **Histórico de passos:** consulta de 7 dias por agregação diária Health Connect, com filtro de origem e dias sem bucket representados como “Sem registro”. A tela permite incluir origens externas somente após escolha explícita.

Essas implementações são **Android local**, não integração física com o relógio. Chamadas, áudio, música, câmera, localizar telefone, vibração remota, sincronização de hora, métricas biométricas e recursos proprietários continuam dependentes de evidência de protocolo. OTA/DFU, reset, pagamentos e cartões de acesso permanecem bloqueados.

## Fronteiras entre módulos

1. **BLE transport não é protocolo.** Scan, conexão GATT, callbacks, fila serial e ciclo de vida ficam em `device/ble`. Framing, UUIDs candidatos e parsing específico ficam em `device/gt3max/protocol`. O transporte não interpreta métricas.
2. **Bluetooth Classic não é BLE.** Chamadas e áudio precisam ser investigados por perfis Classic (por exemplo, HFP/A2DP) e não podem reutilizar o parser NUS/AB.
3. **Saúde exige proveniência.** Cada futura amostra deve preservar tipo, unidade, origem, timestamp do relógio quando houver, timestamp de recebimento, sessão/dispositivo local e qualidade. Agregados do Health Connect não podem ser atribuídos ao GT3 MAX.
4. **Estimativas não são diagnósticos.** Pressão arterial, SpO₂, estresse e METT devem preservar a natureza estimada e suas limitações. METT não deve ser convertido automaticamente em calorias.
5. **Health Connect é adaptador separado.** Só publicar um tipo após confirmar semântica, unidade, horário, origem e autorização específica. A permissão de passos não autoriza outras métricas.
6. **Android local primeiro.** Cronômetro, temporizador, metas e treino respiratório podem ser implementados localmente sem fingir que o relógio os suporta.
7. **Privacidade por padrão.** Não coletar contatos, ciclo fisiológico, identificadores permanentes ou notificações sem função concreta, minimização, consentimento e retenção definida.

## Ordem de implementação

- **A — Base:** separar BLE transport, protocolo candidato, domínio de métricas/proveniência, armazenamento local e Health Connect.
- **B — Android sem protocolo proprietário:** metas locais, cronômetro, temporizador, treino respiratório e lembretes locais.
- **C — Saúde somente leitura:** correlacionar cada métrica entre tela do relógio, Wearfit Pro e captura BLE em várias sessões; começar pelas métricas prioritárias.
- **D — Controles de dispositivo:** mídia, chamadas, alarme, encontrar telefone, brilho, idioma e horário somente após identificar canais/perfis e comandos.
- **Fora da fase:** Alipay, cartões de acesso, OTA/DFU, restauração de fábrica e comandos destrutivos.

## Evidência exigida antes de habilitar uma função

Para cada métrica/comando: duas ou mais sessões independentes; comparação simultânea com relógio e Wearfit Pro; variação controlada; registro de timestamp e payload; semântica/unidade confirmadas; teste de reconexão e falhas. Para ações de escrita, acrescentar especificação, confirmação do usuário, reversibilidade e plano de recuperação.

**Estado geral:** as 33 funções do catálogo ainda não foram validadas fisicamente no Galaxy A06/GT3 MAX. A captura BLE disponível continua sendo perfil NUS/AB candidato; não é evidência de protocolo de saúde ou controle.

## Segurança

- Não publicar MAC, SN ou outros identificadores individuais do relógio.
- Não enviar comandos pela RX NUS nem escrever características proprietárias desconhecidas.
- Não executar OTA/DFU, reset ou factory restore.
- Não solicitar permissões de contatos, chamadas, SMS, localização ou saúde sem recurso funcional e consentimento específico.
- Nenhuma alteração desta matriz habilita transmissão de dados ou comandos ao relógio.
