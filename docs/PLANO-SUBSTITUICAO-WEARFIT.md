# Plano de substituição progressiva do Wearfit Pro

## Objetivo do produto

O Relógio 2.0 deve conectar diretamente ao GT3 Maxx, receber os dados que o firmware realmente disponibiliza, apresentar histórico próprio e compartilhar registros compatíveis com o ecossistema Google. O Wearfit Pro deixa de ser dependência de uso quando a comunicação e as funções essenciais forem reproduzidas e validadas; até lá, permanece como referência de comparação, não como fonte técnica presumida.

O GT3 Maxx observado é um dispositivo BLE/GATT genérico. Não há evidência de Wear OS, Wearable Data Layer ou Health Services. Não usar APIs específicas de Pixel Watch/Wear OS para tentar controlar esse dispositivo.

## Arquitetura-alvo

1. **UI** — conexão, estado, atividade, saúde, histórico, privacidade e diagnóstico.
2. **Domain** — modelos normalizados de amostras, unidades, qualidade, proveniência, sessões e regras de deduplicação.
3. **BLE transport** — scan, conexão, descoberta GATT, callbacks, fila serial, reconexão e ciclo de vida Android. Não interpreta métricas.
4. **Device protocol** — perfil específico do GT3 Maxx, framing, fragmentação, parser e correlação de comandos/respostas. Não contém código de interface.
5. **Storage** — persistência local versionada, com origem, timestamp do dispositivo/telefone e identificador de sessão. Não descartar dados brutos de evidência sem política explícita.
6. **Google integration** — adaptador Health Connect separado, com permissões por tipo e publicação idempotente apenas de amostras validadas.

A primeira extração arquitetural deve retirar BLE e Health Connect de `MainActivity.kt`, que hoje concentra UI, permissões, GATT e leitura agregada de passos. Fazer em mudanças pequenas, preservando comportamento e testes.

## Fases de execução

### Fase 0 — recuperar build e baseline
- Corrigir XML/manifesto, Lint, testes e build das variantes `diagnostic` e `companion`.
- Verificar manifesto mesclado das duas variantes e confirmar que somente companion contém Notification Listener.
- Registrar SHA e URL da execução CI. CI verde não substitui teste físico.

### Fase 1 — captura passiva repetível
- Registrar nome anunciado, RSSI, serviços, UUIDs, propriedades, descritores, MTU e eventos de conexão sem publicar endereço MAC.
- Inscrever-se apenas em notificações GATT permitidas, usando CCCD padrão e confirmação de callback.
- Capturar payloads brutos com horário monotônico e horário civil de recebimento, sem enviar payloads proprietários.
- Comparar sessões com o Wearfit Pro aberto e fechado, e registrar o efeito observável sem interceptar tráfego privado de terceiros.

### Fase 2 — correlação de métricas
- Para cada métrica candidata, variar uma condição controlada (por exemplo, valor de bateria visível) e comparar simultaneamente o valor no Wearfit Pro com o quadro recebido.
- Repetir em pelo menos duas sessões independentes e em mais de um valor.
- Documentar tamanho, cabeçalho, campos, endianness, checksum, fragmentação e timestamp quando houver evidência.
- Uma correspondência de UUID ou fixture pública não prova modelo, chipset nem semântica do payload.

### Fase 3 — parser isolado e testes
- Implementar parser puro, sem dependência Android/UI, apenas para campos sustentados por capturas repetíveis.
- Cobrir quadro truncado, inválido, desconhecido, duplicado, fragmentado, fora de ordem e valores fora de faixa.
- Manter quadro desconhecido como dado bruto; nunca convertê-lo em pressão, FC, passos, sono ou SpO₂ por aproximação.

### Fase 4 — sessão e persistência local
- Definir modelo comum: tipo, valor, unidade, timestamp da fonte, timestamp de recebimento, dispositivo lógico, sessão, qualidade e proveniência.
- Deduplicar por identidade estável da amostra quando existir; não deduplicar medições distintas apenas por terem o mesmo valor.
- Manter histórico local recuperável e exportável; dados ausentes não são zero.

### Fase 5 — Health Connect / Google
- Solicitar somente permissões para tipos que o app já consegue produzir e explicar ao usuário.
- Publicar registros reais com unidade, horário, origem/dispositivo e metadados corretos; usar escrita idempotente e tratar revogação.
- Não atribuir ao GT3 Maxx o agregado de passos lido do Health Connect: esse total pode incluir telefone e outros apps.
- Health Connect é integração local Android; não prometer upload automático para Conta Google ou sincronização universal no Google Fit/Google Health.

### Fase 6 — experiência e distribuição
- Painel diário, gráficos e histórico com rótulo de origem/estado de validação.
- Importar sessões/rotas de Health Connect quando autorizadas e disponíveis; não duplicar atividades.
- Notificações Android somente com consentimento e filtros; envio ao relógio só depois de identificar e validar o protocolo de mensagens.
- Play Store: categoria Saúde e fitness e declarações de saúde/privacidade coerentes com o produto real.

## Evidência atual

- Conexão BLE e descoberta GATT foram observadas no Galaxy A06 com o nome anunciado GT3 MAX.
- UUIDs NUS estão presentes e foi capturado o quadro `AB-00-05-FF-91-80-00-0C`.
- A semelhança com uma fixture pública DT78/X8 Ultra torna o formato AB/NUS um candidato, não identifica o GT3 Maxx como esse relógio.
- O byte final `0C` como bateria 12% continua não validado sem comparação simultânea com Wearfit Pro em diferentes níveis de carga.
- Leituras das características proprietárias observadas retornaram zero bytes. Isso não prova ausência de função.
- Nenhum parser de passos, frequência cardíaca, pressão arterial, sono ou SpO₂ foi validado.
- Notificações locais ainda não são encaminhadas fisicamente ao GT3 Maxx.

## Limites de segurança

- Não enviar dados por características desconhecidas, NUS RX ou serviços proprietários até existir contrato de protocolo comprovado e plano de teste reversível.
- Não executar DFU/OTA, reset, calibração ou restauração de fábrica.
- Não solicitar SMS, acessibilidade ou permissões de saúde sem recurso funcional, explicação e consentimento.
- Não usar dados simulados para preencher lacunas ou publicar no Health Connect.
- Qualquer comportamento não testado no GT3 Maxx/Galaxy A06 deve permanecer marcado como **não validado**.

## Critérios para considerar Wearfit Pro substituível

- Conexão/reconexão estável em sessões repetidas e após reiniciar o telefone/relógio.
- Pelo menos uma métrica prioritária identificada com correlação repetível e timestamp/origem confiáveis.
- Histórico local sem perda, corrupção ou duplicação.
- Publicação Health Connect validada com permissão concedida e revogada.
- Funções de notificação e sincronização só anunciadas como prontas após teste físico.
- Comparação funcional documentada com Wearfit Pro para cada recurso declarado suportado.
