# Google Play Protect e distribuição segura — Relógio 2.0

**Revisão:** 04/11/2026  
**Escopo:** instalação do APK no Android, acesso a notificações e distribuição de builds de teste.  
**Estado:** APK diagnostic do run 41761934088 inspecionado; causa exata do bloqueio e validação no Galaxy A06 permanecem **não validadas**.

## 1. Achado principal na documentação oficial

A orientação do Google para desenvolvedores descreve uma proteção específica para aplicativos instalados por sideload da internet (por exemplo, download no navegador, recebimento por mensageiro ou instalação por gerenciador de arquivos). Em mercados selecionados, a instalação pode ser bloqueada quando o aplicativo declara capacidades sensíveis como:

- `RECEIVE_SMS`;
- `READ_SMS`;
- `NOTIFICATION_LISTENER`;
- `ACCESSIBILITY`.

A mensagem exemplificada pelo Google é “Este app pode pedir acesso a dados sensíveis. Isso pode aumentar o risco de roubo de identidade ou fraude financeira.” A documentação lista também o cenário distinto “App nocivo bloqueado”, que indica classificação como aplicativo potencialmente nocivo ou software indesejado.

O Google reconhece expressamente como uso permitido de NOTIFICATION_LISTENER apps de Saúde e Fitness que retransmitem notificações para seus próprios dispositivos vestíveis. Esse é o caso de uso pretendido para o Relógio 2.0. A categoria declarada sozinha não é uma isenção: a função deve realmente fazer a ponte com o wearable, com consentimento explícito, filtro por aplicativo e transparência. Apps que apenas capturam conteúdo sem finalidade de retransmissão não devem se apoiar nessa exceção.

**Importante:** a proteção de sideload descrita está ativa em mercados selecionados. Não temos evidência para afirmar que essa foi a regra acionada no Galaxy A06 nem que o acesso a notificações foi a causa única. A mensagem exata exibida pelo Play Protect é necessária para classificar o bloqueio.

Fonte oficial:
- https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-BR

## 2. Auditoria do APK atual (branch `fix/integridade-android-20261103`)

Achados confirmados no código e no manifesto:

- O workflow gera `app-debug.apk` com a variante Android `debug`; é um artefato de engenharia, não release de produção.
- O manifesto comum da variante `diagnostic` não declara Notification Listener. A variante `companion` declara `NotificationCaptureService`, protegido por `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` e `android:exported="false"`.
- Não foram encontradas declarações de `READ_SMS`, `RECEIVE_SMS` ou serviço de acessibilidade no manifesto revisado.
- O serviço só captura após acesso especial do Android, ativação explícita e seleção dos pacotes permitidos.
- O cache é volátil, limitado a 30 prévias e 15 minutos; não há persistência das prévias nem envio à rede no código revisado.
- A tela e a captura consentida estão implementadas, mas a retransmissão física ao GT3 Maxx ainda não está implementada, porque o protocolo de notificações não foi validado. Até essa ponte existir, o app ainda não cumpre integralmente o exemplo de uso permitido citado pelo Google.
- As dependências observadas são AndroidX Activity, Health Connect e coroutines. A aprovação de CI não substitui varredura de segurança nem avaliação do Play Protect.

A declaração protegida do serviço é uma boa prática de segurança Android, mas não remove a classificação de risco associada à capacidade de Notification Listener em uma distribuição por sideload.

Referência Android:
- https://developer.android.com/reference/android/service/notification/NotificationListenerService

## 2.1 Inspeção binária do artefato efetivamente publicado

Em 04/11/2026 foi baixado o artefato GitHub Actions `relogio-2.0-diagnostic-debug-apk` do run **41761934088**, commit `e5e4b85ac1ad969956635a860a4b655b4466b768`.

Evidências da inspeção local:

- O ZIP passou no teste de integridade (`unzip -t`) e seu SHA-256 calculado correspondeu exatamente ao digest `sha256:1e09ebbff67960e9bd6f30e0b7a7639b85fdc21e2475b4e91fc083dd349386cd` publicado pela API do GitHub Actions.
- SHA-256 do APK extraído: `21fcdb2f63abd21cdd8a6a5ce8229a1f833a27337ecbe4f3da6ffd944ac0eed1`.
- Identidade do pacote: `com.luizmarquesarruda.relogio20.diagnostic`; versão `0.4.0-alpha.1-diagnostic`; versionCode 7.
- O manifesto binário do APK contém `android:debuggable=true` e `android:allowBackup=false`.
- Não contém `android.permission.INTERNET`, SMS, Accessibility, `BIND_NOTIFICATION_LISTENER_SERVICE` nem `NotificationCaptureService`.
- As permissões funcionais identificadas foram READ_STEPS do Health Connect e Bluetooth/scan/connect, além de uma permissão interna de receiver não exportado gerada pelo AndroidX.
- O certificado extraído do bloco de assinatura informa `CN=Android Debug, O=Android, C=US`, com fingerprint SHA-256 `30:6A:4C:20:C5:AB:29:01:DA:5B:7F:B4:A7:0B:A0:ED:E8:4E:4D:6C:F2:A6:7D:EB:7B:72:31:39:D8:64:2B:8F`. É a identidade debug, não um certificado de release.
- A presença do bloco de assinatura APK v2 foi observada, mas a verificação criptográfica com `apksigner` ainda não havia sido executada no ambiente local. O novo CI passa a executar `apksigner verify --verbose --print-certs` e reprova a publicação se a verificação v2 falhar.

**Conclusão limitada:** este APK diagnostic específico não contém as capacidades sensíveis listadas pelo Google para o bloqueio automático de sideload baseado em SMS, Notification Listener ou Accessibility. Portanto, essa regra específica não explica o bloqueio caso tenha sido exatamente este arquivo. Isso **não prova ausência de malware** e não identifica outras heurísticas do Play Protect. A causa permanece desconhecida sem o texto literal da tela, o APK realmente instalado e a avaliação do Google.

A variante `companion` continua contendo Notification Listener e não deve ser instalada por sideload da internet. O serviço permanece porque faz parte da finalidade futura de ponte com o wearable; o encaminhamento físico ao GT3 Maxx ainda não foi validado.

## 3. Política de distribuição adotada

### 3.1 Build de diagnóstico para instalação local

A variante `diagnostic` foi criada nesta branch e não declara nem empacota o serviço Notification Listener. Ela mantém as funções que não dependem desse acesso (BLE de diagnóstico, Health Connect autorizado e interface de estado).

Na variante de diagnóstico:
- o manifesto mesclado não pode conter `NotificationCaptureService` nem o intent-filter `android.service.notification.NotificationListenerService`;
- a tela de notificações deve informar que a função está indisponível nesta variante;
- não deve haver caminhos alternativos para ler notificações;
- CI deve inspecionar o manifesto final e reprovar a build caso encontre o serviço ou permissões SMS/acessibilidade.

**Estado:** implementado nesta branch; CI e instalação física da variante ainda precisam ser validados.

### 3.2 Build companion com notificações

Manter a capacidade de Notification Listener isolada em uma variante `companion`. Essa permissão é parte da finalidade principal de Saúde e Fitness: retransmitir notificações ao próprio wearable. Antes de distribuir essa variante como produto:

1. Implementar e validar o encaminhamento ao wearable compatível; atualmente o protocolo de mensagens do GT3 Maxx não está validado.
2. Manter consentimento explícito antes de abrir as configurações de acesso especial.
3. Exigir ativação deliberada e seleção individual dos aplicativos.
4. Explicar quais campos são capturados, para qual dispositivo são encaminhados e por quanto tempo são mantidos.
5. Minimizar retenção; não registrar conteúdo em logs, crash reports ou telemetria.
6. Permitir pausar, revogar e limpar os dados.
7. Não solicitar SMS nem Accessibility para contornar limites do Android.

A variante companion preserva o Notification Listener porque ele é parte da função de companion Health & Fitness. O encaminhamento físico ao GT3 Maxx continua **não validado** até existir protocolo confirmado. A distribuição da variante companion deve ocorrer pelo Google Play, após implementar a ponte, revisar a política, preencher Saúde e fitness/Atividade e fitness, Segurança dos dados e política de privacidade. Não distribuir essa variante por sideload de internet.

### 3.3 Distribuição por Google Play para testes

Para testar uma variante com acesso a notificações, priorizar a distribuição pelo Google Play Console em vez de enviar um APK por link direto:

- **Teste interno:** até 100 testadores por app, com instalação pela Play Store.
- **Compartilhamento interno de apps:** permite distribuir APK/AAB por link a testadores autorizados; a documentação indica limite de até 100 downloads por link.
- Os canais internos facilitam a distribuição pelo ecossistema Play, mas não garantem aprovação de política, ausência de avisos do Play Protect ou aceitação da função. A documentação informa que testes internos podem não passar pelas revisões normais de política e segurança.

Fontes oficiais:
- https://support.google.com/googleplay/android-developer/answer/9845334?hl=pt-BR
- https://support.google.com/googleplay/android-developer/answer/9844679?hl=pt-BR

## 4. Assinatura e integridade do artefato

O APK atual é debug. O sistema de build assina automaticamente variantes debug com uma chave de depuração padrão; isso não é a identidade de assinatura adequada para distribuição de produção.

Para distribuição pelo Google Play:
- gerar um Android App Bundle (`AAB`) de release;
- assinar o upload com uma chave de upload dedicada;
- configurar Play App Signing e deixar a chave de assinatura de distribuição sob a gestão do Google Play;
- guardar keystore e senhas fora do repositório, em cofre/segredos protegidos;
- nunca commitar keystore, alias, senha ou chave privada;
- registrar SHA-256 do artefato e certificado de assinatura em cada release.

Para distribuição direta fora do Play, uma release também deve ser assinada com uma chave privada controlada pelo projeto e mantida estável entre atualizações. Isso não elimina o risco de bloqueio do Play Protect quando houver uma capacidade sensível de notificação.

Fontes oficiais:
- https://developer.android.com/build
- https://developer.android.com/studio/publish/app-signing

## 5. Portões obrigatórios no CI

Antes de publicar qualquer artefato:

- executar Android Lint e testes unitários;
- gerar e arquivar o manifesto mesclado de cada variante;
- verificar automaticamente as permissões e serviços presentes em cada variante;
- reprovar `diagnostic` se contiver Notification Listener, SMS ou Accessibility;
- reprovar qualquer variante se declarar permissões não justificadas;
- conferir assinatura e gerar SHA-256;
- publicar nome da variante, versão, commit SHA, certificado e hash junto do artefato;
- declarar explicitamente que CI verde não equivale a aprovação do Play Protect nem a teste físico.

## 6. Como classificar o aviso no Galaxy A06

Não desativar o Play Protect para instalar o APK. Registrar o texto exato da tela:

| Mensagem observada | Interpretação inicial |
|---|---|
| “Este app pode pedir acesso a dados sensíveis…” | Investigar sideload e capacidade Notification Listener/SMS/Accessibility. |
| “App nocivo bloqueado” | Classificação PHA/malware/software indesejado; revisar código, dependências, empacotamento e solicitar análise se houver falso positivo. |
| “A verificação do app é recomendada” | App desconhecido ainda não analisado; permitir a verificação do próprio Google é o caminho normal. |
| “Não foi possível instalar” sem aviso do Play Protect | Investigar assinatura, conflito de certificado/pacote, versão, integridade do APK e compatibilidade Android. |

Se, após auditoria e correções, a classificação continuar incorreta, seguir o processo oficial de contestação do Play Protect:
- https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-BR

## 7. Estado de validação

| Item | Estado |
|---|---|
| Manifesto sem permissões SMS | Verificado no APK binário do run 41761934088 |
| Manifesto sem Accessibility Service | Verificado no APK binário do run 41761934088 |
| Notification Listener protegido e não exportado | Verificado na branch revisada |
| Consentimento e filtro por pacote | Código presente |
| Cache de prévias em memória com limite/expiração | Código presente |
| Variante diagnostic sem Notification Listener | **Confirmada no APK binário do run 41761934088; CI reforçado em novo commit; instalação física pendente** |
| Finalidade Health & Fitness e documentação | **Registrada; falta Play Console** |
| Variante companion com Notification Listener | **Preservada; CI/validação física pendentes** |
| Encaminhamento real ao GT3 Maxx | **Pendente / protocolo não validado** |
| Release assinada e AAB | **Pendente; APK atual usa certificado Android Debug** |
| Instalação via Play Console | **Pendente** |
| Veredito Play Protect no Galaxy A06 | **Não validado** |
| Mensagem exata do bloqueio anterior | **Resumo fornecido pelo Gemini; texto literal ainda não disponível** |

## Decisão final

Não desativar proteções do Android nem tentar mascarar permissões. Manter a variante diagnostic sem acesso a notificações para testes BLE/Health Connect locais. Manter a variante companion com Notification Listener porque o produto é um app Health & Fitness cuja finalidade é retransmitir notificações para o wearable. Antes de alegar que o uso está integralmente alinhado à exceção do Google, implementar e validar a retransmissão ao GT3 Maxx. Distribuir companion pelo Google Play, com declarações de Saúde e fitness/Atividade e fitness, Segurança dos dados e política de privacidade. A categoria não garante remoção de avisos do Play Protect.


## 8. Auditoria de obsolescência — execução #193 (04/11/2026)

O relatório acima descreve o artefato histórico run 41761934088/versão alpha.1 e não deve ser confundido com o pacote atual.

- A execução #193 da PR #9 gerou o pacote relogio-2.0-diagnostic-debug-apk, versão 0.4.0-alpha.3-diagnostic.
- O pacote passou Lint, testes, builds dos dois sabores, guardrails, auditoria de manifestos e verificação de assinatura APK v2.
- O BUILD-INFO foi ajustado para registrar dois identificadores: SHA fonte da branch/head da PR e SHA de merge/teste usado pelo evento pull_request.
- O APK permanece debug; a verificação de CI não confirma instalação física, aceitação pelo Play Protect nem funcionamento no Galaxy A06.
