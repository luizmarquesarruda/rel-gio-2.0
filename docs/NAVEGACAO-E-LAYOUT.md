# Navegação e layout — GT3 MAX

## Auditoria do estado anterior

A interface era construída diretamente em `MainActivity.kt` por meio de `LinearLayout`, `TextView` e `Button`. A mesma tela misturava:
- resumo de passos e consulta Health Connect;
- conexão, busca e diagnóstico BLE;
- ações de notificações e privacidade;
- inventário técnico GATT e log de sessão.

Isso tornava a tela inicial longa, misturava tarefas comuns e diagnóstico técnico e não oferecia uma organização para as funções ainda não implementadas.

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

## Painel de conexão — 04/11/2026

A área de conexão foi reorganizada em um cartão de estado com hierarquia visual e ações explícitas:

- Indicador visual e título resumem o estado conhecido: desconectado, busca, conexão, GATT conectado, desconexão ou falha.
- Texto auxiliar explica o próximo passo sem confundir conexão Bluetooth/GATT com leitura validada de sensores.
- A ação principal passou a ser **Buscar relógio**; **Desconectar** fica desabilitada quando não há conexão em andamento ou estabelecida.
- Resultados de busca são apresentados em cartões tocáveis, com nome, intensidade do sinal e endereço parcialmente mascarado.
- A tela mantém os registros técnicos em área separada; o conteúdo do log continua restrito à sessão atual.
- A máquina BLE, permissões, tempo de busca, descoberta GATT, parser e comandos não foram alterados nesta melhoria.

**Não validado:** renderização em Galaxy A06, tamanhos de fonte ampliados, TalkBack, estados de rotação e testes físicos de conexão após a mudança visual. A mensagem “Bluetooth conectado” significa somente conexão GATT; não comprova leitura de métricas do GT3 MAX.

## Semântica dos estados

- **Tela existente**: existe uma tela no app para a função.
- **Integração direta pendente / não validado**: o relógio pode exibir recursos que ainda não foram observados por BLE; parser e origem direta não foram validados.
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


## Exportação do diagnóstico BLE — 04/11/2026

A ação **Exportar diagnóstico TXT** abre o seletor de documentos do Android e permite salvar localmente o registro da sessão. O relatório inclui ambiente, estado da conexão, MAC mascarado, eventos BLE/GATT e bytes recebidos em hexadecimal. A exportação não inclui texto de notificações do telefone e não transmite dados para servidores. Como payloads BLE podem carregar métricas de saúde, o arquivo deve ser compartilhado com cautela.

O relatório classifica evidências observadas e interpretações ainda não validadas. A exportação não transforma uma conexão GATT em prova de leitura correta de sensores.


## Revisão da página principal — correção da sessão BLE e rolagem — 04/11/2026

A inspeção do código da tela principal identificou dois problemas estruturais:

- O cartão de conexão ficava depois de várias seções de saúde e funções, reduzindo sua visibilidade inicial.
- Apenas o inventário GATT e o registro tinham áreas roláveis próprias; o restante da página não participava de uma rolagem vertical única, o que podia cortar controles em telas compactas.

Correções aplicadas:

- Cartão BLE, identificação local do relógio e exportação TXT foram movidos para logo após o relógio do telefone, antes do resumo de saúde.
- Toda a página passou a ficar dentro de um único ScrollView vertical. A rolagem interna do inventário foi removida.
- O registro mostra as oito linhas mais recentes para não dominar a página; o buffer da sessão e a exportação TXT continuam limitados aos 250 eventos já definidos.
- A ação de busca fica desabilitada durante a busca e enquanto existir uma sessão GATT ativa.
- startBleScan() agora bloqueia uma nova busca se a conexão GATT estiver ativa ou em encerramento, preservando o estado conectado.
- O callback de conexão registra o estado intermediário CONNECTING e a falha ao iniciar discoverServices() é exibida explicitamente.

**Não validado:** inspeção visual em MagicPath, screenshots, renderização real no Galaxy A06, TalkBack, escala de fonte ampliada e rotação. A análise de código não substitui esses ensaios. A revisão visual no MagicPath não pôde ser aberta nesta execução porque a cota de chamadas externas da conta estava esgotada.
## Laboratório e biblioteca interna — 04/11/2026

A revisão de navegação identificou que a página inicial misturava conexão, passos, ações de saúde, notificações, descoberta BLE, exploração manual, catálogo de funções, privacidade e log técnico em uma única coluna. A tela de autodescoberta também misturava inventário com o roteiro de sensores, sem hierarquia clara.

### Organização aplicada no código

- **Painel principal:** conexão GT3 MAX e estado BLE continuam no topo; atalhos são agrupados em “Laboratório e registros” e “Mais recursos”.
- **Testes e descobertas:** reúne o catálogo GATT, o roteiro de experimentos recolhível e a sessão temporizada de observação.
- **Biblioteca interna:** tela própria com pesquisa e filtros para descobertas, leituras/notificações e sessões.
- **Diagnóstico avançado:** exportação TXT e últimos eventos ficam recolhidos por padrão, sem remover o buffer diagnóstico.

### Sessão cronometrada

O usuário escolhe 1, 2, 5 ou 10 minutos e confirma explicitamente a captura local. A tela mostra tempo restante, progresso e quantidade de registros. O prazo da sessão é persistido; voltar à tela principal não reinicia o contador. O cronômetro não executa operações BLE por conta própria.

### Biblioteca e privacidade

A biblioteca persiste inventários GATT como descobertas. Leituras GATT e notificações com HEX são retidas somente durante uma sessão de teste ativa. O armazenamento usa arquivo privado e escrita atômica, schema versionado e limites de tamanho/quantidade. Conteúdo corrompido ou schema futuro não é sobrescrito automaticamente. A exportação continua sendo ação separada e manual.

**Limites:** inspeção do código confirma estrutura e comportamento planejado, mas não substitui screenshots e teste visual no Galaxy A06. O MagicPath não pôde ser acessado nesta rodada por cota externa esgotada; emulador/ADB não está disponível neste ambiente. TalkBack, escala de fonte ampliada, rotação e instalação física permanecem **não validados**.

- O inventário GATT completo também fica recolhido por padrão; UUIDs e propriedades aparecem somente quando o usuário abre “Inventário GATT”.

- Atalhos de laboratório e biblioteca foram posicionados logo após o cartão de conexão e a identificação local do GT3 MAX; o resumo do Health Connect passou para a seção própria “Saúde e integração”.

- Enquanto a captura cronometrada estiver ativa, o painel principal mostra um indicador compacto com tempo restante e quantidade de registros, permitindo acompanhar o prazo ao voltar para a tela de conexão/exploração manual.


## Navegação principal 1 + 6 — revisão de 10/11/2026

A navegação de uso diário possui **uma tela principal e seis áreas secundárias de primeiro nível**. Configurações é uma rota subordinada a Mais recursos e não conta como uma sétima área principal. Telas técnicas e informativas abertas a partir dessas áreas também são rotas subordinadas.

| Nível | Tela/área | Responsabilidade |
|---|---|---|
| Principal | Painel | Relógio do telefone, resumo e atalhos. Não presume conexão ativa nem transforma ausência em zero. |
| Secundária 1 | Conexão e dispositivo | Reutiliza `MainActivity`, preservando scan BLE, ciclo GATT, reconexão, inventário e diagnóstico existentes. |
| Secundária 2 | Saúde | Apresenta FC, SpO₂, pressão e temperatura como N/D até existir fonte e semântica validadas. |
| Secundária 3 | Atividade | Consulta passos agregados do Health Connect; distância, calorias e exercícios permanecem N/D sem fonte validada. |
| Secundária 4 | Sono | Duração e estágios permanecem N/D enquanto não houver integração comprovada. |
| Secundária 5 | Histórico | Consulta agregados diários de passos dos últimos sete dias. A biblioteca BLE permanece identificada como histórico técnico, não como histórico de saúde. |
| Secundária 6 | Mais recursos | Reúne descoberta GATT, biblioteca local, catálogo, ferramentas do telefone, hora/clima, privacidade e a rota subordinada Configurações. Notificações aparecem somente na variante companion. |

### Rotas subordinadas

- **Configurações:** acessível por Mais recursos; reúne formato de hora, unidade de distância e inclusão opcional de fontes externas do Health Connect. Não aparece como atalho independente no Painel.
- Laboratório BLE, biblioteca de descobertas, catálogo, ferramentas locais, hora/clima e privacidade são destinos internos de Mais recursos; não aumentam a contagem das seis áreas secundárias.

### Linguagem visual

- Fundo `#07111F`, superfícies `#0E1B2D`, superfícies elevadas `#13243A`, bordas `#28415F`.
- Texto principal `#F8FAFC`, secundário `#9FB4CF`, destaque `#7DB3FF`; sucesso `#4ADE80`, atenção `#FBBF24`, falha `#FB7185`.
- Cartões arredondados, hierarquia tipográfica curta, alvos de toque amplos e conteúdo em rolagem vertical.
- As novas telas compartilham o componente visual `ui/RelogioUi.kt`. A navegação é nativa Android/Kotlin; não foi introduzido React Native nem Jetpack Compose.

### Contrato de dados e configurações

- O painel indica “conexão não consultada”; somente a tela Conexão e dispositivo mostra o estado BLE real.
- Saúde e Sono usam N/D enquanto não houver amostras confiáveis. Pressão e SpO₂ não são apresentadas como diagnóstico médico.
- A consulta de passos e o histórico diário usam Health Connect. Por padrão, o filtro é restrito à origem do próprio Relógio 2.0; em Configurações, o usuário pode habilitar fontes externas, como telefone e outros aplicativos autorizados. Mesmo habilitado, o total é identificado como agregado, nunca como passos exclusivos do GT3 Maxx.
- A unidade km/mi é uma preferência de apresentação para métricas futuras; não converte nem inventa dados que ainda não existem.
- Preferências são persistidas em armazenamento privado do aplicativo e não enviam comandos BLE.

### Preservação funcional e validação

Nenhuma alteração intencional foi feita na máquina de estados BLE, scan, conexão GATT, callbacks, política de reconexão, fila de operações, captura, parser ou perfil de protocolo. `MainActivity` foi reposicionada conceitualmente como a tela secundária Conexão e dispositivo e recebeu ajustes visuais e leitura das preferências de fontes/horário.

**Não validado nesta alteração:** compilação, testes Kotlin/Python, Lint, execução de CI, renderização em Galaxy A06, TalkBack, escalas de fonte, rotação e teste físico com GT3 Maxx. A tela de conexão mantém seu código de transporte; a mudança de layout não comprova que o relógio esteja conectando nem que sensores estejam transmitindo métricas válidas.
