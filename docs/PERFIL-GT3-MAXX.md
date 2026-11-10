# Perfil do dispositivo: GT3 Maxx

## Identificação informada pelo usuário

- Nome comercial: GT3 Maxx
- Categoria: smartwatch genérico chinês
- Aplicativo companheiro: Wearfit Pro
- Telefone de desenvolvimento: Samsung Galaxy A06, Android 16
- Estado: pareamento pelo Wearfit Pro informado pelo usuário; protocolo e chipset ainda não identificados.

## Identificação da unidade física informada (04/11/2026)

- Nome comercial exibido: GT3 MAX Call
- Modelo exibido: GT3 MAX
- Firmware informado: `V1.07`
- Kernel / versão de software: `SH0050-1_1.1.3_1.23_1.18_1.1`

O usuário também forneceu número de série, código e endereço Bluetooth para identificação. Esses identificadores foram omitidos deste arquivo para evitar exposição em documentação pública.

**Interpretação pendente:** a string SH0050 pode representar plataforma/build ou versão composta, mas não há documentação pública confiável que permita identificar o SoC ou decodificar os componentes. A versão V1.07 não corresponde textualmente à versão NJY-N03-1.0.4 da amostra FCC de 2022. Não assumir que são a mesma revisão de hardware.

## Características físicas/comerciais relatadas (04/11/2026)

O usuário compartilhou a seguinte descrição geral do produto/anúncio:
- Sensor óptico central anunciado para frequência cardíaca e oxigenação (SpO₂).
- Bluetooth integrado para sincronização com Android e iOS.
- Parte traseira descrita como plástico/vidro, com marcações RoHS e CE.

**Classificação da evidência:** descrição comercial genérica, não inspeção independente do aparelho nem ficha técnica do fabricante. Não identifica componente óptico, fabricante do sensor, chipset, versão de firmware, serviços GATT, algoritmo, taxa de amostragem ou margem de erro. As marcações RoHS/CE, mesmo quando presentes, não comprovam precisão clínica nem liberam acesso programático às medições. O sensor e qualquer leitura direta continuam **não validados**. A descrição não fornece evidência para pressão arterial.

## Evidência BLE adicional (03/11/2026)

O inventário observado inclui Nordic UART com UUIDs terminados em dcca9e e a notificação AB 00 05 FF 91 80 00 0C. Existe uma referência pública DT78/X8 Ultra com os mesmos UUIDs e uma fixture AB de bateria. Isso sustenta um perfil **NUS/AB candidato**, mas não identifica chipset ou modelo. A hipótese de bateria 12% não foi validada com o Wearfit Pro. Detalhes: PROTOCOLO-BLE-CANDIDATO.md.

## Pista de identificação regulatória — ainda não vinculada à unidade física

Existe um cadastro público de FCC ID `2A56X-GT3MAX`, modelo `GT3MAX`, atribuído à NJY Technology Co., Limited, com registro de 2022. Isso é apenas uma pista: o usuário ainda não confirmou que o rótulo/firmware da unidade em mãos contém esse FCC ID.

Um dos PDFs listados nesse cadastro (documento 6135696) redireciona para uma cópia rotulada GT4 e descreve conexão pelo MActivePro, não pelo Wearfit Pro. Por esse conflito, esse manual não foi usado como especificação do relógio atual. A captura de tela do PDF espelhado retornou 403; o texto indexado foi tratado apenas como alerta de inconsistência.

Referências:
- Cadastro FCC ID 2A56X-GT3MAX: https://fccid.io/2A56X-GT3MAX
- Página de metadados do manual: https://fccid.io/2A56X-GT3MAX/User-Manual/User-Manual-6135682
- Resultado espelhado com redirecionamento: https://fcc.report/FCC-ID/2A56X-GT3MAX/6135696.pdf

Próxima confirmação física: verificar em Configurações → Sobre (ou etiqueta/caixa) modelo exato, versão de firmware/hardware e FCC ID, se houver. Não publicar serial nem MAC.

## Hipóteses que NÃO devem ser tratadas como fatos

O nome GT3 Maxx não identifica de forma inequívoca fabricante, SoC, firmware ou protocolo BLE. Não assumir compatibilidade com FitPro, DaFit, GloryFit ou qualquer protocolo publicado apenas por semelhança visual/comercial.

## Plano de identificação

1. Capturar nome anunciado pelo BLE, fabricante (Manufacturer Data), service UUIDs e RSSI.
2. Inspecionar serviços e características GATT com nRF Connect, sem escrever em nenhuma característica.
3. Comparar UUIDs e propriedades com referências públicas apenas como pistas.
4. Fazer captura repetida em duas sessões e registrar diferenças.
5. Comparar dados exibidos pelo Wearfit Pro com qualquer notificação/leitura observada.
6. Determinar se o relógio anuncia separadamente Bluetooth Classic para chamadas; esse canal não deve ser confundido com BLE de sensores.

## Segurança

- Não executar OTA, DFU, reset, calibração ou comandos de configuração.
- Não enviar bytes para características write/write-no-response.
- Não publicar MAC, serial, identificadores pessoais ou capturas com dados privados.
- Frequência cardíaca, SpO2 e pressão estimada não devem ser apresentados como diagnóstico médico. Em particular, pressão estimada por relógio não substitui aparelho de braço validado.

## Próxima evidência necessária

Captura de tela do nRF Connect após scan, mostrando o dispositivo GT3 Maxx e, após conexão, a lista de serviços GATT. Ocultar endereço MAC antes de compartilhar. Se o Wearfit Pro mostrar versão de firmware/hardware, registrar também.
