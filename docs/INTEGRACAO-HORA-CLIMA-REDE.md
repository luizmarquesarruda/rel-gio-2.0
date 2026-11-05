# Integração de hora e clima pela rede do celular

**Data:** 05/11/2026  
**Versão de desenvolvimento:** 0.4.0-alpha.4  
**Estado:** implementação Android adicionada; transmissão meteorológica BLE e sincronização física do GT3 MAXX ainda não validadas.

## Objetivo

O Relógio 2.0 passa a obter a hora do sistema Android e consultar condições meteorológicas pela conexão de dados móveis ou Wi-Fi do telefone, sem depender operacionalmente do WearFit Pro.

O telefone é a ponte de rede. O aplicativo não tenta obter hora de servidores diretamente: usa o relógio civil do Android, que pode estar sincronizado pela rede quando "Data e hora automáticas" estiver habilitado nas configurações do sistema.

## Arquitetura implementada

| Componente | Responsabilidade |
|---|---|
| \`TimeWeatherActivity\` | Tela de hora local, estado de data/hora automática, busca de cidade, previsão e atualização manual. |
| \`OpenMeteoWeatherRepository\` | Geocodificação e consulta HTTPS à API Open-Meteo; conversão de JSON para modelos internos. |
| \`WeatherPreferences\` | Cache privado da cidade escolhida e da última previsão válida. |
| \`WeatherSyncWorker\` | Atualização meteorológica em segundo plano, sem acessar BLE ou dados de saúde. |
| \`WeatherSyncScheduler\` | Trabalho periódico único, com rede disponível, a cada seis horas e janela flexível de uma hora. |
| \`WeatherModels\` | Modelos de localidade/previsão e descrições de códigos WMO. |
| \`MainActivity\` | Ao descobrir GATT, sincroniza a hora automaticamente uma única vez por sessão somente se CTS padrão 0x1805/0x2A2B existir e a característica anunciar WRITE. |

### Fronteiras de segurança

- O horário de referência é \`System.currentTimeMillis()\` / \`LocalDateTime.now()\` do Android; o app não altera a hora do telefone.
- A sincronização CTS usa somente o contrato Bluetooth SIG conhecido, depois de confirmar serviço, característica, conexão e propriedade WRITE.
- A captura conhecida do GT3 MAXX não anunciou CTS 0x1805/0x2A2B. Neste exemplar, o horário continua sendo exibido no telefone e **não há evidência de que o app consiga ajustar o RTC/visor do relógio**.
- Não foi implementado comando meteorológico para o GT3 MAXX. Nenhuma característica proprietária, NUS RX, Secure DFU/FE59, reset ou canal de escrita desconhecido recebe dados.
- O serviço meteorológico não recebe MAC, serial, identificador do relógio, dados de saúde, conteúdo de notificações ou coordenadas GPS atuais do telefone.

## Fonte de horário

O app lê o horário e fuso configurados no Android. A tela informa se as opções globais de data/hora automática e fuso automático estão ativas. A sincronização pelo operador depende da configuração e da disponibilidade da rede no sistema; o Relógio 2.0 não consegue obrigar a operadora a corrigir o relógio do Android.

Quando CTS Bluetooth SIG compatível for detectado, o app envia o horário local automaticamente uma vez naquela sessão GATT. Callback GATT confirma o resultado da operação de transporte, não a atualização física do mostrador. A unidade GT3 MAX Call observada não anuncia CTS; a sincronização do relógio físico permanece **não validada**.

## Fonte meteorológica

### Open-Meteo

A implementação utiliza dois endpoints HTTPS:

- Geocodificação: \`https://geocoding-api.open-meteo.com/v1/search\`.
- Previsão: \`https://api.open-meteo.com/v1/forecast\`.

A pesquisa manual envia o nome digitado da cidade e solicita até seis resultados em português. Depois que o usuário escolhe uma localidade, a consulta de previsão envia somente latitude/longitude dessa cidade selecionada.

O pedido de previsão inclui:

- Atual: temperatura, sensação térmica, umidade, precipitação, código WMO, velocidade/direção do vento e rajadas.
- Horário: probabilidade de precipitação na próxima hora.
- Diário: temperaturas mínima/máxima, probabilidade máxima de chuva, nascer e pôr do sol.
- Unidades: temperatura Celsius, vento km/h, precipitação em milímetros.
- Fuso: \`timezone=auto\`, retornado pelo serviço para a localidade consultada.

A previsão representa modelos meteorológicos para a região, não leitura instantânea de uma estação instalada no telefone ou relógio.

### Uso, licença e atribuição

O serviço gratuito Open-Meteo é destinado a uso não comercial e os dados são fornecidos sob CC BY 4.0. A tela exibe atribuição à Open-Meteo. Uma distribuição comercial ou mudança de finalidade exige revisar os termos vigentes e obter autorização/contrato adequado antes de manter esse endpoint.

Referências:
- API e variáveis: https://open-meteo.com/en/docs
- Geocodificação: https://open-meteo.com/en/docs/geocoding-api
- Termos, uso não comercial e atribuição: https://open-meteo.com/en/terms

## Atualização, cache e modo offline

- Ao abrir a tela, a hora local é atualizada visualmente a cada segundo.
- Ao abrir a tela, a previsão em cache é exibida imediatamente.
- Se o cache tiver mais de 30 minutos, a tela solicita uma atualização de rede.
- O usuário pode pesquisar outra cidade e atualizar manualmente.
- Depois que a tela é aberta, o app agenda um trabalho periódico único de seis horas com requisito de conectividade.
- WorkManager é deliberadamente inexato. Economia de bateria, Doze, ausência de rede e políticas do fabricante podem adiar a execução; não há promessa de atualização em horário exato.
- Se a rede falhar, a última previsão válida é preservada no armazenamento privado e permanece disponível para consulta, com o horário da última atualização.
- A consulta direta aberta pela tela executa na IO dispatcher; não bloqueia a thread principal.
- O cache não é compartilhado com Health Connect e não é transmitido ao smartwatch.

## Privacidade e permissões

A função declara \`INTERNET\` e \`ACCESS_NETWORK_STATE\`, permissões normais do Android. Não solicita diálogo de permissão em tempo de execução.

Para manter a coleta mínima:

- Não há permissão ACCESS_FINE_LOCATION ou ACCESS_COARSE_LOCATION para o clima.
- Não há localização em segundo plano.
- A cidade é escolhida manualmente pelo usuário.
- Não há conta, chave de API privada, analytics, publicidade ou identificador de dispositivo no pedido.
- A previsão em cache permanece no SharedPreferences privado do aplicativo.
- O guardrail de manifesto passa a permitir exatamente uma declaração INTERNET nas variantes diagnostic e companion e continua bloqueando as permissões sensíveis anteriores.
- O guardrail de dependências runtime permanece ativo. WorkManager foi adicionado para agendamento persistente.

A consulta de geocodificação envia o nome da cidade digitado ao provedor; a consulta de previsão envia coordenadas da cidade selecionada. Isso é necessário para prestar o serviço meteorológico e deve ser informado ao usuário.

## Integração futura com o smartwatch

A camada de clima está separada do transporte BLE. Para transmitir a previsão ao GT3 MAXX será necessário identificar, em documentação oficial ou capturas controladas e repetíveis, um contrato de mensagem de clima que pertença ao firmware desta unidade.

Critérios mínimos antes de liberar:

1. Identificar o canal e o framing de forma repetível em sessões independentes.
2. Correlacionar a cidade e valores da previsão exibida no aplicativo de referência com as mensagens observadas.
3. Confirmar encoding, escala, unidade, fuso, validade e limites do payload.
4. Implementar um adapter de dispositivo isolado, com teste offline e fila GATT serial.
5. Validar em teste físico reversível, sem usar canais de DFU, reset ou funções desconhecidas.

A presença de NUS/AB, por si só, não comprova que ele transporte clima. O projeto não deve enviar payload inventado ao NUS RX nem a características proprietárias.

## Validação

| Item | Estado |
|---|---|
| Modelos e rótulos WMO | Código e teste unitário adicionados; execução ainda não validada |
| Geocodificação e previsão | Implementação adicionada; chamada real pelo Galaxy A06 ainda não validada |
| Cache privado | Implementação adicionada; teste de recuperação em aparelho ainda não validado |
| Atualização periódica | WorkManager configurado; execução em segundo plano ainda não validada no Samsung |
| Manifesto / auditor de INTERNET | Regra atualizada para uma declaração única; CI ainda não executada nesta alteração |
| Exibição da hora do Android | Implementação adicionada; teste físico ainda não validado |
| Ajuste de hora do GT3 MAXX | Não validado; CTS ausente na captura |
| Envio de clima ao GT3 MAXX | Não implementado; protocolo proprietário ainda não identificado |
