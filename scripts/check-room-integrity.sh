#!/usr/bin/env bash
set -euo pipefail

ROOT="$(git rev-parse --show-toplevel)"
cd "$ROOT"

if ! grep -R --include='*.kt' -qE '@Entity|@Database|: RoomDatabase' app/src 2>/dev/null; then
  echo "INFO: Room ainda não está integrado; nenhuma migration/schema foi alterada."
  exit 0
fi

if [[ ! -d app/schemas ]]; then
  echo "ERRO: app/schemas ausente após integração Room." >&2
  exit 1
fi

if ! find app/schemas -maxdepth 1 -type f -name '*.json' -print -quit | grep -q .; then
  echo "ERRO: nenhum schema JSON encontrado em app/schemas." >&2
  exit 1
fi

CHANGED="$(git diff --cached --name-only)"
if grep -Eiq '(Entity|Database|Migration|build.gradle.kts)' <<< "$CHANGED"; then
  if ! grep -Eq '^app/schemas/.*\.json$' <<< "$CHANGED"; then
    echo "ERRO: alteração Room staged sem schema JSON." >&2
    exit 1
  fi
fi

echo "Room: schemas presentes. MigrationTestHelper ainda precisa rodar no projeto real."
