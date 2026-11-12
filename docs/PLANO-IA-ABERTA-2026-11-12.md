# Plano de IA aberta — Relógio 2.0

Data: 12/11/2026.

## Decisão

Manter separado o motor local determinístico já criado e uma eventual camada generativa. A IA generativa será opcional, local, acionada pelo usuário e nunca fará parte do ciclo BLE/GATT.

## Fontes e licenças verificadas em 12/11/2026

- **Qwen Code** — agente de desenvolvimento aberto, licença Apache-2.0; suporta provedores locais como Ollama e vLLM. É ferramenta para o ambiente de programação, não um módulo de runtime do aplicativo: https://github.com/QwenLM/qwen-code
- **Qwen3-0.6B-GGUF** — pesos oficiais sob Apache-2.0; a variante Q8_0 publicada tem aproximadamente 639 MB e a ficha indica uso com llama.cpp. O tamanho do arquivo não representa a RAM total necessária durante inferência: https://huggingface.co/Qwen/Qwen3-0.6B-GGUF
- **llama.cpp** — runtime aberto com orientação oficial para Android/NDK e exemplo de app Android; a documentação recomenda iniciar com contexto moderado (por exemplo, 4096 tokens), pois contextos grandes podem elevar bastante a memória: https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md
- **LiteRT-LM** — runtime aberto Apache-2.0 com API Kotlin para Android e opções de aceleração; requer artefato de modelo compatível .litertlm, portanto não é intercambiável diretamente com GGUF: https://github.com/google-ai-edge/LiteRT-LM e https://developers.google.cn/edge/litert-lm/android

Licenças do runtime, do agente e dos pesos são independentes. Cada arquivo de modelo incorporado ou baixado deve ter licença, origem, versão e SHA-256 registrados. Não copiar pesos grandes para o Git do aplicativo.


## Aplicação no Relógio

O motor determinístico de tendências e o histórico versionado continuam como fonte de verdade. A IA poderá transformar um insight existente em explicação simples, mas não pode criar uma medição nem inferir que o GT3 MAX forneceu um valor que veio do Health Connect.

Regras obrigatórias:
- cada entrada conserva origem, data local, estado de validação e qualidade;
- agregados Health Connect permanecem HEALTH_CONNECT_AGGREGATE/UNVALIDATED;
- dados do GT3 MAX só entram como fisicamente validados após teste reproduzível;
- não usar payload BLE bruto, MAC ou identificadores do relógio como prompt;
- não executar durante conexão, fila GATT, coleta ou em segundo plano;
- não enviar dados de saúde à rede, nem usar LLM para diagnóstico clínico, recomendação médica ou alarme de risco.

## Piloto proposto

- Primeiro protótipo: Qwen3-0.6B-GGUF Q8_0 local com llama.cpp, condicionado à medição no Galaxy A06.
- Alternativa: LiteRT-LM Kotlin se houver modelo .litertlm compatível e vantagem medida em memória/latência.
- Função inicial: resumir um insight determinístico selecionado pelo usuário, citando o período e a origem e incluindo a frase “não é avaliação médica”.
- Nenhuma ferramenta/autonomia do modelo: sem permissões, funções de rede, agenda, notificações ou comandos ao hardware.

## Critérios de entrada

Medir modelo carregado/descarregado, latência inicial e recorrente, tokens/s, RAM máxima, ANR, aquecimento, bateria e impacto no consumo do app quando BLE está desligado. Testar recusa de dados incompletos, datas/fusos/DST, fontes misturadas e prompt injection. O usuário deve poder desativar a IA e apagar o modelo sem apagar o histórico.

## Estado

A IA local determinística das Fases 1–3 não é um LLM e não depende dele. Esta pesquisa não adiciona runtime, peso, permissão ou chamada de rede. Modelo generativo e desempenho no Galaxy A06/GT3 MAX: **não validados**.


## Assistente aberto para desenvolver os três repositórios

Para programação, a recomendação é distinta do pequeno modelo a ser executado dentro do app:

- **Agente:** Qwen Code, licença Apache-2.0. Ele aceita provedores locais por API compatível com OpenAI; a documentação oficial fornece exemplo com Ollama e modelo Qwen2.5 7B.
- **Modelo de código:** Qwen2.5-Coder-7B-Instruct, licença Apache-2.0. A página oficial do modelo confirma a licença; o catálogo Ollama mostra pacote de aproximadamente 4,7 GB para qwen2.5-coder:7b.
- **Execução:** Ollama ou LM Studio local, sem enviar o conteúdo dos projetos para serviços de terceiros. Não entregar arquivos .env, tokens, secrets, dados financeiros, VINs, MACs ou dados pessoais ao agente. O agente deve trabalhar em branch isolada, rodar testes, produzir diff e aguardar revisão humana; não fazer merge ou deploy autônomo.

Fontes oficiais:
- Qwen Code — provedores locais: https://qwenlm.github.io/qwen-code-docs/pt-BR/users/configuration/model-providers/
- Qwen2.5-Coder-7B-Instruct — licença Apache-2.0: https://huggingface.co/Qwen/Qwen2.5-Coder-7B-Instruct
- Pacotes Qwen2.5-Coder via Ollama: https://registry.ollama.com/library/qwen2.5-coder

Não confundir esse modelo de programação para computador de desenvolvimento com o Qwen3-0.6B proposto para sumarização curta no Android. São cargas diferentes. O agente de programação não será incorporado aos aplicativos.
