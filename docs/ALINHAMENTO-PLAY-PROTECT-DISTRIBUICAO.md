# Google Play Protect e distribuição segura — Relógio 2.0

**Revisão:** 03/11/2026  
**Escopo:** instalação do APK no Android, acesso a notificações e distribuição de builds de teste.  
**Estado:** diagnóstico documental concluído; causa exata do bloqueio e validação no Galaxy A06 permanecem **não validadas**.

## 1. Achado principal na documentação oficial

A orientação do Google para desenvolvedores descreve uma proteção específica para aplicativos instalados por sideload da internet (por exemplo, download no navegador, recebimento por mensageiro ou instalação por gerenciador de arquivos). Em mercados selecionados, a instalação pode ser bloqueada quando o aplicativo declara capacidades sensíveis como:

- `RECEIVE_SMS`;
- `READ_SMS`;
- `NOTIFICATION_LISTENER`;
- `ACCESSIBILITY`.

A mensagem exemplificada pelo Google é “Este app pode pedir acesso a dados sensíveis. Isso pode aumentar o risco de roubo de identidade ou fraude financeira.” A documentação lista também o cenário distinto “App nocivo bloqueado”, que indica classificação como aplicativo potencialmente nocivo ou software indesejado.

O Google reconhece usos legítimos de acesso a notificações, incluindo apps de saúde/fitness que encaminham notificações ao respectivo dispositivo vestível e apps que exibem notificações em interfaces alternativas. Isso não é uma isenção automática: a finalidade precisa corresponder ao comportamento real, com consentimento explícito e controles adequados.

**Importante:** a proteção de sideload descrita está ativa em mercados selecionados. Não temos evidência para afirmar que essa foi a regra acionada no Galaxy A06 nem que o acesso a notificações foi a causa única. A mensagem exata exibida pelo Play Protect é necessária para classificar o bloqueio.

Fonte oficial:
- https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-BR

## 2. Auditoria do APK atual (branch `fix/integridade-android-20261103`)

Achados confirmados no código e no manifesto:

- O workflow gera `app-debug.apk` com a variante Android `debug`; é um artefato de engenharia, não release de produção.
- O manifesto declara `NotificationCaptureService`, protegido por `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE` e com `android:exported="false"`, conforme o padrão mostrado na referência Android.
- Não foram encontradas declarações de `READ_SMS`, `RECEIVE_SMS` ou serviço de acessibilidade no manifesto revisado.
- O serviço só captura após acesso especial do Android, ativação explícita e seleção dos pacotes permitidos.
- O cache é volátil, limitado a 30 prévias e 15 minutos; não há persistência das prévias nem envio à rede no código revisado.
- O encaminhamento ao GT3 Maxx ainda não está implementado. Portanto, não declarar que a função atual já cumpre o caso de uso de retransmissão para wearable descrito pelo Google.
- As dependências observadas são AndroidX Activity, Health Connect e coroutines. A aprovação de CI não substitui varredura de segurança nem avaliação do Play Protect.

A declaração protegida do serviço é uma boa prática de segurança Android, mas não remove a classificação de risco associada à capacidade de Notification Listener em uma distribuição por sideload.

Referência Android:
- https://developer.android.com/reference/android/service/notification/NotificationListenerService

## 3. Política de distribuição adotada

### 3.1 Build de diagnóstico para instalação local

Criar uma variante `diagnostic` que não declare nem empacote o serviço Notification Listener. Ela deve manter apenas as funções que não dependem desse acesso (BLE de diagnóstico, Health Connect autorizado e interface de estado).

Na variante de diagnóstico:
- o manifesto mesclado não pode conter `NotificationCaptureService` nem o intent-filter `android.service.notification.NotificationListenerService`;
- a tela de notificações deve informar que a função está indisponível nesta variante;
- não deve haver caminhos alternativos para ler notificações;
- CI deve inspecionar o manifesto final e reprovar a build caso encontre o serviço ou permissões SMS/acessibilidade.

**Estado:** recomendado; ainda não implementado.

### 3.2 Build companion com notificações

Manter a capacidade de Notification Listener isolada em uma variante `companion`. Antes de distribuir essa variante como produto:

1. Implementar e validar o encaminhamento ao wearable compatível; atualmente o protocolo de mensagens do GT3 Maxx não está validado.
2. Manter consentimento explícito antes de abrir as configurações de acesso especial.
3. Exigir ativação deliberada e seleção individual dos aplicativos.
4. Explicar quais campos são capturados, para qual dispositivo são encaminhados e por quanto tempo são mantidos.
5. Minimizar retenção; não registrar conteúdo em logs, crash reports ou telemetria.
6. Permitir pausar, revogar e limpar os dados.
7. Não solicitar SMS nem Accessibility para contornar limites do Android.

Até essas condições serem cumpridas, a variante companion e o encaminhamento de mensagens permanecem **não validados**.

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
| Manifesto sem permissões SMS | Verificado na branch revisada |
| Manifesto sem Accessibility Service | Verificado na branch revisada |
| Notification Listener protegido e não exportado | Verificado na branch revisada |
| Consentimento e filtro por pacote | Código presente |
| Cache de prévias em memória com limite/expiração | Código presente |
| Variante diagnostic sem Notification Listener | **Pendente** |
| Variante companion e encaminhamento real ao GT3 Maxx | **Pendente / protocolo não validado** |
| Release assinada e AAB | **Pendente** |
| Instalação via Play Console | **Pendente** |
| Veredito Play Protect no Galaxy A06 | **Não validado** |
| Mensagem exata do bloqueio anterior | **Não disponível** |

## Decisão final

Não desativar proteções do Android nem tentar mascarar permissões. Separar o APK de diagnóstico sem acesso a notificações da variante companion. Usar distribuição Play para testar a variante companion, após cumprir a finalidade declarada e os requisitos de privacidade. Manter o APK debug atual apenas como artefato técnico, sem prometer que ele será instalável por sideload em todos os aparelhos.
