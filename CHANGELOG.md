# Changelog

## Não lançado — matriz GT3 MAX e alinhamento de módulos

- Catálogo de 33 funções fornecidas do Wearfit Pro organizado em matriz de compatibilidade, sem presumir suporte do GT3 MAX.
- Fila GATT e parser BLE candidato movidos para pacotes próprios, com testes correspondentes e sem alteração de comportamento/protocolo.
- Arquitetura, mapa de módulos, índice documental e caminho do artefato diagnostic no README alinhados.
- CI deste conjunto e teste físico: **aguardando validação**. A versão `0.4.0-alpha.1` não foi promovida por esta alteração.

## 0.4.0-alpha.1 — 04/11/2026

### Direção Saúde & Fitness
- Produto reposicionado como companheiro de Saúde e Fitness, com o relógio como fonte principal das medições e Conexão Saúde como camada Android de compartilhamento.
- Interface principal orientada a atividade diária, smartwatch e Conexão Saúde.
- Categoria da ficha Play Store preparada como Saúde e fitness; o manifesto não define `appCategory=fitness`, pois esse valor não existe entre as categorias Android válidas.
- Mantida a ponte de notificações como função de companion: leitura consentida de notificações selecionadas, sem responder ou alterar o app de origem.
- O Google lista a retransmissão de notificações para dispositivos vestíveis próprios como caso permitido para apps de Saúde e Fitness; a retransmissão física ao GT3 Maxx segue pendente de protocolo validado.
- Não foi adicionada permissão WRITE_STEPS: o driver atual ainda não fornece amostras de passos do GT3 Maxx comprovadas. Não publicar dados simulados nem dados agregados do telefone como se viessem do relógio.
- Health Connect permanece limitado à leitura agregada de passos; escrita e métricas adicionais dependem de amostras reais, consentimento por tipo e validação.
- Teste físico no Galaxy A06/GT3 Maxx e aceitação pelo Play Protect: **não validados**.


As alterações relevantes do Relógio 2.0 são registradas aqui. Segue SemVer: MAJOR.MINOR.PATCH; versões alpha/beta indicam desenvolvimento e não equivalem a release estável.

## 0.3.0-alpha.4 — 03/11/2026

### Segurança e privacidade de notificações
- Pré-visualizações permanecem somente em memória, com limite de 30 itens e expiração automática após 15 minutos baseada em relógio monotônico.
- Cache é limpo quando o NotificationListenerService desconecta ou é destruído.
- A tela informa, antes de conceder o acesso, quais dados podem ser lidos, como são filtrados e por quanto tempo permanecem na memória.
- A captura exige acesso especial autorizado pelo Android, ativação explícita e ao menos um aplicativo selecionado.
- Nenhuma permissão SMS/acessibilidade, transmissão de conteúdo ou escrita BLE foi adicionada.

### Testes e validação
- Teste unitário cobre expiração das prévias no limite de 15 minutos.
- Lint, testes e APK desta versão: aguardando CI após o commit.
- Play Protect no Galaxy A06: **não validado**; esta alteração reduz retenção e melhora transparência, mas não comprova a causa nem garante remoção do alerta.
- Teste físico com GT3 Maxx: **não validado**.

## 0.3.0-alpha.3 — 03/11/2026

### Corrigido e aprimorado
- Verificação explícita da permissão BLUETOOTH_CONNECT antes de consultar nome/endereço BLE; callbacks de scan tratam revogação de permissão e falha de estado do scanner.
- Callbacks GATT são entregues pelo Handler principal, evitando concorrência com a fila de operações.
- Novas operações BLE são bloqueadas durante a desconexão; falhas de conexão limpam estado e fila.
- Lista de aplicativos de notificações ganhou área rolável própria.
- CI separado em Lint e testes/build; relatório Lint é publicado e o APK debug pode ser preservado quando build/testes passam, mesmo se Lint falhar.

### Protocolo BLE — análise de evidência
- UUIDs NUS dcca9e e quadro recebido AB 00 05 FF 91 80 00 0C comparados com fixture pública do projeto fbiego/dt78.
- Perfil NUS_AB_FRAME_COMPATIBLE_CANDIDATE e parser offline implementados.
- Quadro 0x91/0x80 pode indicar bateria; 0x0C sugere 12%, ainda **não validado** por comparação simultânea com Wearfit Pro.
- Nenhum payload foi escrito no relógio; RX, Secure DFU/OTA, reset e comandos de configuração continuam bloqueados.

### Testes adicionados
- Parser AB: captura local, fixture pública 80%, cabeçalho/tamanho inválidos, comando desconhecido e quadro incompleto.

### Validação
- Lint, testes e APK desta versão: aguardando CI.
- Teste físico Galaxy A06/GT3 Maxx: **não validado**.
## 0.3.0-alpha.2 — 03/11/2026

### Corrigido
- Cancelamento do timeout do scan BLE ao parar a busca, conectar ou receber falha; callbacks antigos são ignorados.
- Fila serial para leituras GATT e escritas do descritor CCCD; inscrições só entram em estado ativo após confirmação do callback.
- Callbacks GATT de conexões antigas não alteram o estado da conexão atual.
- Health Connect distingue ausência de registros de um total válido igual a zero e não reabre o pedido de permissão a cada atualização periódica.
- Cache de notificações é atualizado quando o Android remove uma notificação e quando o usuário desmarca um aplicativo.

### Testes adicionados
- Ordenação, recusa imediata e reset da fila GATT.
- Remoção por chave/pacote e limite de 30 notificações.
- Diferença entre passos sem registros, zero e valores positivos.

### Validação
- CI, lint e testes desta versão: **não validados** até o workflow concluir.
- Teste físico no Galaxy A06 e no GT3 MAX: **não validado**.
- Nenhuma escrita proprietária foi enviada ao relógio; o protocolo BLE permanece somente leitura.

## 0.3.0-alpha.1 — 03/11/2026

### Incluído
- Serviço Android NotificationListenerService.
- Tela de acesso a notificações com autorização especial, controle de pausa e seleção por aplicativo.
- Suporte a prévias publicadas pelo Android, incluindo WhatsApp/WhatsApp Business quando instalados, autorizados e selecionados.
- Cache temporário limitado às 30 notificações mais recentes em memória; sem persistência, logs ou tráfego de rede.
- Testes unitários da normalização de texto.

### Limitações
- Recepção de notificações pelo app ainda aguarda CI e teste físico.
- Não há encaminhamento de notificações ao GT3 MAX: protocolo proprietário não validado.
- Conteúdo só é capturado se o Android o expuser ao listener; textos ocultos/redigidos não são contornados.
- Build debug alpha; não é release assinada de produção.

## 0.2.0-alpha.1 — 03/11/2026

### Incluído
- Documentação centralizada e mapa de módulos.
- Inventário de funções do Wearfit Pro, com prioridade, origem e estado de validação.
- Especificação da integração Android NotificationListenerService para WhatsApp e notificações de outros apps.
- Diário de bordo e política de versionamento.

### Corrigido nesta linha de desenvolvimento
- Comparação do retorno booleano da leitura de característica BLE.
- Assinatura do callback de permissões Android na implementação Health Connect.

### Ainda não validado
- CI e geração do APK após as correções.
- Teste físico das permissões Health Connect no Galaxy A06.
- Captura de notificações WhatsApp pelo serviço próprio.
- Entrega de notificações ao GT3 MAX via BLE.
- Leitura de passos diretamente do GT3 MAX e ajuste do horário do relógio.

## 0.1.0 — protótipo inicial

- Prova de conceito Android/Kotlin para descoberta BLE/GATT em modo de diagnóstico somente leitura.
- Inventário de serviços, características e notificações GATT.
- Integração inicial de leitura agregada de passos por Health Connect na PR #2.
- Exibição da hora local do telefone; sem ajuste do relógio físico.
