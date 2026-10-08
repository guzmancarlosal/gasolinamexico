#!/bin/bash
set -e

echo "🚀 Iniciando compilación de Bundle Release para Google Play..."

export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

KEYSTORE_PROP="keystore.properties"
KEYSTORE_FILE="gasolinaMexico.jks"

if [ ! -f "$KEYSTORE_FILE" ]; then
  echo "⚠️ Advertencia: No se encontró $KEYSTORE_FILE en la raíz del proyecto."
  if [ -f "$HOME/Downloads/gasolinaMexico.jks" ]; then
    echo "Copiando desde ~/Downloads..."
    cp "$HOME/Downloads/gasolinaMexico.jks" ./gasolinaMexico.jks
  fi
fi

./gradlew bundleRelease

echo "🎉 ¡Compilación finalizada con éxito!"
echo "Tu archivo AAB generado se encuentra en:"
ls -lh app/build/outputs/bundle/release/*.aab 2>/dev/null || echo "app/build/outputs/bundle/release/"
