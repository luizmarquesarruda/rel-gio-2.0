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
