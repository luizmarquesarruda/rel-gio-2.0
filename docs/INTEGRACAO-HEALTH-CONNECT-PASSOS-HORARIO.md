# Integração com passos e horário

## Conexão Saúde / Health Connect

O aplicativo usa Health Connect (Conexão Saúde) como camada Android para ler a contagem agregada de passos do dia. Não usa as APIs antigas do Google Fit, cuja descontinuação está prevista para o fim de 2026.

- Permissão solicitada: `android.permission.health.READ_STEPS`.
- Acesso somente após consentimento explícito.
- Consulta o total agregado desde o início do dia no fuso horário local até o momento da leitura.
- Atualiza automaticamente a cada 60 segundos enquanto o aplicativo está aberto; ao sair da tela, interrompe as consultas periódicas.
- Não grava passos no Health Connect nesta fase.
- O total pode incluir dados do telefone e de aplicativos/dispositivos que publiquem passos no Health Connect.
- A presença dos passos do GT3 MAX depende de o Wearfit Pro ou outro componente publicar esses dados no Health Connect. Isso ainda não foi confirmado neste aparelho.

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
2. O Wearfit Pro ou a conexão direta BLE entrega as amostras ao Relógio 2.0.
3. O app valida tipo, unidade, timestamp, intervalo, origem e qualidade.
4. Após consentimento explícito por tipo, o app grava somente amostras reais no Conexão Saúde usando as permissões WRITE correspondentes.
5. Outros aplicativos compatíveis podem ler os registros conforme as permissões concedidas pelo usuário.

A documentação Google recomenda `insertRecords` para escrever registros no Health Connect. Para passos, o registro deve preservar intervalo e metadados do dispositivo; um identificador de registro do cliente pode evitar duplicação em sincronizações repetidas.

**Situação desta versão:** o Relógio 2.0 lê o total agregado de passos do Health Connect usando `READ_STEPS`. Não possui ainda um fluxo validado que extraia passos do GT3 Maxx e os grave com `WRITE_STEPS`. O parser BLE atual reconhece apenas um quadro de bateria como candidato; ele não fornece amostras de passos, frequência cardíaca, sono ou SpO₂. Não adicionar valores fictícios nem converter o total agregado do telefone em dados do relógio.

### Limite entre Health Connect e Google Health API

Health Connect é uma camada centrada no dispositivo, com controle local de permissões e armazenamento no aparelho. Ele não equivale a um upload automático para a nuvem da Conta Google. Para integração cloud/account-centric com o ecossistema Google, a documentação de migração do Google Fit aponta para a Google Health API; essa integração requer projeto, OAuth e análise separada. O Relógio 2.0 não declara essa integração como implementada.

### Próxima validação

- Confirmar no Wearfit Pro se passos e outras métricas são publicados no Health Connect.
- Se não forem, identificar SDK oficial/exportação ou concluir engenharia reversa BLE com evidência repetível e somente após validação de segurança.
- Implementar escrita por tipo somente depois de validar fonte e metadados.
- Solicitar apenas as permissões WRITE dos tipos efetivamente publicados.
- Verificar ausência de duplicação e comparação com a leitura exibida no app original.
