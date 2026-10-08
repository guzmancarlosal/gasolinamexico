#!/bin/bash
set -e

VERSION_FILE="version.json"

if [ ! -f "$VERSION_FILE" ]; then
  echo '{"versionName":"2.8.5","versionCode":33}' > "$VERSION_FILE"
fi

CURRENT_NAME=$(grep '"versionName"' "$VERSION_FILE" | sed -E 's/.*: "([^"]+)".*/\1/')
CURRENT_CODE=$(grep '"versionCode"' "$VERSION_FILE" | sed -E 's/.*: ([0-9]+).*/\1/')

echo "Versión actual: $CURRENT_NAME (Build $CURRENT_CODE)"

NEW_NAME=$1
NEW_CODE=$2

if [ -z "$NEW_NAME" ]; then
  read -p "Ingresa la nueva versión comercial (deja vacío para mantener $CURRENT_NAME): " INPUT_NAME
  if [ -n "$INPUT_NAME" ]; then
    NEW_NAME="$INPUT_NAME"
  else
    NEW_NAME="$CURRENT_NAME"
  fi
fi

if [ -z "$NEW_CODE" ]; then
  DEFAULT_CODE=$((CURRENT_CODE + 1))
  read -p "Ingresa el nuevo build number (deja vacío para $DEFAULT_CODE): " INPUT_CODE
  if [ -n "$INPUT_CODE" ]; then
    NEW_CODE="$INPUT_CODE"
  else
    NEW_CODE="$DEFAULT_CODE"
  fi
fi

# Guardar en version.json
cat <<EOF > "$VERSION_FILE"
{
  "versionName": "$NEW_NAME",
  "versionCode": $NEW_CODE
}
EOF

echo "✅ version.json actualizado: $NEW_NAME (Build $NEW_CODE)"

# Actualizar iOS si existe el proyecto Xcode
IOS_PBXPROJ="ios/App/App.xcodeproj/project.pbxproj"
if [ -f "$IOS_PBXPROJ" ]; then
  sed -i '' "s/MARKETING_VERSION = [^;]*;/MARKETING_VERSION = $NEW_NAME;/g" "$IOS_PBXPROJ"
  sed -i '' "s/CURRENT_PROJECT_VERSION = [^;]*;/CURRENT_PROJECT_VERSION = $NEW_CODE;/g" "$IOS_PBXPROJ"
  echo "✅ iOS project.pbxproj sincronizado con la versión $NEW_NAME ($NEW_CODE)"
fi

echo "🚀 Versiones sincronizadas para Android e iOS exitosamente."
