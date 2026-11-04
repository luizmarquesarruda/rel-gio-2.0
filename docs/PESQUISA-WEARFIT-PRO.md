# Pesquisa: Wearfit Pro

## Fontes consultadas

1. Declaração de privacidade: https://wearfit.app/statement-on-wearfit-pro-and-privacy/
2. Contrato de uso: https://wearfit.app/wearfit-pro-user-agreement/
3. Referência independente OpenWearOTA (FitPro, não Wearfit Pro): https://github.com/HackSucks/OpenWearOTA/blob/main/DOCUMENTATION.md

## Recursos funcionais documentados pelo fornecedor

O site público do Wearfit Pro apresenta recursos para frequência cardíaca, registros esportivos, sono, oxigênio no sangue, pressão arterial, glicose, fadiga, temperatura, frequência respiratória e ciclo menstrual. A FAQ também lista reconhecimento automático de atividade, metas, chamada, música, câmera remota, alarmes, cronômetro, timer, localizar telefone, notificações de mensagens, lembretes de saúde, brilho, idioma e ajustes do dispositivo.

Essas páginas descrevem o ecossistema Wearfit Pro; não garantem que todos os recursos estejam presentes no GT3 MAX específico ou que seus valores possam ser extraídos por BLE. Sensores, precisão, firmware e disponibilidade variam por modelo.

A documentação pública de notificações ensina a ativar Dispositivo → Message notification e lista telefone, SMS, WeChat, QQ e Weibo. WhatsApp não aparece nessa lista pública. A compatibilidade específica da versão instalada permanece **não validada**.

## O que está documentado

A declaração do Wearfit Pro descreve:
- gerenciamento de dispositivos e conexão Bluetooth;
- categorias de dados esportivos e de saúde como passos, distância, calorias, frequência cardíaca, sono, SpO2, pressão e respiração;
- funções de conveniência e notificações descritas nas páginas de ajuda do fornecedor;
- identificador aleatório, nome, informações de hardware e estado do dispositivo;
- possibilidade de aplicações de terceiros interagirem com o dispositivo pareado pelo Wearfit Pro;
- acesso a dados de sensores, sono, tipo de exercício, estado de uso, logs e localização mediante autorização expressa.

## O que NÃO está comprovado

- disponibilidade pública de SDK para desenvolvedores independentes;
- nome, versão, artefato Maven ou documentação de API de integração;
- compatibilidade dessa integração com o relógio específico do usuário;
- compatibilidade Android com Health Connect; o material público consultado menciona HealthKit no site do produto, o que não prova suporte Health Connect;
- protocolo BLE, UUIDs, comandos ou formato de pacotes do dispositivo.

A política de privacidade não é documentação de SDK. Não presumir que o aplicativo possa consultar o banco interno do Wearfit Pro, reutilizar sessão de conta ou acessar endpoints privados.

## Privacidade e armazenamento

A declaração de privacidade informa que certas categorias de dados podem ser sincronizadas com a nuvem do Wearfit Pro e cita armazenamento em servidores na China para os serviços descritos. O Relógio 2.0 não deve reutilizar conta, sessão, endpoints privados nem enviar dados para serviços externos sem uma integração oficial e consentimento específico. O padrão do projeto permanece armazenamento local e minimização.

## Caminhos a investigar

1. Contato com o desenvolvedor Wakeup/Weike para solicitar SDK/API e termos de integração.
2. Verificar se o Wearfit Pro oferece exportação/compartilhamento de dados na versão instalada.
3. Identificar o relógio e investigar BLE GATT com ferramenta de inspeção, sem escrita.
4. Avaliar Android Health Connect apenas como fonte complementar, se o Wearfit Pro realmente exportar/sincronizar dados compatíveis.

## Documentação pública adicional

- Funções do aplicativo: https://wearfit.app/wearfit-pro-app-function/
- FAQ do aplicativo: https://wearfit.app/frequently-asked-questions-about-wearfit-pro-app/
- Monitoramento do sono: https://wearfit.app/sleep-monitoring/
- Monitoramento de frequência cardíaca: https://wearfit.app/measure-heart-rate/
- Notificações de mensagens: https://wearfit.app/message-notification/

## Referência OpenWearOTA

O projeto OpenWearOTA descreve protocolos de produtos FitPro e é material de engenharia reversa independente. Não deve ser tratado como protocolo Wearfit Pro nem aplicado sem identificar chipset e validar capturas. Qualquer operação OTA/firmware está fora do escopo.

## Evidência BLE específica do GT3 MAX (03/11/2026)

Foi comparado o inventário local com https://github.com/fbiego/dt78/blob/master/dt78_ble_server/dt78_ble_server.ino, um emulador BLE DT78/X8 Ultra para WearFit 2.0. O código usa os UUIDs NUS terminados em dcca9e e contém a fixture AB 00 05 FF 91 80 00 50, comentada como bateria 80%. O GT3 MAX expôs os mesmos UUIDs e enviou AB 00 05 FF 91 80 00 0C.

A coincidência é forte o bastante para registrar um **perfil candidato NUS/AB**, mas não prova chipset, modelo, fabricante ou compatibilidade total com Wearfit Pro. A interpretação de 12% é uma hipótese de bateria ainda não validada por comparação simultânea com o Wearfit Pro. Ver PROTOCOLO-BLE-CANDIDATO.md.

Não confundir com https://github.com/victorbillyph/chinese-smartwatch-homeassistant, que documenta Bluetrum AB5610 com UUID NUS terminado em dcca9f e quadros DF/FD. O GT3 MAX capturado usa dcca9e e quadro AB; copiar o parser DF/FD seria incorreto.

Nenhum comando de consulta, configuração, sync de hora, reset ou OTA foi enviado. A referência de protocolo é utilizada somente para parsing offline e comparação de notificações já recebidas.
