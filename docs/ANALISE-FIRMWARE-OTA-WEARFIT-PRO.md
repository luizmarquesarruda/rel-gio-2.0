# Investigação de firmware OTA — GT3 MAX / Wearfit Pro

**Revisão:** 04/11/2026  
**Estado:** protocolo de transporte identificado; pacote binário específico ainda não obtido.  
**Regra:** análise somente. Nenhuma atualização/flash deve ser iniciada por esta investigação.

## 1. Identidade regulatória encontrada

O registro público FCC `2A56X-GT3MAX` identifica um equipamento chamado GT3 MAX, atribuído à NJY Technology Co., Limited.

O relatório de teste de rádio de 2022 descreve a unidade de ensaio com:
- Software: `NJY-N03-1.0.4`
- Hardware: `M01_MB_V1.1`
- Bateria nominal: 270 mAh
- Bluetooth 5.1 / BLE

Fontes:
- FCC ID: https://fccid.io/2A56X-GT3MAX
- Relatório de teste (espelho do PDF): https://fcc.report/FCC-ID/2A56X-GT3MAX/6135680.pdf
- Relatório de exposição RF: https://fcc.report/FCC-ID/2A56X-GT3MAX/6135693.pdf

**Limite:** esses identificadores pertencem à amostra ensaiada em 2022. Ainda não foi confirmado que a unidade do usuário apresenta o mesmo código de software e revisão de placa. O manual listado no cadastro FCC redireciona para um documento cujo conteúdo descreve GT4/MActivePro; por esse conflito, ele não foi usado como especificação do GT3 MAX conectado ao Wearfit Pro.

## 1A. Identificação fornecida da unidade física — 04/11/2026

O usuário informou os seguintes dados na tela/sistema do próprio relógio:
- Nome/modelo comercial: `GT3 MAX Call` / `GT3 MAX`
- Versão de firmware exibida: `V1.07`
- Campo Kernel / Versão de Software: `SH0050-1_1.1.3_1.23_1.18_1.1`

O número de série, endereço Bluetooth e código de pareamento foram recebidos para correlação, mas foram deliberadamente omitidos deste documento e não devem ser publicados em issues, logs ou artefatos.

### Interpretação

- `V1.07` é a versão de firmware informada pelo usuário e deve ser tratada como versão instalada até nova leitura pelo Wearfit Pro.
- `SH0050-1_1.1.3_1.23_1.18_1.1` parece ser um identificador composto de plataforma/build ou componentes de software. Não foi encontrada correspondência pública indexada para a string exata nem documentação confiável que decodifique os segmentos.
- Não é correto inferir que `SH0050` seja um SoC, fabricante de chipset ou nome de placa sem evidência adicional.
- A unidade informada não pode ser considerada idêntica à amostra FCC de 2022 (`NJY-N03-1.0.4` / `M01_MB_V1.1`). Há uma diferença clara entre as identificações de software, e a revisão de hardware da unidade atual ainda é desconhecida.
- O sufixo comercial `Call` pode indicar uma variante com chamadas Bluetooth, mas não comprova uma placa ou pacote OTA compatível com a variante GT3 MAX simples.

### Resultado da busca pública usando os identificadores novos

Foram pesquisadas as strings exatas `SH0050-1_1.1.3_1.23_1.18_1.1`, `SH0050`, `GT3 MAX Call V1.07` e combinações com Wearfit Pro. Não foi encontrado um pacote oficial de firmware, manifesto, changelog ou repositório público atribuível a essa versão.

A busca continua inconclusiva; ausência de resultados indexados não prova que o firmware não exista no servidor OTA do fabricante.

## 2. Mecanismo DFU observado na captura do GT3 MAX

A captura GATT do usuário já havia registrado o serviço Secure DFU `0xFE59`, com características DFU Control Point e DFU Packet.

O perfil oficial Nordic Secure DFU define:
- Serviço: `0xFE59`
- Control Point: `8EC90001-F315-4F60-9FB8-838830DAEA50`
- Packet: `8EC90002-F315-4F60-9FB8-838830DAEA50`

Referência Nordic:
https://docs.nordicsemi.com/r/bundle/nrf5_sdk_v14.1.0/page/lib_dfu_transport_ble.html

A correspondência de UUIDs é forte e específica para o transporte Nordic Secure DFU. Ela não prova que o relógio use um SoC Nordic nRF51/nRF52 genuíno, que o firmware seja público, nem que qualquer arquivo Nordic genérico seja compatível.

## 3. Como o Wearfit Pro entrega o firmware

A documentação pública do Wearfit Pro descreve o fluxo de atualização em Dispositivo → Firmware Update. A declaração de privacidade diz que o Wearfit Pro atua como caminho de atualização e que a versão do firmware é fornecida pelo fabricante do dispositivo.

Fontes:
- Procedimento OTA: https://wearfit.app/ota-upgrade-equipment/
- Política do Wearfit Pro: https://wearfit.app/statement-on-wearfit-pro-and-privacy/

Uma captura de logs publicada por usuários do Wearfit Pro em fórum (4PDA, não fonte oficial) mostra um fluxo de exemplo em que o servidor retorna campos como `fileUrl`, `fileSize`, `patchFileUrl`, `userDataUrl` e `nextVersion`. O app descarrega um arquivo `*_ota.zip` em:

`/storage/emulated/0/Android/data/com.wakeup.howear/files/Download/ota/`

O mesmo exemplo registra uma consulta ao endpoint de atualização com parâmetros próprios do dispositivo (código/projeto/versão/identidade), uma resposta com URL de arquivo e versão-alvo, e a chamada de um gerenciador `NordicOtaMgr` para transferir via `8EC90001` e `8EC90002`. Isso explica por que o binário não é necessariamente um link público estático: o servidor seleciona a imagem a partir da identificação enviada pelo app.

Fonte comunitária:
https://4pda.to/forum/index.php?showtopic=1039984&st=5800

**Limite importante:** esse log é de outro usuário/modelo e não fornece prova de que o ZIP daquele exemplo seja o firmware do GT3 MAX do projeto. Ele serve como evidência do caminho de aquisição/armazenamento usado por uma versão do Wearfit Pro, não como imagem recomendada.

## 4. Formato provável do pacote

O perfil Nordic Secure DFU usa um pacote de distribuição ZIP. Pacotes Nordic usuais incluem `manifest.json`, um init packet `.dat` e uma ou mais imagens `.bin`; o init packet carrega metadados usados pelo bootloader para verificar a atualização.

Referências:
- Nordic Android DFU Library: https://github.com/NordicSemiconductor/Android-DFU-Library
- Formato/validação Secure DFU: https://docs.nordicsemi.com/r/bundle/nrf5_sdk_v14.1.0/page/lib_dfu_transport_ble.html

O pacote real do Wearfit Pro ainda não foi inspecionado. Portanto, ZIP/manifest/`.dat`/`.bin` são a expectativa técnica baseada no perfil DFU, não uma afirmação sobre o arquivo específico do GT3 MAX.

## 5. Resultado da busca pública pelo binário

Foram localizados:
- O registro FCC e os relatórios de certificação do GT3 MAX.
- A versão de firmware da amostra regulatória: `NJY-N03-1.0.4`.
- O hardware da amostra: `M01_MB_V1.1`.
- A documentação Wearfit Pro sobre atualização OTA.
- Um log comunitário que revela o endpoint/fluxo genérico de atualização e o diretório de cache OTA.

**Não foi localizado um arquivo oficial `.zip`, `.bin` ou imagem assinada publicamente que possa ser atribuído com segurança à unidade informada (`GT3 MAX Call`, firmware `V1.07`, build `SH0050-1_1.1.3_1.23_1.18_1.1`) nem à amostra regulatória `GT3 MAX / NJY-N03 / M01_MB_V1.1`.** Não baixar nem usar firmware de X8, GT4, DT78, FitPro ou outro modelo por semelhança de nome/UUID.

## 6. Como obter o pacote do relógio do usuário sem iniciar o flash

Primeiro, no Wearfit Pro, abrir Dispositivo → Firmware Update e registrar:
1. versão instalada mostrada pelo app;
2. versão oferecida pelo servidor, se houver;
3. texto da atualização e qualquer identificador de modelo.

Não pressionar o botão final **Update/Upgrade** para esta investigação. O exemplo comunitário mostra o app iniciando o download e logo depois o Nordic DFU; o fluxo pode passar do download para o flash automaticamente.

Para tentar obter a resposta do servidor sem iniciar a atualização, inspecionar os logs criados ao consultar a versão. Em Android, o local comunitariamente observado é:

`/storage/emulated/0/Android/data/com.wakeup.howear/files/log/`

Procurar ocorrências de `updateFirmware`, `fileUrl`, `nextVersion` ou `ota`. A presença e o conteúdo desses logs variam por versão do app.

Se o pacote já tiver sido descarregado anteriormente, verificar:

`/storage/emulated/0/Android/data/com.wakeup.howear/files/Download/ota/`

No computador com ADB, os diretórios podem ser copiados sem root, caso a versão do Android permita acesso pelo shell:

```bash
adb pull /sdcard/Android/data/com.wakeup.howear/files/log ./wearfit-pro-logs
adb pull /sdcard/Android/data/com.wakeup.howear/files/Download/ota ./wearfit-pro-ota
```

No Samsung Meus Arquivos, o mesmo caminho pode aparecer em Armazenamento interno → Android → data → com.wakeup.howear → files. O Android pode restringir a navegação direta a Android/data.

Antes de compartilhar logs, ocultar MAC, serial, identificadores de conta, cabeçalhos `token`/JWT, cookies, códigos de autenticação e qualquer URL assinada que contenha credenciais temporárias. O exemplo público do fórum expõe um token de sessão de outro usuário; esse token não deve ser reutilizado nem replicado. Não compartilhar credenciais da conta Wearfit Pro.

## 7. Análise exigida antes de qualquer atualização

Quando o pacote específico for obtido, a análise deverá:
1. calcular SHA-256 e tamanho do arquivo;
2. listar o conteúdo ZIP sem executar nem instalar nada;
3. validar integridade do ZIP e examinar `manifest.json`;
4. extrair metadados do init packet `.dat`, incluindo hardware/ID de dispositivo, versão, dependências e assinatura, quando presentes;
5. identificar os componentes `.bin` e seus tamanhos;
6. verificar se o pacote corresponde ao modelo e hardware observados no relógio;
7. comparar a versão oferecida com a instalada e documentar a origem do download.

Mesmo que a estrutura seja válida, isso não autoriza o flash. O procedimento de atualização continuará bloqueado até correspondência inequívoca do dispositivo, integridade/assinatura e plano de recuperação.

## 8. Decisão de segurança

- Não foi executada atualização do GT3 MAX.
- Nenhum comando foi enviado ao serviço DFU.
- Nenhum firmware de outro modelo foi reaproveitado.
- A unidade atual foi identificada pelo usuário como GT3 MAX Call, V1.07, build SH0050-1_1.1.3_1.23_1.18_1.1.
- O transporte Secure DFU está identificado; o pacote binário específico permanece **não obtido / não validado**.
- A relação entre a unidade atual e a amostra FCC NJY-N03/M01_MB_V1.1 permanece **não confirmada**.
