# Integração com passos e horário

## Conexão Saúde / Health Connect

O aplicativo usa Health Connect (Conexão Saúde) como camada Android para ler a contagem agregada de passos do dia. Não usa as APIs antigas do Google Fit, cuja descontinuação está prevista para o fim de 2026.

- Permissão solicitada: `android.permission.health.READ_STEPS`.
- Acesso somente após consentimento explícito.
- Consulta o total agregado desde o início do dia no fuso horário local até o momento da leitura.
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
