# Relógio 2.0 — arquitetura Saúde & Fitness

**Revisão:** 04/11/2026  
**Produto:** companion Android para smartwatch  
**Categoria planejada no Google Play:** Saúde e fitness  
**Recurso de saúde a declarar:** Atividade e fitness

## 1. Definição do produto

O Relógio 2.0 é um aplicativo de Saúde e Fitness que acompanha um smartwatch e conecta dados de atividade do usuário ao ecossistema Android.

O relógio é a fonte das medições feitas por seus próprios sensores. O app companion recebe esses dados por integração suportada (Wearfit Pro, SDK documentado ou BLE validado), identifica origem e horário, valida os valores e os compartilha com Health Connect somente depois de autorização explícita.

O app também oferece uma ponte de notificações do telefone ao relógio: o Android publica notificações, o app lê notificações dos aplicativos escolhidos pelo usuário e a camada de transporte as encaminha ao wearable. A leitura não altera o aplicativo de origem: o Relógio 2.0 não responde, apaga, arquiva nem manipula a notificação.

## 2. Fluxos de dados independentes

### A. Saúde: wearable → Android → Health Connect

```text
Sensores do smartwatch
        ↓
Wearfit Pro / SDK oficial / BLE validado
        ↓
Adaptador do dispositivo
        ↓
Validação de tipo, unidade, timestamp, intervalo e origem
        ↓
Consentimento Health Connect por tipo de dado
        ↓
Health Connect (armazenamento centrado no dispositivo)
        ↓
Outros apps de saúde compatíveis, conforme autorização
```

Regras:
- Cada amostra carrega tipo, valor, unidade, timestamp do dispositivo (quando disponível), timestamp de recebimento, identificador local da fonte e qualidade.
- O app não atribui ao relógio um total agregado que também contém dados do telefone ou de outros apps.
- Valores ausentes não são zero; dados estimados devem ser rotulados como estimados.
- Escrever no Health Connect exige a permissão WRITE correspondente ao tipo de dado, declarada no manifesto e autorizada pelo usuário.
- Para passos, usar `StepsRecord` com intervalo de tempo e metadados de dispositivo do tipo relógio. Sincronizações repetidas devem usar Client Record ID/Version para evitar duplicação.
- Não escrever dados gerados por simulação, fixture, cálculo não validado ou captura BLE bruta.

### B. Notificações: Android → companion → wearable

```text
Apps do Android
        ↓ publicação normal de notificações
NotificationListenerService (somente após autorização do usuário)
        ↓ filtro pelos pacotes escolhidos
Normalização mínima / prévia em memória
        ↓ camada de transporte do wearable
Smartwatch
```

Regras:
- A finalidade do acesso a notificações é retransmiti-las ao próprio dispositivo vestível.
- Solicitar autorização especial pelo fluxo oficial do Android, explicar a finalidade antes de abrir as configurações e exigir seleção explícita de apps.
- O usuário pode pausar, revogar e limpar prévias.
- A leitura não executa respostas rápidas, ações, exclusão, arquivamento ou interação com o app de origem.
- Não acessar banco privado, credenciais ou conteúdo que o Android oculte/redija.
- O cache atual é volátil, com limite de 30 prévias e retenção máxima de 15 minutos; não registrar corpos de mensagens em logs ou rede.
- Encaminhar ao relógio exige protocolo/SDK de transporte compatível. Não escrever payload BLE desconhecido.

**Estado técnico:** captura consentida e visualização local implementadas na variante companion. O transporte de notificações até o GT3 Maxx continua bloqueado porque o protocolo correspondente não foi validado. O app não deve anunciar a retransmissão como concluída até esse teste.

## 3. Estado de cada métrica nesta versão

| Métrica/função | Fonte pretendida | Estado real |
|---|---|---|
| Passos no painel | Health Connect agregado | Leitura agregada implementada; pode combinar telefone e outras fontes |
| Passos originados no GT3 Maxx | Sensor do relógio via Wearfit Pro/BLE | Não comprovado; ainda não gravar no Health Connect |
| Frequência cardíaca | Sensor óptico do relógio | Protocolo/SDK e dados não validados |
| Sono | Sensor/algoritmo do relógio | Integração não validada |
| SpO₂ | Sensor do relógio | Integração não validada; não usar para diagnóstico |
| Bateria | Quadro BLE candidato NUS/AB | Candidato, não validado por comparação repetida |
| Notificações selecionadas | Android NotificationListenerService | Captura local implementada; retransmissão física ainda não validada |
| Ajuste de horário | Android ↔ relógio | Protocolo não identificado |

## 4. Google Play e Play Protect

A orientação oficial do Google Play Protect cita como uso permitido de `NOTIFICATION_LISTENER` os aplicativos de Saúde e Fitness que retransmitem notificações aos seus próprios dispositivos vestíveis. Esse é o caso de uso pretendido para o Relógio 2.0. A categoria declarada sozinha não é uma isenção: a função deve realmente fazer a ponte com o wearable, com consentimento explícito, filtro por aplicativo e transparência.

Para publicar:
- Categoria da ficha: **Saúde e fitness**.
- Health apps declaration: marcar apenas **Atividade e fitness** e os tipos de dados realmente acessados.
- Preencher Segurança dos dados de acordo com comportamento e SDKs reais.
- Publicar uma política de privacidade pública, ativa e não editável, e exibir o texto/link dentro do app.
- Explicar o acesso a dados de saúde e notificações em destaque antes do consentimento.
- Distribuir a variante companion com Notification Listener pelo Google Play, e não como APK enviado diretamente por navegador/mensageiro.
- Manter a variante diagnostic sem Notification Listener apenas para validação local de BLE e Health Connect.

A categoria de saúde não elimina automaticamente bloqueios do Play Protect. O Google pode verificar permissões, comportamento, binário e dependências.

## 5. Health Connect não é a nuvem da Conta Google

Health Connect é uma camada de dados centrada no dispositivo, com permissões controladas pelo Android. Escrever um registro nele disponibiliza o dado a outros apps compatíveis autorizados; não significa upload automático à Conta Google.

A documentação de migração do Google Fit diferencia:
- **Health Connect:** armazenamento e compartilhamento on-device, recomendado para tracking móvel e agregação de passos.
- **Google Health API:** integração cloud/account-centric, indicada quando o produto precisa de integração de nuvem/conta Google, com projeto e OAuth próprios.

O Relógio 2.0 não declara a Google Health API como implementada. Não usar Google Fit legado como solução nova.

## 6. Permissões mínimas

Estado atual:
- `READ_STEPS`: declarada e usada para ler o agregado de passos.
- `WRITE_STEPS`: ainda não declarada; aguarda uma fonte validada de passos do relógio.
- Frequência cardíaca, SpO₂, sono e outros tipos: não solicitar permissões até existir uma feature funcional, fonte comprovada e justificativa específica.
- Bluetooth: usar apenas SCAN/CONNECT necessários à conexão do wearable.
- Notification Listener: somente na variante companion, com consentimento e finalidade de ponte.

## 7. Testes de aceitação antes de publicação

1. Comparar passos do relógio, Wearfit Pro, Relógio 2.0 e Health Connect no mesmo intervalo e identificar cada origem.
2. Validar timestamps, fuso, contagem incremental, duplicação e reconexão.
3. Confirmar que o app não publica no Health Connect valores de telefone como se fossem do relógio.
4. Validar leitura de notificações com cada pacote selecionado, pausa, revogação e limpeza.
5. Confirmar que notificações chegam ao wearable apenas por transporte documentado/validado, sem alterar o app de origem.
6. Inspecionar manifestos de diagnostic e companion, permissões Health Connect e serviços.
7. Testar no Galaxy A06 e revisar o resultado do Play Protect.
8. Completar Saúde e fitness, Segurança dos dados e política de privacidade no Play Console.

## 8. Fontes oficiais

- Play Protect — orientação para apps com Notification Listener: https://developers.google.com/android/play-protect/warning-dev-guidance?hl=pt-BR
- Publicar app de saúde no Google Play: https://developer.android.com/health-and-fitness/health-connect/publish
- Gravar dados no Health Connect: https://developer.android.com/health-and-fitness/health-connect/write-data
- Passos no Health Connect: https://developer.android.com/health-and-fitness/health-connect/features/steps
- Migração Google Fit: https://developer.android.com/health-and-fitness/health-connect/migration/fit
- Declaração de dados e privacidade: https://developer.android.com/privacy-and-security/declare-data-use
