# Privacidade, segurança e governança de dependências

## Objetivo

Definir uma linha de base verificável para o Relógio 2.0. O documento do Wearfit Pro intitulado "Third-Party SDK List", atualizado em 04/08/2023, é usado apenas como referência comparativa. Ele declara SDKs e categorias de dados tratados pelo Wearfit; não comprova que esses SDKs estejam presentes no APK instalado nem que cada campo seja coletado em toda execução.

## Política do Relógio 2.0

- Sem anúncios, banners, publicidade personalizada ou atribuição publicitária.
- Não integrar SDKs de publicidade, marketing comportamental ou monetização.
- Não coletar IMEI, IMSI, número de série do telefone, MAC do telefone, SSID/BSSID ou lista de aplicativos instalados para analytics.
- Não usar identificadores publicitários nem criar identificadores persistentes de rastreamento.
- Não enviar dados de saúde, localização precisa, notificações ou identificadores a servidores externos por padrão. Exceção explicitamente aprovada: a função de clima envia o nome de cidade digitado na geocodificação e as coordenadas da cidade escolhida ao Open-Meteo, sem localização GPS atual, dados de saúde ou identificadores.
- Bluetooth, Health Connect, mapas e recursos de IA só podem comunicar com serviços externos quando a função exigir, com finalidade documentada e consentimento adequado.
- Diagnósticos devem evitar conteúdo de notificações, dados de saúde e identificadores diretos. Logs devem ser locais, mínimos e removíveis.
- Toda permissão Android precisa estar ligada a uma função implementada, explicada ao usuário e testável.
- Dependências diretas e transitivas devem ser inventariadas; novas dependências precisam de justificativa e revisão de licença, manutenção, permissões e comportamento de rede.

## SDKs declarados pelo Wearfit Pro na lista de 04/08/2023

| SDK / fornecedor citado | Finalidade declarada | Tratamento no Relógio 2.0 |
|---|---|---|
| Content SDK / Beijing Juliang Engine | Recomendação de conteúdo | Não integrar |
| Growth Marketing Suite / Volcano Engine | Análise de dados | Não integrar |
| ToBid / Beijing Chuangzhi | Publicidade, atribuição e antifraude | Não integrar |
| Alipay SDK | Pagamento e segurança de pagamento | Não integrar sem requisito de pagamento aprovado |
| Umeng Statistics | Estatísticas | Não integrar analytics comercial |
| Pangle | Publicidade e atribuição | Não integrar |
| Tencent GDT | Publicidade e atribuição | Não integrar |
| JPush | Push de serviços | Não integrar; notificações locais Android são suficientes para o escopo atual |
| AutoNavi Map | Mapas e localização | Preferir APIs Android/Google adequadas; justificar qualquer SDK futuro |
| Baidu Map | Mapas e navegação | Preferir APIs Android/Google adequadas; justificar qualquer SDK futuro |
| WeChat Open SDK | Login, compartilhamento e pagamento | Não integrar sem requisito aprovado |
| Outros 19 SDKs | Não detalhados na versão inglesa fornecida | Não presumir identidade; exigir inventário da versão integral antes de qualquer decisão |

A lista original declara ainda coleta de diversos identificadores de dispositivo, informações de rede, localização, sensores e comportamento. Essa declaração deve ser tratada como alerta de risco e não como evidência forense de tráfego ou conteúdo do APK.

## Estado observado no repositório

Na revisão de 03/11/2026, as dependências diretas declaradas em `app/build.gradle.kts` eram:
- `androidx.activity:activity-ktx:1.10.1`
- `androidx.health.connect:connect-client:1.1.0`
- `androidx.work:work-runtime-ktx:2.12.0` — atualização meteorológica periódica condicionada à rede.
- `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2`
- JUnit para testes.

Nenhum SDK de publicidade ou analytics comercial aparece nessa lista direta. Isso não basta para concluir que a árvore transitiva completa está livre de tais componentes; por isso o CI também verifica as dependências runtime das variantes.

Permissões explícitas no manifesto principal incluem Bluetooth/scan/connect, localização legada limitada ao Android 11, READ_STEPS do Health Connect, INTERNET e ACCESS_NETWORK_STATE. INTERNET é exceção de escopo restrito para geocodificação e previsão Open-Meteo; não habilita envio de saúde, notificações ou identificadores. A variante companion adiciona Notification Listener. A verificação automatizada exige exatamente uma declaração INTERNET e continua bloqueando o conjunto de permissões invasivas não justificadas.

## Verificação automatizada

O workflow Android CI executa `scripts/check-privacy-guardrails.sh` depois da compilação. A verificação:
1. Gera as árvores runtime de dependências diagnostic e companion.
2. Falha se encontrar namespaces conhecidos de SDKs publicitários/analytics listados nesta política.
3. Inspeciona permissões declaradas nos APKs e falha para permissões sensíveis não aprovadas.
4. Não substitui revisão manual, SBOM, análise dinâmica de tráfego ou auditoria do APK final.

A lista de namespaces é uma barreira inicial, não uma garantia universal: SDKs podem usar outros identificadores, nomes de artefato ou carregamento dinâmico. Toda dependência nova continua exigindo revisão.

## Limitações e validação

- O documento de terceiros está datado de 04/08/2023; pode não refletir versões atuais.
- Não foi realizada captura de tráfego de rede do Wearfit Pro nesta etapa.
- Não foi feita análise forense do APK do Wearfit Pro.
- A ausência de SDKs conhecidos no build do Relógio 2.0 não foi verificada antes da execução do novo guardrail.
- O guardrail CI precisa passar no novo commit para ser considerado validado.
- Teste físico de privacidade no Galaxy A06: **não validado**.

## Processo para aprovar exceções

Qualquer exceção deve incluir: funcionalidade necessária, dados acessados, permissões exigidas, destinos de rede, política de retenção, base de consentimento, avaliação de licença e segurança, alternativa sem SDK e aprovação documentada no diário de bordo. Não incluir SDK apenas por conveniência de analytics ou monetização.


## Exceção de rede controlada — clima

A versão 0.4.0-alpha.4 adiciona acesso HTTPS à Open-Meteo para pesquisa de cidades e previsão meteorológica. O usuário escolhe a cidade manualmente; a função não solicita GPS nem localização em segundo plano. O nome da cidade é enviado ao endpoint de geocodificação e as coordenadas da cidade escolhida são enviadas ao endpoint de previsão. Nenhum MAC, serial, ID, dado de saúde ou conteúdo de notificação é enviado.

A previsão é armazenada em SharedPreferences no sandbox privado do aplicativo. O WorkManager executa atualização periódica com rede disponível, mas o horário não é exato e pode ser adiado por Doze/economia de bateria. A tela mostra a atribuição Open-Meteo e a documentação do projeto registra a licença dos dados CC BY 4.0 e a limitação de uso gratuito não comercial. Para uma finalidade comercial, é necessária nova avaliação contratual.

O auditor de manifesto permite exatamente uma permissão INTERNET e bloqueia zero ou múltiplas declarações. O script de guardrails continua bloqueando permissões sensíveis; INTERNET foi removida apenas da lista de proibições gerais. A nova política ainda precisa passar na CI e ser confirmada no manifesto dos dois APKs.
