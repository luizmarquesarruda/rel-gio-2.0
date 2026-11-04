# Integração oficial de terceiros — Wearfit Pro / GT3 MAX Call

**Revisão:** 04/11/2026  
**Projeto consumidor:** Relógio 2.0 (Android/Kotlin)  
**Dispositivo:** GT3 MAX Call, firmware informado V1.07, build informado SH0050-1_1.1.3_1.23_1.18_1.1

## 1. Evidência decisiva da declaração de privacidade

A declaração de privacidade do Wearfit Pro fornecida pelo proprietário (atualização indicada: 04/08/2023) declara que, depois do pareamento bem-sucedido, aplicativos de terceiros que interajam com o wearable pareado **por meio do Wearfit Pro** podem:

- obter um identificador aleatório do dispositivo, redefinível e exclusivo para cada aplicativo;
- consultar nome, informações de hardware e estado do dispositivo (carregando/em uso);
- enviar mensagens ou notificações ao wearable;
- mediante autorização expressa, acessar dados de sensores, estado do sono, tipo de exercício (por exemplo, caminhada/corrida), estado de uso/vestimenta, logs, localização e informações do dispositivo.

A declaração oficial publicada pela Wearfit Pro também descreve esse canal de terceiros e a exigência de autorização expressa. Fonte: https://wearfit.app/statement-on-wearfit-pro-and-privacy/ (seção 1.2). A página declara a revisão de 24/02/2022 e vigência a partir de 10/03/2022; a cópia fornecida pelo proprietário está marcada como atualizada em 04/08/2023. As duas versões não devem ser presumidas idênticas em todos os detalhes.

### Interpretação correta

Isto é evidência de que o ecossistema Wearfit Pro prevê integração de aplicativos de terceiros. **Não é, por si só, uma especificação de SDK/API pública.** A política não informa:
- nome e versão do SDK de integração de dispositivo;
- coordenadas Maven/AAR, pacote Kotlin/Java ou serviço Android;
- interface de autorização/consentimento para o app parceiro;
- callbacks, intents, Content Provider, Binder, broadcast ou protocolo local;
- esquema das amostras, unidades, timestamps, qualidade e paginação do histórico;
- compatibilidade do canal de terceiros com o modelo específico GT3 MAX Call;
- se o envio de mensagens usa uma API local ou serviço remoto.

Portanto, a política abre uma rota oficial plausível, mas a implementação real exige o SDK/contrato técnico fornecido pela Weike/WAKE UP e autorização do programa de parceiros.

## 2. Não confundir a lista de SDKs de terceiros

A página third_Party_SDK_en.html é um inventário dos SDKs que o próprio Wearfit Pro integra (estatística/analytics, publicidade, mapas, push, pagamento, autenticação, TTS e afins). Ela **não é um download de SDK de integração do relógio** e não apresenta documentação de API do canal de terceiros descrito na política.

Fonte: https://h5.iwhop.com/login/privacy_policy/third_Party_SDK_en.html

Não copiar para o Relógio 2.0 SDKs de anúncios, analytics, pagamento, redes sociais, push ou mapas do Wearfit Pro. Eles não são necessários para acessar o GT3 MAX e ampliariam permissões e coleta de dados sem finalidade.

## 3. Sinal público de programa de parceiros

A página corporativa da WAKE UP/WEARFIT descreve uma “open and sharing ecological platform” e convida parceiros a ingressar na plataforma aberta. Também apresenta contato para cooperação comercial.

Fonte: https://iwhop.cn/terminalAPP

Contato de cooperação comercial publicado na página: 0755-28718599. E-mail de cooperação de cadeia de suprimentos publicado: chenxia@iwhop.com. O endereço common@iwhop.com é indicado na declaração de privacidade para dúvidas sobre privacidade. Esses contatos são evidência de canais públicos de contato, não confirmação de que um SDK já foi concedido.

## 4. Contrato técnico a solicitar ao fornecedor

Solicitar formalmente:

1. SDK Android para parceiros integrados ao Wearfit Pro, compatível com Android 16/API 36.
2. Nome do artefato, versão, checksum, licença, minSdk/targetSdk e instruções de distribuição.
3. Método de associação do app parceiro ao relógio já pareado no Wearfit Pro; política de vínculo, desvinculação e rotação do identificador aleatório.
4. Fluxo de autorização granular e revogação para: estado/bateria, frequência cardíaca, SpO₂, sono, exercício/passos, pressão estimada, estado de uso, logs e localização.
5. API de leitura em tempo real e histórico, com unidade, timezone, timestamp da medição, timestamp de sincronização, fonte, qualidade, limites, paginação e deduplicação.
6. API de envio de notificações/mensagens, limites de tamanho, codificação, confirmação, comportamento offline e tratamento de falhas.
7. Compatibilidade explicitamente confirmada com GT3 MAX / variante GT3 MAX Call, firmware V1.07, build SH0050-1_1.1.3_1.23_1.18_1.1.
8. Regras de privacidade, retenção, localização dos servidores e se os dados podem ser mantidos apenas localmente pelo aplicativo parceiro.
9. App de exemplo, documentação API, ambiente de teste e processo de homologação.

Nenhuma senha, token, número de série, MAC ou código pessoal deve ser enviado em issue pública. Usar somente canal de contato oficial e compartilhar identificadores da unidade apenas em comunicação privada quando solicitados e necessários.

## 5. Arquitetura alvo no Relógio 2.0

Criar uma porta independente, sem acoplar UI ao SDK do fornecedor:

- WearableDataSource: contrato de leitura de capacidades, estado do dispositivo, eventos e histórico.
- WearfitPartnerAdapter: implementação futura somente após receber e revisar o SDK oficial.
- Gt3MaxBleAdapter: fallback para dados BLE comprovados localmente; continua isolado do transporte.
- HealthConnectPublisher: adaptador separado e opcional para publicar somente tipos de dados realmente recebidos, depois de autorização por tipo e revisão de políticas.
- DeviceCapabilityRegistry: declara por dispositivo/variante quais recursos a API confirma; não inferir todas as funções da lista genérica do Wearfit Pro.

Toda amostra deve preservar:
- métrica e unidade;
- valor bruto e valor normalizado;
- horário da medição e horário de recebimento/sincronização;
- fonte explícita (WEARFIT_PARTNER_SDK, GT3_MAX_BLE, HEALTH_CONNECT_AGGREGATE);
- identificador aleatório do wearable no escopo da instalação, se fornecido;
- estado de validação e qualidade;
- consentimento/escopo que autorizou a leitura.

Não usar o MAC/SN como chave persistente de dados de saúde. O identificador aleatório é por app e redefinível; tratar rotação/reset como novo vínculo e reconciliar apenas com consentimento. Não atribuir agregado do Health Connect ao relógio.

## 6. O que o código atual comprova — e o que não comprova

- O app consegue fazer scan, conectar por BLE/GATT e enumerar serviços na captura já realizada.
- O perfil GT3 MAX Call criado nesta branch compara nome e fingerprint GATT da unidade.
- O canal NUS TX pode emitir notificações; o quadro AB 0x91/0x80 é interpretado apenas como bateria candidata.
- O app não executa comandos proprietários por NUS RX nem escreve características privadas desconhecidas.
- A captura GATT anterior não comprova o SDK ponte do Wearfit Pro; não há ainda artefato/API oficial parceiro integrado.
- Frequência cardíaca, SpO₂, pressão, sono, passos próprios do relógio, histórico de exercício, chamadas Classic e controles ainda precisam de origem e contrato validados.

## 7. Decisão

**Prioridade:** obter e avaliar primeiro o SDK/API oficial de parceiro Wearfit Pro, porque a própria política contempla acesso autorizado de terceiros e envio de mensagens pelo ecossistema. Manter o driver BLE atual como caminho de compatibilidade e diagnóstico, sem o tratar como protocolo completo do relógio.

Não implementar API inventada, não fazer introspecção de sessão privada do Wearfit Pro, não reutilizar tokens de terceiros, não interceptar tráfego autenticado e não assumir que a autorização genérica da política dispensa o consentimento explícito do usuário.

## Fontes

- Wearfit Pro — Statement on Wearfit Pro and Privacy: https://wearfit.app/statement-on-wearfit-pro-and-privacy/
- WAKE UP / WEARFIT — página de produto e plataforma/ecossistema aberto: https://iwhop.cn/terminalAPP
- Wearfit Pro — Third-Party SDK List (inventário dos SDKs usados pelo próprio app, não SDK público do relógio): https://h5.iwhop.com/login/privacy_policy/third_Party_SDK_en.html
- Política de privacidade detalhada fornecida pelo proprietário nesta conversa, marcada como atualizada em 04/08/2023.
