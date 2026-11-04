# Investigação de atalho — firmware, hora/data e temperatura do GT3 MAX

**Data:** 04/11/2026  
**Dispositivo reportado:** GT3 MAX Call  
**Firmware informado na interface:** V1.07  
**Build informado:** SH0050-1_1.1.3_1.23_1.18_1.1  
**Estado da análise:** metadados disponíveis; binário específico não obtido; protocolo proprietário não validado.

## Conclusão executiva

Não há base técnica para corrigir hora, data ou temperatura alterando o firmware neste momento. O build é um rótulo exibido pela interface e não substitui o arquivo de firmware. Sem o pacote OTA específico, não é possível inspecionar strings, tabelas de comandos, módulos RTC, drivers de sensor ou estrutura de dados.

O atalho com menor risco é observar o tráfego do Wearfit Pro durante a sincronização normal e comparar com a sessão do Relógio 2.0. A captura deve ser passiva; não copiar comandos de outros modelos e não iniciar DFU.

## Evidências já disponíveis

- O GATT anuncia seis serviços e onze características na sessão de 04/11/2026.
- O serviço Bluetooth Current Time Service (0x1805) e a característica Current Time (0x2A2B) não aparecem.
- O serviço padrão Health Thermometer (0x1809) e a característica Temperature Measurement (0x2A1C) não aparecem no inventário.
- NUS está presente: serviço 6E400001, RX 6E400002 (WRITE_NO_RESPONSE) e TX 6E400003 (NOTIFY).
- O aplicativo recebeu três quadros AB/0x91/0x80 que sugerem bateria, mas a semântica ainda é candidata.
- O serviço FE59 corresponde ao transporte Nordic Secure DFU; ele não é um canal para exploração de sensores e permanece bloqueado.
- V1.07 e SH0050... são metadados informados pela tela/app, não valores lidos diretamente do GATT.

## Hora e data: hipótese e caminho de correção

Como CTS não foi anunciado, a escrita padronizada de hora não está disponível. A hipótese de maior valor é que Wearfit Pro envie uma mensagem proprietária pelo NUS RX ou por uma característica proprietária depois de abrir uma sessão e executar handshake.

Plano seguro:
1. No Wearfit Pro, conectar o relógio normalmente e observar a hora antes/depois.
2. Capturar logs do Android/Wearfit Pro no momento em que o app sincroniza a hora, sem pressionar atualizar firmware.
3. Se possível, capturar BLE/HCI snoop com autorização e sem registrar conteúdo de notificações pessoais.
4. Identificar UUID de destino, sequência de conexão, payload, timezone, formato de 12/24 h, checksum e confirmação do relógio.
5. Repetir com mudança controlada de minuto e data, preservando timezone e horário de verão como variáveis.
6. Implementar somente depois de duas ou mais capturas independentes consistentes e uma resposta/efeito verificável.

**Bloqueio atual:** não enviar nenhum payload ao NUS RX nem às características proprietárias. Uma escrita incorreta pode iniciar pareamento, alterar configuração, travar a sessão ou executar operação não prevista.

## Temperatura: separar três conceitos

1. **Temperatura ambiente/clima:** normalmente vem de serviço meteorológico do telefone ou do app; pode depender de localização, internet e atualização do clima. Não é necessariamente medida pelo relógio.
2. **Temperatura da pele/corporal:** exige identificar sensor, local de medição, unidade, calibração e algoritmo. Não equivale automaticamente à temperatura central do corpo.
3. **Temperatura do componente/bateria:** pode ser telemetria interna, não dado de saúde.

O próximo passo é observar a tela específica do relógio e do Wearfit Pro, identificar se há uma medição iniciada manualmente ou um cartão de clima, e comparar tráfego antes/durante/depois. Não publicar temperatura no Health Connect enquanto origem, unidade e significado não forem confirmados.

## Análise estática do firmware: material necessário

Para análise real, obter o pacote destinado exatamente ao GT3 MAX Call V1.07/build SH0050... a partir do cache/log do Wearfit Pro ou canal oficial do fabricante. Antes de abrir:
- preservar o arquivo original e registrar SHA-256, tamanho, origem e data;
- listar o ZIP sem executar conteúdo;
- validar integridade e manifesto;
- inspecionar init packet, IDs de hardware, dependências e assinatura;
- comparar metadados do pacote com a unidade física.

Não usar firmware de GT4, DT78, X8, FitPro ou outro relógio por semelhança de nome ou UUID. Não fazer flash, downgrade ou reset durante a investigação.

## Critérios para liberar implementação

- Identidade do pacote e do hardware correspondentes à unidade real.
- Protocolo de hora reproduzido em capturas independentes.
- Confirmação de timezone e resposta do relógio.
- Temperatura identificada como clima, pele/corporal ou componente.
- Testes negativos garantindo que quadros desconhecidos não sejam tratados como dados de saúde.

Até lá, a interface deve exibir hora/data como não sincronizadas e temperatura como indisponível/não identificada, sem inventar valores.
