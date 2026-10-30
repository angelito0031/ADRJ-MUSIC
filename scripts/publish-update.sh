#!/bin/bash
# Uso: ./scripts/publish-update.sh <versionCode> <versionName> <mensaje> [force]
# Ejemplo: ./scripts/publish-update.sh 2 1.1.0 "Bug fixes" false

set -e

if [ ! -f .env ]; then
    echo "ERROR: falta .env. Copiá .env.example a .env y completalo."
    exit 1
fi
source .env

if [ -z "$SIGNING_KEY" ]; then
    echo "ERROR: SIGNING_KEY vacío en .env"
    exit 1
fi

VERSION_CODE=$1
VERSION_NAME=$2
MESSAGE=$3
FORCE=${4:-false}

if [ -z "$VERSION_CODE" ] || [ -z "$VERSION_NAME" ] || [ -z "$MESSAGE" ]; then
    echo "Uso: $0 <versionCode> <versionName> <mensaje> [force=true|false]"
    exit 1
fi

# Construir payload SIN signature
PAYLOAD=$(cat <<JSON
{"latestVersionCode":$VERSION_CODE,"latestVersionName":"$VERSION_NAME","forceUpdate":$FORCE,"minimumVersionCode":1,"message":"$MESSAGE","downloadUrl":"https://gitlab.com/$GITHUB_USER/$GITHUB_REPO/-/releases/v$VERSION_NAME/downloads/app-release.apk","publishedAt":"$(date -u +%Y-%m-%dT%H:%M:%SZ)"}
JSON
)

# Firmar con HMAC-SHA256
SIGNATURE=$(printf '%s' "$PAYLOAD" | openssl dgst -sha256 -hmac "$SIGNING_KEY" | awk '{print $NF}')

# Escribir JSON con signature usando python
mkdir -p force_update
python3 - <<PY
import json
data = json.loads('''$PAYLOAD''')
data['signature'] = '$SIGNATURE'
with open('force_update/force_update.json', 'w') as f:
    json.dump(data, f, indent=2)
print('✅ Firmado con HMAC-SHA256')
PY

echo ""
echo "=== force_update.json ==="
cat force_update/force_update.json

# Publicar en rama config
git fetch origin config 2>/dev/null || true
git checkout -B config
git add force_update/force_update.json
git commit -m "Update v$VERSION_NAME (force=$FORCE)" || echo "Sin cambios"
git push origin config --force
git checkout main

echo ""
echo "✅ Publicado en: https://gitlab.com/$GITHUB_USER/$GITHUB_REPO/-/raw/config/force_update/force_update.json"
