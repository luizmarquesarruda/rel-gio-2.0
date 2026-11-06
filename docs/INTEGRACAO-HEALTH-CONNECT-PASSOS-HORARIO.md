# Integração com passos e horário

## Conexão Saúde / Health Connect

O aplicativo usa Health Connect (Conexão Saúde) como camada Android local para ler a contagem agregada de passos do dia. Não usa as APIs antigas do Google Fit, cujo suporte está previsto até o fim de 2026. Health Connect não equivale a sincronização automática com a nuvem da Conta Google.

- Permissão solicitada: `android.permission.health.READ_STEPS`.
- Acesso somente após consentimento explícito.
- Consulta o total agregado desde o início do dia no fuso horário local até o momento da leitura.
- Atualiza automaticamente a cada 60 segundos enquanto o aplicativo está aberto; ao sair da tela, interrompe as consultas periódicas.
- Não grava passos no Health Connect nesta fase.
- A consulta está encapsulada em data/healthconnect/HealthConnectStepsRepository; domain/health/StepsSnapshot preserva intervalo, hora de recebimento e origem HEALTH_CONNECT_AGGREGATE.
- null (sem agregado disponível) permanece distinto de 0 (agregado real igual a zero).
- O total pode incluir dados do telefone e de aplicativos/dispositivos que publiquem passos no Health Connect.
- A presença de passos do GT3 MAX no Health Connect depende de o próprio Relógio 2.0 produzir amostras validadas e publicá-las com permissão específica. Isso ainda não está implementado.

## Horário

A tela mostra a data e hora local fornecidas pelo Android. O ajuste do relógio físico permanece pendente: o protocolo e o comando de horário do GT3 MAX não foram identificados. Nenhuma escrita BLE será tentada até existir documentação/captura suficiente e validação segura.

## Não validado

- Sincronização real dos passos originados especificamente no GT3 MAX.
- Sincronização do horário do relógio físico.
- Funcionamento no Galaxy A06 após instalação do APK.
- Build e testes do CI desta fase.


## Fluxo pretendido de sensores do smartwatch para o Google

O fluxo do produto é:

1. Os sensores físicos do smartwatch medem passos e, quando suportado, outras métricas.
2. A conexão BLE direta entrega as amostras ao Relógio 2.0.
3. O app valida tipo, unidade, timestamp, intervalo, origem e qualidade.
4. Após consentimento explícito por tipo, o app grava somente amostras reais no Conexão Saúde usando as permissões WRITE correspondentes.
5. Outros aplicativos compatíveis podem ler os registros conforme as permissões concedidas pelo usuário.

A documentação Google recomenda `insertRecords` para escrever registros no Health Connect. Para passos, o registro deve preservar intervalo e metadados do dispositivo; um identificador de registro do cliente pode evitar duplicação em sincronizações repetidas.

**Situação desta versão:** o Relógio 2.0 lê o total agregado de passos do Health Connect usando `READ_STEPS`. Não possui ainda um fluxo validado que extraia passos do GT3 Maxx e os grave com `WRITE_STEPS`. O parser BLE atual reconhece apenas um quadro de bateria como candidato; ele não fornece amostras de passos, frequência cardíaca, sono ou SpO₂. Não adicionar valores fictícios nem converter o total agregado do telefone em dados do relógio.

### Limite entre Health Connect e Google Health API

Health Connect é uma camada centrada no dispositivo, com controle local de permissões e armazenamento no aparelho. Ele não equivale a um upload automático para a nuvem da Conta Google. Para integração cloud/account-centric com o ecossistema Google, a documentação de migração do Google Fit aponta para a Google Health API; essa integração requer projeto, OAuth e análise separada. O Relógio 2.0 não declara essa integração como implementada.

### Próxima validação

- Validar no próprio relógio e nas capturas BLE se passos e outras métricas são realmente disponibilizados.
- Se não forem, identificar SDK oficial/exportação ou concluir engenharia reversa BLE com evidência repetível e somente após validação de segurança.
- Implementar escrita por tipo somente depois de validar fonte e metadados.
- Solicitar apenas as permissões WRITE dos tipos efetivamente publicados.
- Verificar ausência de duplicação e comparação com a leitura exibida no app original.

## Próxima evolução: rotas, mapa e sinais vitais

### Caminhadas e rotas

Há dois fluxos distintos que não devem ser confundidos:
1. **Registrar uma caminhada pelo telefone:** GPS do Android e serviço em primeiro plano durante a sessão, com consentimento de localização e controle claro de início/pausa/fim.
2. **Importar uma rota que já veio do relógio:** requer que o protocolo BLE do GT3 MAX disponibilize uma sessão com pontos de rota e timestamps. A captura BLE disponível hoje não demonstra esse suporte.

O Health Connect possui registros de sessão de exercício e rotas, mas o acesso à rota exige permissões próprias de rota além das permissões da sessão. A integração deve armazenar pontos, altitude e tempo de forma coerente, evitar amostrar localização fora de uma sessão ativa e permitir revogação/exclusão. Uma visualização no mapa só será ligada a dados de rota reais; não inferir caminhos a partir de contagem de passos ou localizações isoladas.

### Frequência cardíaca, SpO₂ e pressão

Health Connect oferece tipos de registro para frequência cardíaca, saturação de oxigênio e pressão arterial, cada um com permissões próprias. Isso não significa que o GT3 MAX exporte esses tipos, nem que Health Connect os tenha recebido do relógio.

- Primeiro verificar, no Health Connect, quais tipos têm registros e qual aplicativo/dispositivo os originou.
- Só então avaliar leitura pelo Relógio 2.0 com permissões específicas e proveniência preservada.
- Para dados BLE diretos, exigir payloads correlacionados em várias sessões com a tela do próprio relógio, unidades e timestamps confirmados.
- Nunca transformar a alegação de sensor óptico em medição recebida. Não apresentar pressão estimada como diagnóstico.

### Estado do adaptador de passos nesta fase

- Acesso ao provider e consulta de passos foram movidos para HealthConnectStepsRepository.
- O módulo UI não monta mais diretamente AggregateRequest nem conhece StepsRecord.
- Foram adicionados testes para zero, ausência, origem e intervalo.
- As demais métricas e rotas permanecem **não implementadas/não validadas**.

## Referências oficiais

- Migração do Google Fit: https://developer.android.com/health-and-fitness/health-connect/migration/fit?hl=pt-br
- Rotas de exercício e permissões: https://developer.android.com/health-and-fitness/health-connect/features/exercise-routes?hl=pt-br
- Sinais vitais e permissões por tipo: https://developer.android.com/health-and-fitness/health-connect/experiences/vitals?hl=pt-br