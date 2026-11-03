# Relógio 2.0

Aplicativo Android para acompanhar e organizar os dados de um smartwatch atualmente pareado pelo Wearfit Pro.

> **Estado inicial:** projeto em descoberta técnica. O modelo exato do relógio e o protocolo BLE ainda não foram identificados. Nenhum comando de escrita no dispositivo será implementado antes da validação do protocolo.

## Objetivos da primeira versão

- Identificar o relógio e seu estado de conexão.
- Investigar acesso autorizado a passos, frequência cardíaca, sono, atividades e bateria.
- Exibir claramente origem, horário e confiabilidade de cada dado.
- Manter histórico local e exportação dos dados.
- Não depender de acesso não documentado à conta ou aos servidores do Wearfit Pro.

## Estratégia de integração

1. Identificar modelo, fabricante e chipset do relógio.
2. Verificar se existe SDK oficial ou mecanismo documentado de integração com Wearfit Pro.
3. Inspecionar os serviços e características BLE em modo somente leitura.
4. Implementar um adaptador específico para o dispositivo confirmado.
5. Validar os dados comparando-os com o Wearfit Pro.

A declaração de privacidade do Wearfit Pro descreve compartilhamento autorizado de dados com aplicações de terceiros, mas não equivale a uma especificação pública de SDK/API. A integração direta por BLE também depende do modelo e do firmware.

## Segurança e privacidade

- Solicitar apenas permissões Android necessárias.
- Não coletar localização continuamente.
- Não transmitir dados de saúde para servidores externos por padrão.
- Não gravar características BLE desconhecidas.
- Não executar atualização de firmware, reset ou comandos de configuração.
- Armazenar dados localmente e permitir sua exclusão/exportação.

## Documentação

- [Plano técnico e arquitetura](docs/ARQUITETURA.md)
- [Pesquisa de integração Wearfit Pro](docs/PESQUISA-WEARFIT-PRO.md)
- [Identificação do relógio e testes](docs/IDENTIFICACAO-DO-RELOGIO.md)

## Próximo bloqueio

Precisamos do nome/modelo exibido em **Wearfit Pro → Dispositivo** (ou foto dessa tela e da seção “Sobre”). Sem isso, qualquer implementação específica de protocolo seria especulativa.
