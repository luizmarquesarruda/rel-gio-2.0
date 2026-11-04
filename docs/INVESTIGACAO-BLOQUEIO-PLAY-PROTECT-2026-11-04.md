# Investigação do bloqueio B do Play Protect — 04/11/2026

## Evidência recebida

O usuário identificou a mensagem como a categoria B: “O app foi bloqueado para proteger seu dispositivo”. A descrição veio de uma síntese do Gemini, não de transcrição literal nem captura de tela. Portanto, a classificação é útil, mas o texto completo e a origem exata do APK permanecem não confirmados.

## Regra oficial relevante

A orientação do Google descreve bloqueio preventivo de instalação por sideload da internet quando o APK declara capacidades sensíveis, incluindo RECEIVE_SMS, READ_SMS, NOTIFICATION_LISTENER e ACCESSIBILITY. O Google lista como uso permitido do Notification Listener os apps Saúde e Fitness que retransmitem notificações para seus próprios wearables, desde que respeitem consentimento e finalidade. Isso não é uma isenção automática nem substitui a análise do Google.

Fonte oficial: https://developers.google.com/android/play-protect/warning-dev-guidance

## Evidência do repositório

- O manifesto comum declara BLE, READ_STEPS do Health Connect e consultas limitadas; não declara INTERNET, SMS ou Accessibility.
- A variante diagnostic não deve declarar NotificationListenerService.
- A variante companion declara NotificationCaptureService protegido por BIND_NOTIFICATION_LISTENER_SERVICE e exported=false.
- A ponte física de notificações para o GT3 Maxx ainda não foi implementada porque o protocolo não foi validado.
- O único pacote publicado pelo workflow é diagnostic. Companion é compilada e auditada, mas não deve ser distribuída como APK de sideload.
- O APK diagnostic do run 41761934088 foi inspecionado: pacote com sufixo .diagnostic, sem INTERNET, SMS, Accessibility, Notification Listener ou NotificationCaptureService. Seu certificado é Android Debug, não certificado de release.

## Diagnóstico atual

Se o arquivo instalado foi a variante companion ou um APK anterior que ainda continha Notification Listener, o bloqueio B é compatível com a regra de sideload descrita pelo Google.

Se o arquivo instalado foi exatamente o diagnostic do run 41761934088, a regra específica baseada em Notification Listener/SMS/Accessibility não explica o bloqueio. Isso não prova que o APK é seguro nem exclui outra classificação do Play Protect. A causa não pode ser determinada sem o APK exato e a mensagem literal.

## CI e correção do auditor

Run 41764184171: https://github.com/luizmarquesarruda/rel-gio-2.0/actions/runs/41764184171

- Testes da política de manifesto: sucesso.
- Android Lint nas duas variantes: sucesso.
- Testes unitários e montagem das duas variantes: sucesso.
- Guardrails de dependências e permissões: sucesso.
- Auditoria final falhou porque `apkanalyzer manifest print` emitiu o rótulo `Saúde & Fitness` com ampersand cru, produzindo XML inválido para o parser Python. A falha era na serialização do relatório da ferramenta, não evidência de manifesto Android inválido nem de malware.
- O pacote de instalação não foi publicado; somente `android-validation-reports` foi publicado.
- Corrigido o auditor para escapar ampersands não codificados antes de analisar XML e adicionados testes para ampersand cru e entidade `&amp;` já válida. Matcher de entidades numéricas também foi conferido.
- Nova execução após a correção: run 41764797014, inicialmente na fila: https://github.com/luizmarquesarruda/rel-gio-2.0/actions/runs/41764797014
- Artefato diagnostic novo: ainda não publicado; aguardar a nova execução.

## Próximas ações seguras

1. Aguardar auditoria de manifesto/assinatura e publicação do novo pacote diagnostic.
2. Usar apenas o artefato diagnostic identificado por commit, pacote e SHA-256; não instalar APK companion de sideload.
3. Não desativar o Play Protect nem remover capacidades às cegas.
4. Quando houver acesso ao aparelho, conferir o nome do arquivo baixado e a tela literal do bloqueio.
5. Se o diagnostic auditado continuar bloqueado, coletar o APK/hash e seguir o processo oficial de contestação do Play Protect após revisar o binário.

## Limites

CI verde comprova somente as verificações automatizadas executadas. Não comprova ausência absoluta de malware, aprovação do Play Protect, segurança em execução ou funcionamento físico no Galaxy A06/GT3 Maxx. Instalação e classificação Play Protect permanecem **não validadas**.
