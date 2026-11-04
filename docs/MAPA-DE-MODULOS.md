# Mapa de módulos

## Estado estrutural observado

A implementação atual é um protótipo Android nativo em Kotlin. BLE, Health Connect e interface ainda estão concentrados em `MainActivity.kt`. Isso acelera a prova de conceito, mas cria acoplamento e torna testes e manutenção mais difíceis.

A separação abaixo é a estrutura-alvo. Não declarar a refatoração como concluída até que os arquivos e testes tenham sido efetivamente movidos.

## Componentes efetivamente presentes nesta versão

A implementação ainda não foi separada em camadas completas, mas estes componentes Kotlin já existem e são testáveis isoladamente:

- `MainActivity.kt`: interface atual, permissões, scan e ciclo de vida GATT, Health Connect e apresentação de notificações BLE brutas.
- `SerialGattOperationQueue.kt`: serialização das leituras GATT e escritas do descritor CCCD.
- `WearfitAbFrameParser.kt`: perfil candidato NUS/AB e interpretação restrita do quadro de bateria 0x91/0x80.
- `StepsCountFormatter.kt`: distinção entre agregado ausente e zero passos.
- `notifications/NotificationCaptureService.kt`: listener Android.
- `notifications/NotificationCaptureStore.kt`: cache volátil e preferências de seleção.
- `notifications/NotificationCenterActivity.kt`: tela de consentimento, seleção de apps e prévias.

O parser AB não é ainda um driver GT3 Maxx: não reagrupa fragmentos, não processa comandos desconhecidos e não envia payloads.

## Estrutura-alvo

```text
app/src/main/java/com/luizmarquesarruda/relogio20/
├── app/
│   ├── MainActivity.kt
│   └── RelogioApplication.kt
├── ui/
│   ├── dashboard/
│   ├── device/
│   ├── health/
│   ├── notifications/
│   └── settings/
├── domain/
│   ├── model/
│   ├── repository/
│   └── usecase/
├── data/
│   ├── healthconnect/
│   ├── notifications/
│   └── local/
├── device/
│   └── ble/
│       ├── core/
│       ├── gatt/
│       └── gt3max/
├── platform/
│   ├── permissions/
│   ├── bluetooth/
│   └── clock/
└── common/
    ├── time/
    └── logging/
```

## Responsabilidades e contratos

| Módulo | Responsabilidade | Regra |
|---|---|---|
| UI | Estado visual, ações e acessibilidade | Não conhece UUIDs nem comandos BLE |
| Domain | Modelos, casos de uso e regras de qualidade | Não depende de Android ou Wearfit Pro |
| Device BLE Core | Scan, conexão GATT, descoberta e ciclo de vida | Não interpreta pacotes de saúde sem parser validado |
| Driver GT3 MAX | UUIDs e protocolo do modelo confirmado | Só implementa comandos comprovados e testados |
| Health Connect | Leitura agregada de métricas Android | Toda amostra carrega origem; não atribuir automaticamente ao relógio |
| Notifications | Recepção, filtragem e normalização de notificações Android | Só após acesso concedido pelo usuário |
| Local Data | Preferências, cache e histórico local | Minimização, retenção explícita e migração versionada |
| Platform | Permissões e recursos do Android | Permissão mínima e explicação visível ao usuário |

## Modelo comum de evento/métrica

Cada registro persistido deve ter, quando aplicável:
- identificador interno não sensível;
- tipo, valor e unidade;
- origem: relógio, Wearfit Pro, Health Connect, notificação Android ou cálculo local;
- instante do evento e instante de recebimento;
- identificador local do dispositivo ou pacote de origem, quando necessário;
- qualidade: confirmado, agregado, estimado, bruto ou não validado;
- versão do parser e do esquema.

Dados ausentes não são zero. Valores estimados não podem ser apresentados como medição clínica. Pacotes BLE brutos não devem ser convertidos em métricas por adivinhação.

## Ordem recomendada de refatoração

1. Corrigir e estabilizar o build das PRs BLE e Health Connect.
2. Extrair os modelos e formatadores puros para `domain`/ `common`.
3. Extrair BLE Core sem alterar o comportamento do protocolo.
4. Extrair Health Connect para `data/healthconnect`.
5. Criar serviço independente de notificações Android.
6. Só então implementar driver GT3 MAX, se houver especificação de protocolo suficiente.

Cada etapa deve ter testes e uma PR pequena. Não misturar refatoração estrutural com alteração de protocolo BLE.
