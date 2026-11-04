# Navegação e layout — GT3 MAX

## Auditoria do estado anterior

A interface era construída diretamente em `MainActivity.kt` por meio de `LinearLayout`, `TextView` e `Button`. A mesma tela misturava:
- resumo de passos e consulta Health Connect;
- conexão, busca e diagnóstico BLE;
- ações de notificações e privacidade;
- inventário técnico GATT e log de sessão.

Isso tornava a tela inicial longa, misturava tarefas comuns e diagnóstico técnico e não oferecia uma organização para as funções do catálogo Wearfit Pro.

## Estrutura aplicada

A tela inicial continua responsável por conexão e diagnóstico, sem mover nem reescrever a máquina BLE. Foi acrescentada a entrada **Explorar funções do relógio**, que abre `FeatureHubActivity`.

A navegação secundária agrupa recursos em seis áreas:

| Área | Conteúdo |
|---|---|
| Saúde | FC, SpO₂, pressão, sono, estresse, METT, respiração e ciclo fisiológico |
| Atividades | reconhecimento automático, caminhadas/corridas, metas e histórico |
| Dispositivo | brilho, botão, bloqueio, idioma/horário, reinício, fábrica e OTA |
| Conectividade | notificações, chamadas, áudio, música e câmera |
| Ferramentas | alarmes, cronômetro, temporizador, encontrar telefone, cartão de visita, Quick collection, Alipay e cartão de acesso |
| Privacidade e dados | divulgação, permissões e origem/qualidade dos dados |

As áreas existentes de notificações e privacidade encaminham para as telas já presentes. Os demais cartões mostram descrição e estado, sem emitir comandos.

## Semântica dos estados

- **Tela existente**: existe uma tela no app para a função.
- **Integração direta pendente / não validado**: Wearfit pode exibir/sincronizar, mas o parser e a origem direta no Relógio 2.0 não foram validados.
- **Não implementado**: não há ação funcional no app.
- **Bloqueado por segurança**: sem comandos de OTA, factory reset, bloqueio remoto, pagamentos ou cartões de acesso.
- **Semântica pendente**: o nome traduzido não basta para inferir o comportamento.

## Decisões de UX e segurança

1. Não criar uma tela independente para cada item de catálogo; agrupar por tarefa reduz profundidade e duplicação.
2. Manter diagnóstico GATT separado do uso diário, sem remover a tela técnica existente.
3. Informar estado e limitação em cada recurso para não sugerir suporte inexistente.
4. Não transformar estimativas de pressão arterial ou SpO₂ em aconselhamento clínico.
5. METT permanece em MET; não há conversão arbitrária em calorias.
6. Dados de ciclo fisiológico são sensíveis e não serão coletados sem consentimento específico.
7. Ações destrutivas e integrações financeiras não recebem controles executáveis.
8. Nenhuma permissão Android adicional foi solicitada nesta alteração.

## Acessibilidade e responsividade

Os cartões usam texto explícito, alvos de toque amplos, foco e ordem vertical de leitura. A tela usa rolagem vertical e dimensionamento baseado em densidade.

**Não validado visualmente:** screenshots em Galaxy A06/emulador, TalkBack, escala de fonte ampliada, contraste real, navegação por teclado, modo paisagem e estados de tela pequena. A revisão de código não substitui ensaio de acessibilidade no aparelho.

## Dívida visual remanescente

- Migrar progressivamente as telas para componentes reutilizáveis e tokens de tema.
- Extrair UI de `MainActivity.kt` sem alterar o ciclo BLE.
- Criar telas funcionais somente quando a integração correspondente for implementada e validada.
- Adicionar previews/screenshot tests após disponibilizar ambiente Android instrumentado.
