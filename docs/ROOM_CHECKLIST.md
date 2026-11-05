# ROOM_CHECKLIST.md

## Estado do repositório

**Não há código Room, entidade, classe de banco, migration ou schema exportado nesta branch.**
Este documento é uma barreira de integração; não gera schema nem introduz Room.

## Pré-commit obrigatório quando Room for incorporado

- [ ] @Entity(tableName = ...) mantém o mesmo nome em todas as versões.
- [ ] @Database(version = N) não é reduzida, reutilizada ou alterada sem migration.
- [ ] Nenhuma classe Migration existente foi renomeada ou removida.
- [ ] room.schemaLocation aponta para caminho absoluto: $projectDir/schemas.
- [ ] Todos os arquivos app/schemas/*.json são versionados.
- [ ] Toda mudança de schema tem migration explícita e não destrutiva.
- [ ] MigrationTestHelper valida cada caminho de atualização suportado.
- [ ] Nenhum teste usa banco vazio como substituto de migration real.

## Configuração Gradle Kotlin DSL — aplicar somente após integrar Room

~~~kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
~~~

Execute bash scripts/check-room-integrity.sh antes do commit.
