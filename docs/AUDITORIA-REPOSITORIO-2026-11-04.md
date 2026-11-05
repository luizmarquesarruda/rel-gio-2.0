# Auditoria geral do repositório — 04/11/2026

## Resultado executivo

Foi realizada revisão estática das configurações de build, manifests, workflow, documentação, camadas de dados, domínio, BLE/GATT, descoberta automática, parser NUS/AB, captura de notificações e testes. Correções foram aplicadas numa branch isolada, sem alterar branches-base, sem criar PR e sem merge.

A auditoria não equivale a validação final: o usuário proibiu disparar CI nesta etapa, o ambiente não dispõe de checkout local utilizável/Gradle Wrapper e não houve acesso físico ao Galaxy A06/GT3 MAX para validar o protocolo e o handoff.

## Correções implementadas nesta branch

### Persistência do inventário GATT

Antes, o decoder JSON tratava arrays ausentes como vazios e usava `optInt/optLong` com valores padrão. Um arquivo parcialmente corrompido podia ser considerado válido e substituído pela próxima captura.

Agora:
- campos obrigatórios, tipos numéricos e arrays são exigidos;
- schema futuro permanece identificado como incompatível, sem migração destrutiva;
- tamanho do JSON, cardinalidade, timestamps, contagens, UUIDs, IDs duplicados, HEX candidato e faixas de valores são validados;
- chamadas concorrentes de instâncias distintas são serializadas por um lock compartilhado;
- a gravação usa `apply()` para evitar I/O síncrono no callback BLE; o método confirma somente a atualização em memória/agendamento, não a durabilidade em disco;
- dados inválidos não são reinterpretados como biblioteca vazia.

### Biblioteca de descobertas

O `@Synchronized` anterior protegia somente a instância do objeto. Como diferentes Activities constroem instâncias separadas do armazenamento, operações concorrentes ainda podiam intercalar leitura-modificação-gravação.

Agora:
- o lock é compartilhado entre as instâncias dentro do processo;
- o tamanho do arquivo é conferido antes da alocação de leitura;
- a escrita continua usando `AtomicFile`; a leitura restaura o backup `.bak` após interrupção de escrita e limita o arquivo antes de alocar o buffer;
- a fila assíncrona agora preserva falhas de persistência até a barreira de exportação/encerramento; o flush não retorna sucesso se qualquer gravação do lote falhou;
- o codec valida schema, tipos de registro, IDs únicos, timestamps, sessão, campos condicionais e formato HEX;
- prazos incompatíveis com a duração declarada e timestamps antes do início são recusados;
- não se grava payload fora de uma sessão consentida ativa.

### Handoff com Wearfit Pro

O código já bloqueava scan/conexão/reconexão enquanto o handoff estava ativo. A interface, entretanto, ainda deixava o botão de busca habilitado. Agora o botão reflete a mesma política e fica desabilitado enquanto a sessão estiver liberada. A retomada continua explícita pelo botão de handoff.

### Health Connect

`StepsSnapshot` agora rejeita timestamp de recebimento anterior ao fim do intervalo consultado. A origem continua obrigatoriamente `HEALTH_CONNECT_AGGREGATE`; nenhum dado é atribuído ao relógio e nenhuma escrita Health Connect foi adicionada.

## Segurança preservada

Esta revisão não habilita:
- escrita em NUS RX ou características proprietárias;
- comandos de sincronização CTS, ajuste de hora, vibração, idioma, lock/unlock ou configuração;
- DFU/OTA, reset, factory restore ou calibração;
- publicação de dados de saúde no Health Connect;
- novos serviços, permissões ou tráfego de rede.

Leituras continuam limitadas ao plano GATT existente; inscrições usam NOTIFY/INDICATE e CCCD padrão conforme consentimento e política já implementados.

## Revisão estrutural e pendências

1. `MainActivity.kt` continua concentrando UI, permissões e ciclo BLE. A extração completa é dívida arquitetural real; não foi feita em massa porque uma reescrita ampla neste estágio aumentaria o risco de regressão nos callbacks Android/GATT.
2. O repositório não contém Gradle Wrapper. O workflow instala a versão do Gradle diretamente, mas a compilação local reprodutível por `./gradlew` ainda não está disponível. A inclusão deve ser feita com wrapper oficial correspondente ao Gradle adotado e validada antes de mudar o pipeline.
3. As operações GATT e a sincronização física de horário ainda dependem de callback/firmware e de teste real. A existência do encoder CTS não prova que o relógio aceite sincronização; a escrita segue suspensa.
4. O perfil NUS/AB e a bateria `0x91/0x80` continuam candidatos, não protocolos validados.
5. Health Connect fornece passos agregados de fontes autorizadas; não representa passos medidos especificamente pelo GT3 MAX.
6. O fluxo de notificação na variante companion permanece em pré-visualização local; encaminhamento ao relógio segue bloqueado até confirmação do protocolo.

## Verificação

- Branch-base de auditoria: `21e44dd5bace9801b7ced7183ff15b2ca730d588`.
- CI: **não disparada**, conforme orientação do usuário. Não houve rerun nem cancelamento.
- Testes unitários: arquivos de regressão adicionados/atualizados, mas **não executados nesta revisão**.
- Lint/build/APKs/auditoria de manifesto compilado: **não validados nesta revisão**.
- Galaxy A06 / Android 16 / GT3 MAX Call / Wearfit Pro: **teste físico não validado**.
- Merge: **não realizado**.

## Arquivos principais alterados

- `FeatureDiscoveryStore.kt` e novo `FeatureDiscoverySnapshotPolicy.kt`;
- `DiscoveryLibraryStore.kt`, `DiscoveryLibraryCodec.kt` e `DiscoveryLibraryModels.kt`;
- `StepsSnapshot.kt`;
- `BleExternalHandoffPolicy.kt` e `MainActivity.kt`;
- testes do inventário, codec/sessão, timestamp Health Connect e handoff;
- README, arquitetura, mapa de módulos, changelog e diário de bordo.


## Retenção e consentimento — esclarecimento

A biblioteca BLE guarda até 1.000 registros no arquivo privado. O consentimento cronometrado controla **quando HEX pode ser capturado**; ele não configura expiração automática do HEX já salvo. A UI não oferece exclusão individual. Esta limitação foi documentada no README para que “capturado apenas durante sessão” não seja interpretado como “apagado ao fim da sessão”.

A ponte de notificações opera com uma regra diferente: prévias são voláteis, limitadas a 30 e 15 minutos, e agora são eliminadas imediatamente quando a captura é desativada ou o acesso é revogado.


## Terceira rodada — invalidação de callbacks, retenção e notificação

- Chaves de operações READ/CCCD/WRITE com timeout ou interrompidas por Service Changed ficam invalidadas até uma nova instância GATT. A fila não repete essas mesmas chaves na sessão atual; a tentativa é liberada após reconectar.
- Callbacks de notificação só são processados se a conexão ainda for atual, conectada e não estiver sendo encerrada.
- A fila limitada registra avisos de overflow também para HEX de READ e valores decodificados, separando observação do recebimento da confirmação de que a gravação foi aceita.
- O catálogo de funções e as telas de privacidade/biblioteca foram alinhados com a política real de CTS e retenção de HEX.
- A captura de notificação é limpa quando desativada; limite 30/15 min mantido.
- O exportador máscara endereços Bluetooth e omite texto de notificações Android; o fingerprint apenas processa metadados/hash curto.


## Quarta rodada — corrida entre callback e pausa da captura de notificações

- Identificada a janela entre a checagem inicial no `NotificationListenerService` e a inserção em memória: pausar a captura poderia limpar a lista e, depois, uma callback já iniciada poderia inserir mais uma prévia.
- `NotificationCaptureStore.addIfAllowed` revalida o estado e a allow-list sob o mesmo lock que serializa `setEnabled(false)` e alterações de aplicativos permitidos.
- Remover um aplicativo da allow-list também purga prévias daquele pacote.
- Testes adicionados para a barreira final quando consentimento está ativo/inativo.


## Quarta rodada — confirmação de gravações assíncronas

- Corrigido o falso positivo de flush na biblioteca de descobertas: cada tarefa agora registra o resultado real de persistência e a barreira de flush retorna falso quando uma gravação do lote falha.
- A inserção de tarefas e a barreira usam o mesmo lock de fila para delimitar lotes sem misturar falhas de operações posteriores.
- Adicionados testes unitários para falha seguida de sucesso, consumo/reset da falha e lote sem falhas.
- Execução de CI e testes Android: **não validados** nesta rodada; CI não foi disparada.

## 05/11/2026 — Exploração passiva e segura do canal NUS TX

- A exploração manual aprofundada agora inclui somente a característica NUS TX exata (UUID padrão 6e400003...) quando o relógio anuncia NOTIFY/INDICATE e o descritor CCCD 0x2902.
- O canal NUS RX (6e400002...) continua fora do plano: não é lido, inscrito nem escrito. Propriedades WRITE anunciadas em TX também não geram operações de escrita.
- O caminho automático existente continua condicionado ao fingerprint GATT completo; a inclusão no plano aprofundado não relaxa essa validação. A exploração manual exige confirmação explícita.
- A tela de descobertas diferencia canal TX encontrado aguardando inscrição/notificação. A existência do UUID não é apresentada como sensor ou protocolo validado.
- Adicionados testes para TX exato, RX excluído e ausência de CCCD.
- Fundamentação: Android executa discovery/read/notify de forma assíncrona e entrega os resultados em callbacks; a inscrição depende da propriedade anunciada e do CCCD. Referência: documentação oficial BluetoothGatt e Bluetooth GATT.
- CI, testes Kotlin, build e validação física no GT3 MAX/Galaxy A06: **não validados**. Nenhum workflow foi disparado.
