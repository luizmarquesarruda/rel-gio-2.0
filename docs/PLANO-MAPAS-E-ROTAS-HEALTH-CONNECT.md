# Plano de evolução — mapas, caminhadas e sinais vitais

**Produto:** Relógio 2.0  
**Dispositivo-alvo:** GT3 MAX / GT3 Maxx, pareado atualmente pelo Wearfit Pro  
**Revisão:** 04/11/2026  
**Estado:** especificação técnica; recursos deste documento ainda não estão implementados.

## 1. Objetivo

Permitir que o usuário acompanhe caminhadas em mapa e consulte métricas de saúde com origem, unidade e horário confiáveis, aproveitando APIs Android/Health Connect e substituindo progressivamente o Wearfit Pro quando o protocolo do relógio estiver comprovado.

## 2. Dois caminhos de rota — não misturar as fontes

### Caminhada registrada pelo telefone

O aplicativo inicia uma sessão explícita, solicita apenas as permissões de localização necessárias e registra pontos GPS durante a atividade. Deve usar serviço Android em primeiro plano com notificação persistente enquanto a sessão estiver ativa, controle de pausa/retomada/finalização e política de bateria. O app não deve coletar localização continuamente fora de uma sessão iniciada pelo usuário.

Ao finalizar, a sessão e seus pontos podem ser gravados no Health Connect após permissões específicas para sessão de exercício e rota. A tela de mapa usa os pontos realmente registrados e mostra lacunas de GPS; não interpola trechos como se fossem medições reais.

### Rota importada do GT3 MAX

Só poderá ser exibida como rota do relógio se Wearfit Pro, SDK documentado ou protocolo BLE validado disponibilizar os pontos geográficos e timestamps. A captura GATT atual não contém evidência de coordenadas, sessão de exercício nem rota. Passos agregados não permitem reconstruir um trajeto.

### Integração no Health Connect

O acesso a rotas exige permissões próprias de exercício e rota; ler ou gravar pontos não é coberto por READ_STEPS. A leitura de rotas de outros apps exige interação deliberada do usuário na UI; não tratar acesso em background como garantido. Não adicionar permissões de rota ao manifesto até existir uma tela funcional e fluxo de consentimento.

## 3. Sinais vitais

O Health Connect oferece registros padronizados para frequência cardíaca, SpO₂ e pressão arterial, além de outros tipos. Cada tipo tem permissões próprias de leitura e gravação.

| Métrica | Tipo de dado Health Connect | Pré-condição |
|---|---|---|
| Frequência cardíaca | HeartRateRecord | Série e timestamps identificados; origem verdadeira preservada |
| SpO₂ | OxygenSaturationRecord | Valor e unidade confirmados; uso informativo, não diagnóstico |
| Pressão arterial | BloodPressureRecord | Sistólica/diastólica e contexto da medição; não aceitar estimativa opaca como equivalente a aparelho validado |
| Sono | SleepSessionRecord | Início/fim e, se disponíveis, fases com semântica confirmada |
| Passos | StepsRecord | Amostras ou agregado claramente identificado como fonte Health Connect |

A descrição comercial compartilhada cita sensor óptico central para frequência cardíaca e SpO₂. Isso não comprova que o firmware exponha leituras por BLE nem que o Wearfit Pro publique esses registros no Health Connect. O sensor, a precisão e o protocolo permanecem não validados.

## 4. Contrato obrigatório de dados

Toda amostra deverá preservar:
- Tipo, valor, unidade e semântica.
- Timestamp de medição vindo da fonte, quando disponível.
- Timestamp de recebimento pelo telefone.
- Origem (GT3 MAX, Wearfit Pro, Health Connect agregado, telefone ou entrada manual).
- Identidade local do dispositivo e sessão, sem publicar MAC/serial.
- Estado de validação e indicador de dado estimado.

Nunca:
- Converter ausência em zero.
- Atribuir agregado Health Connect ao GT3 MAX sem prova de origem.
- Publicar dados simulados, pacotes brutos ou números extraídos de hipótese no Health Connect.
- Apresentar SpO₂ ou pressão estimada como diagnóstico.
- Gravar amostras com horário de recebimento como se fosse timestamp medido pelo relógio.

## 5. Arquitetura proposta

- `device/ble`: scan, GATT, callbacks, fila, ciclo de conexão e transporte bruto.
- `device/gt3max/protocol`: UUIDs e framing candidatos, isolados do transporte.
- `domain/health`: métrica, unidade, qualidade, proveniência e validação.
- `data/healthconnect`: adaptadores de leitura/gravação com permissões específicas por tipo.
- `data/local`: histórico local versionado e deduplicação.
- `platform/location`: ciclo de vida GPS/foreground service para caminhadas iniciadas pelo app.
- `ui/activity` e `ui/maps`: sessão, mapa, resumo e histórico sem acoplar UI ao BLE.

A próxima extração de código deve retirar scan/GATT de MainActivity antes de acrescentar GPS, para não ampliar o acoplamento já existente.

## 6. Fases recomendadas

1. **Health Connect adapter:** extração da leitura de passos — código iniciado nesta branch; CI pendente.
2. **Histórico local:** persistência versionada, identificadores estáveis, prevenção de duplicação, exportação e testes de migração.
3. **Verificação de fontes:** identificar quais dados o Wearfit Pro já publica no Health Connect, com origem e timestamp reais.
4. **Rotas do telefone:** serviço em primeiro plano, permissões contextuais, GPS, gravação de sessão e tela de mapa.
5. **Rotas do relógio:** apenas após capturar e validar coordenadas/sessões do GT3 MAX em múltiplas atividades.
6. **Sinais vitais:** leitura de fontes existentes no Health Connect e depois driver BLE somente quando semântica e unidade forem comprovadas.
7. **Compartilhamento:** escrita Health Connect por tipo, somente para amostras válidas, com permissão e deduplicação.
8. **Cloud Google (opcional):** avaliar Google Health API separadamente caso seja necessária integração vinculada à Conta Google; não confundir com armazenamento local Health Connect.

## 7. Critérios de aceitação

- Caminhada inicia apenas após ação explícita do usuário e mostra estado GPS real.
- Pausa/interrupção não inventa pontos; falhas de GPS aparecem como lacunas.
- Mapa e histórico preservam data, duração, distância e fonte da rota.
- Dados de sensores incluem unidade, timestamp e origem rastreáveis.
- Permissões podem ser negadas/revogadas sem travar o app.
- Health Connect não recebe qualquer registro sem consentimento e validação por tipo.
- Testes unitários, Lint, CI, auditoria do APK e teste físico no Galaxy A06 concluem antes de promover o recurso.

## 8. Estado atual

- BLE/GATT e descoberta de serviços: observados na captura física.
- Perfil NUS/AB e possível pacote de bateria: candidato, não validado.
- Passos: leitura agregada Health Connect; não é fonte exclusiva do GT3 MAX.
- Frequência cardíaca, SpO₂, pressão, sono e rotas do GT3 MAX: não validados.
- GPS de caminhadas, mapa, histórico de rotas e gravação no Health Connect: não implementados.
- Nenhuma permissão adicional de localização/rotas/sinais vitais foi solicitada nesta fase.

## Referências oficiais de implementação

- Conexão Saúde — rotas de exercícios e permissões READ_EXERCISE_ROUTES / WRITE_EXERCISE_ROUTE: https://developer.android.com/health-and-fitness/health-connect/features/exercise-routes?hl=pt-br
- Conexão Saúde — sinais vitais e permissões por tipo: https://developer.android.com/health-and-fitness/health-connect/experiences/vitals?hl=pt-br
- Migração Google Fit: suporte às APIs Fit até o fim de 2026; Health Connect para apps móveis e Google Health API para integrações cloud/account-centric: https://developer.android.com/health-and-fitness/health-connect/migration/fit?hl=pt-br