# Auditoria local de credenciais Git

Este procedimento é executado somente em clone local pelo mantenedor. Não consulta API remota,
branches remotas ou valores de segredos.

## Inspecionar nomes de arquivos sensíveis no histórico

~~~bash
git log --all --full-history --name-only --format= |
  grep -Ei '(^|/)([^/]*\.jks|[^/]*\.keystore|local\.properties|\.env([^/]*)?|google-services\.json)$' |
  sort -u
~~~

## Enumerar somente caminho e hash do blob — nunca conteúdo

~~~bash
git rev-list --all --objects |
  awk 'tolower($2) ~ /(^|\/)([^/]*\.jks|[^/]*\.keystore|local\.properties|\.env([^/]*)?|google-services\.json)$/ {print $1, $2}'
~~~

## Protocolo de remediação

1. Revogar/rotacionar imediatamente a credencial no serviço que a emitiu.
2. Delimitar escopo, permissões, período de exposição e consumidores afetados.
3. Preservar cópia local isolada, com acesso restrito, antes de qualquer reescrita.
4. Executar git filter-repo somente em clone local isolado; revisar branches, tags e refs.
5. Validar o histórico resultante e orientar a equipe a fazer novo clone.
6. Não reutilizar clones anteriores à reescrita.
7. Atualização do remoto exige planejamento e autorização explícita separada.

**Declaração desta mudança:** nenhuma ação remota de varredura, revogação, reescrita de histórico
ou atualização de refs existentes foi executada.
