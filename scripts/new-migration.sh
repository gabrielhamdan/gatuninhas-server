#!/usr/bin/env bash
#
# Gera um arquivo de migration do Flyway com versao baseada em timestamp
# (formato yyyyMMddHHmmss). Usar timestamp em vez de numero sequencial
# (V1, V2, V3...) evita conflito quando varios devs criam migrations em
# paralelo em branches diferentes - cada um gera um numero unico sozinho,
# sem precisar coordenar com o resto do time.
#
# Uso:
#   ./scripts/new-migration.sh "nome da tarefa"
#
# Exemplo:
#   ./scripts/new-migration.sh "cria tabela de gatos"
#   -> src/main/resources/db/migration/V20260821231500__cria_tabela_de_gatos.sql
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MIGRATIONS_DIR="$SCRIPT_DIR/../src/main/resources/db/migration"

if [ "$#" -eq 0 ]; then
    echo "Uso: $0 \"descricao da migration\""
    echo "Exemplo: $0 \"cria tabela de gatos\""
    exit 1
fi

DESCRIPTION="$*"

# slugify: minusculas, troca acentos comuns do PT-BR pelo equivalente sem
# acento. Substituicoes literais uma a uma (nao "[aeiou com acento]" em bloco)
# de proposito: em locale C/POSIX o sed trata bracket expressions como bytes
# individuais, e como os acentos UTF-8 compartilham o primeiro byte, eles se
# misturam. Substituicao literal funciona igual em qualquer locale.
SLUG=$(echo "$DESCRIPTION" | tr '[:upper:]' '[:lower:]')
SLUG=$(echo "$SLUG" | sed \
    -e 's/á/a/g' -e 's/à/a/g' -e 's/â/a/g' -e 's/ã/a/g' -e 's/ä/a/g' \
    -e 's/é/e/g' -e 's/è/e/g' -e 's/ê/e/g' -e 's/ë/e/g' \
    -e 's/í/i/g' -e 's/ì/i/g' -e 's/î/i/g' -e 's/ï/i/g' \
    -e 's/ó/o/g' -e 's/ò/o/g' -e 's/ô/o/g' -e 's/õ/o/g' -e 's/ö/o/g' \
    -e 's/ú/u/g' -e 's/ù/u/g' -e 's/û/u/g' -e 's/ü/u/g' \
    -e 's/ç/c/g' -e 's/ñ/n/g')
SLUG=$(echo "$SLUG" | sed -E 's/[^a-z0-9]+/_/g; s/^_+//; s/_+$//')

if [ -z "$SLUG" ]; then
    echo "Erro: a descricao resultou em um nome vazio depois de normalizada."
    exit 1
fi

VERSION=$(date +%Y%m%d%H%M%S)
FILENAME="V${VERSION}__${SLUG}.sql"
FILEPATH="$MIGRATIONS_DIR/$FILENAME"

mkdir -p "$MIGRATIONS_DIR"

if [ -e "$FILEPATH" ]; then
    echo "Erro: $FILEPATH ja existe. Tenta de novo em um segundo."
    exit 1
fi

cat > "$FILEPATH" << EOF
-- Migration: ${DESCRIPTION}
-- Criada em: $(date +"%Y-%m-%d %H:%M:%S")

EOF

echo "Migration criada: $FILEPATH"