# Estudo técnico do firmware — GT3 MAX Call

**Data:** 05/11/2026  
**Dispositivo:** GT3 MAX Call / nome BLE observado \`GT3 MAX\`  
**Telefone de referência:** Samsung Galaxy A06, Android 16  
**Objetivo:** separar funções anunciadas comercialmente, recursos expostos pelo GATT e recursos realmente demonstrados por tráfego/valores.

## 1. Conclusão executiva

Ainda não existe evidência suficiente para uma análise estática do firmware. A versão \`V1.07\` e o identificador \`SH0050-1_1.1.3_1.23_1.18_1.1\` foram relatados a partir da interface/informações do dispositivo; não foram obtidos de uma característica Bluetooth Device Information nem de um arquivo binário.

Não foi fornecido um pacote \`.bin\`, \`.fw\`, \`.zip\` ou imagem equivalente. Portanto, não é possível extrair código, strings, tabela de comandos, mapa de memória, chipset ou versão interna a partir do firmware.

O material confiável disponível para orientar o aplicativo é o inventário GATT observado em 03/11/2026 e a notificação NUS recebida nessa sessão. A principal oportunidade agora é melhorar a coleta e a comparação de evidências, não enviar comandos desconhecidos.

## 2. Evidências atuais do dispositivo

A captura GATT registrada no projeto encontrou seis serviços:

| Serviço observado | Evidência | Interpretação segura |
|---|---|---|
| \`00003802...\` | Característica \`00004a02...\` com READ, NOTIFY e WRITE | Canal customizado; finalidade desconhecida |
| \`cc353442...\` | Características \`c551c36a...\` e \`00008a8f...\` com READ/NOTIFY/WRITE | Canais customizados; sem semântica confirmada |
| Generic Access \`0x1800\` | Device Name e Appearance | Metadados genéricos do dispositivo |
| \`00003803...\` | Característica \`00004a03...\` com READ/NOTIFY/WRITE/WRITE WITHOUT RESPONSE | Canal customizado de escrita; não enviar dados |
| Nordic UART \`6e400001...\` | TX com NOTIFY; RX com WRITE WITHOUT RESPONSE | Transporte de notificação confirmado; RX bloqueado |
| Secure DFU \`0xFE59\` | Serviço e características de DFU presentes no inventário | Presença anunciada, não autorização para atualizar firmware |

O inventário completo, propriedades e UUIDs está em [Captura BLE de 03/11/2026](CAPTURA-BLE-GT3-MAX-2026-11-03.md).

### Dados observados

- A conexão GATT e a descoberta de serviços foram concluídas com sucesso naquela sessão.
- Uma notificação de oito bytes chegou pela NUS TX: \`AB-00-05-FF-91-80-00-0C\`.
- A estrutura é compatível com uma hipótese de quadro AB já documentada, mas o valor final \`0C\` não foi correlacionado de forma repetível com a bateria mostrada no relógio/Wearfit Pro.
- Leituras de algumas características customizadas retornaram zero bytes. Isso não prova que sejam inúteis: a semântica pode depender de notificação, estado interno ou interação iniciada por um aplicativo autorizado.
- Nenhum valor de passos, sono, frequência cardíaca, SpO₂, pressão arterial ou temperatura foi confirmado nessa captura.

## 3. Recursos padrão Bluetooth que o app agora consegue classificar

O novo módulo \`GattCapabilityMatrix\` avalia onze grupos de perfis/capacidades Bluetooth SIG. Ele não inicia leituras nem inscrições: descreve apenas o que o inventário atual anuncia.

| Recurso | UUIDs principais | Resultado na captura disponível |
|---|---|---|
| Bateria | \`180F / 2A19\` | Não anunciado via GATT |
| Fabricante/modelo/firmware/hardware/software | \`180A\` e \`2A29/2A24/2A26/2A27/2A28\` | Serviço não anunciado |
| Hora padronizada | \`1805 / 2A2B\` | Não anunciado; sem base para sincronização CTS |
| Frequência cardíaca | \`180D / 2A37\` | Não anunciado |
| Pressão arterial GATT | \`1810 / 2A35/2A36/2A49\` | Não anunciado |
| Oximetria | \`1822 / 2A5E/2A5F/2A60\` | Não anunciado |
| Temperatura corporal | \`1809 / 2A1C/2A1E\` | Não anunciado |
| Ambiente | \`181A / 2A6D/2A6E/2A6F\` | Não anunciado |
| Composição corporal | \`181B / 2A9B/2A9C\` | Não anunciado |
| Corrida/cadência | \`1814 / 2A53/2A54\` | Não anunciado |
| Ciclismo/cadência | \`1816 / 2A5B/2A5C\` | Não anunciado |

**Como interpretar:** “não anunciado via GATT” significa apenas que o serviço padrão não apareceu no inventário capturado. O firmware pode usar canais proprietários. A ausência do UUID não prova ausência física de sensor ou da função no menu do relógio.

A matriz também diferencia serviço sem característica esperada, característica READ, NOTIFY/INDICATE com CCCD e tráfego observado. Uma propriedade GATT não confirma que o firmware entregue medições válidas.

## 4. O que podemos aproveitar agora

### Implementável com a evidência já disponível

1. **Descoberta e inventário GATT:** registrar serviços, características, propriedades e descritores de cada conexão.
2. **Assinatura estrutural:** comparar a estrutura GATT entre sessões para detectar alterações. O SHA-256 usa somente UUIDs/propriedades/descritores.
3. **Observação passiva NUS TX:** capturar notificações permitidas, contabilizar frequência/tamanho e preservar amostras HEX somente em sessão consentida.
4. **Framing AB candidato:** continuar remontando quadros fragmentados e separando quadros concatenados, sem atribuir função a comandos desconhecidos.
5. **Biblioteca de evidências:** manter origem, horário, canal, tamanho, fingerprint e estado de validação.
6. **Decoders SIG:** aproveitar os decoders existentes somente quando o dispositivo anunciar os serviços/características correspondentes.

### Recursos ainda dependentes de protocolo ou teste físico

| Recurso desejado | Situação | Próxima evidência necessária |
|---|---|---|
| Bateria | Quadro AB candidato; não confirmado | Comparação simultânea com o indicador do relógio em vários níveis de carga |
| Passos e distância | Não observados diretamente | Captura antes/depois de caminhada e correlação com visor e Wearfit Pro |
| Frequência cardíaca | Não validada | Captura sincronizada durante uma medição iniciada no relógio |
| SpO₂ | Não validada | Captura durante medição sob demanda e correlação temporal |
| Pressão arterial | Não validada | Captura controlada; não tratar estimativa como valor médico |
| Sono | Não há formato identificado | Captura de sincronização após uma noite, com registros e timestamps correlacionados |
| Esportes | Não há eventos/estrutura identificados | Iniciar e terminar uma atividade no relógio e comparar tráfego |
| Alarmes/lembretes | Não há comando confirmado | Identificar API oficial ou protocolo documentado antes de qualquer escrita |
| Notificações | Captura Android existe na variante companion; transporte para o relógio não está provado | Identificar framing oficial e validar recebimento no visor |
| Chamadas/áudio | Canal Bluetooth Classic pode ser separado do BLE de dados | Identificação do perfil de chamadas e escopo Android, sem misturar com GATT |
| Mostradores/clima/música | Não provados no protocolo do exemplar | SDK oficial ou capturas repetíveis, sem experimentar escritas arbitrárias |

## 5. Assinatura estrutural entre versões

O módulo \`GattProfileSignature\` gera um SHA-256 determinístico usando:

- UUID dos serviços;
- UUID das características;
- propriedades READ/WRITE/NOTIFY/INDICATE;
- UUID dos descritores.

A ordem de serviços, características e propriedades não altera a assinatura. Uma diferença entre duas capturas é sinal para revisão, não prova de atualização de firmware: pode ser cache GATT, variação de conexão, outro dispositivo ou alteração do servidor GATT.

A assinatura **não** é hash do firmware, não autentica o relógio e não utiliza MAC, número de série, dados do fabricante ou payload de saúde.

## 6. Limites que permanecem bloqueados

- Não escrever em \`NUS RX\` nem em características customizadas \`0x4A02\`, \`0x4A03\` ou UUIDs desconhecidos.
- Não experimentar comandos de medição, sincronização proprietária, calibração, reset ou restauração.
- Não executar Secure DFU/OTA. A presença de \`0xFE59\` não prova compatibilidade de pacote, integridade da imagem ou recuperação de falha.
- Não assumir que GT3 MAX, GT3 MAX Call e dispositivos visualmente semelhantes usam a mesma placa, chipset ou protocolo.
- Não publicar métricas em Health Connect até validar semântica, unidade, timestamp, origem, duplicidade e comportamento físico.
- Não classificar SpO₂, pressão estimada ou ECG de smartwatch como diagnóstico médico.

## 7. Próximo protocolo de investigação física

1. Desconectar o Wearfit Pro manualmente e conectar somente pelo Relógio 2.0.
2. Exportar o relatório TXT com a captura do inventário GATT completo.
3. Guardar a assinatura estrutural exibida e repetir a captura em outra sessão para confirmar estabilidade.
4. Registrar uma ação de cada vez no relógio (por exemplo, abrir a tela de frequência cardíaca e iniciar a medição); usar o marcador temporal do laboratório para alinhar a ação e as notificações.
5. Repetir com passos/caminhada, SpO₂, sono e bateria somente quando houver uma ação ou dado comparável.
6. Comparar valor, unidade, horário e mudança do visor. Classificar cada hipótese como **formato observado**, **candidato**, **semântica validada** ou **validação física repetível**.
7. Só então criar decodificador específico e contrato Domain; nenhum comando de escrita proprietária entra no escopo sem especificação oficial e plano de reversão.

## 8. Estado da implementação

- Matriz de onze grupos SIG: **implementada no código; testes adicionados, não executados**.
- Assinatura estrutural GATT: **implementada no código; testes adicionados, não executados**.
- Integração da matriz na tela de descobertas e no motor de inventário: **implementada no código**.
- Identificação do chipset e análise estática do firmware binário: **não realizadas** — o arquivo do firmware não está disponível.
- Build, Lint, testes Kotlin, CI e teste físico no Galaxy A06/GT3 MAX: **não validados nesta rodada**.

A meta desta etapa é aumentar a qualidade das evidências. Não existe ainda base técnica para afirmar que o firmware permite controlar todos os recursos anunciados comercialmente.
