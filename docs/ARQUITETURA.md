# Arquitetura inicial

## Decisão técnica provisória

Aplicativo Android-first. A escolha final entre React Native com módulo nativo Android e Kotlin nativo será tomada após uma prova de conceito BLE no Galaxy A06. Não acoplar a interface ao protocolo do relógio.

## Camadas

- **UI:** painel, conexão, métricas, histórico e configurações.
- **Domain:** modelos normalizados, regras de qualidade, unidades e casos de uso.
- **Device integration:** interface independente do fabricante; implementações específicas por modelo/chipset.
- **Platform:** Bluetooth Low Energy, permissões Android, armazenamento e exportação.
- **Data:** repositórios locais, migrações versionadas e trilha de proveniência.

## Contrato de dados

Cada amostra deve registrar:
- tipo e valor;
- unidade;
- timestamp informado pelo relógio, quando disponível;
- timestamp de recebimento no telefone;
- origem (relógio, Wearfit Pro/importação ou cálculo local);
- identificador local do dispositivo;
- qualidade/estado de validação.

Não misturar valores calculados com medições do sensor. Não apresentar dado ausente como zero.

## Integração BLE

A descoberta inicial será somente leitura: scan, identificação de serviços, características, propriedades e notificações. Não escrever em características desconhecidas. O protocolo só será implementado após capturas repetíveis e comparação com o app oficial.

## Armazenamento

Começar com armazenamento local simples e versionado. Definir banco definitivo após prova de conceito e medição no aparelho. Exportação CSV/JSON deve preservar origem e timestamps.

## Testes de aceitação

- Permissões e Bluetooth desligado/ligado.
- Pareamento, desconexão, reconexão e relógio fora de alcance.
- Dados ausentes, atrasados, duplicados e fora de ordem.
- Comparação com Wearfit Pro em períodos equivalentes.
- Reinício do app e do telefone sem perda/corrupção do histórico.
- Nenhum comando BLE de escrita durante a fase de descoberta.
