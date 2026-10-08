#!/bin/bash
set -e

echo "📸 Preparando captura de screenshots oficiales para App Store..."

mkdir -p screenshots

SIMULATOR_NAME="iPhone 17 Pro Max"
SIMULATOR_ID=$(xcrun simctl list devices available | grep "$SIMULATOR_NAME" | head -n 1 | sed -E 's/.*\(([A-F0-9-]+)\).*/\1/')

if [ -z "$SIMULATOR_ID" ]; then
  # Fallback a primer iPhone disponible
  SIMULATOR_ID=$(xcrun simctl list devices available | grep "iPhone" | head -n 1 | sed -E 's/.*\(([A-F0-9-]+)\).*/\1/')
fi

echo "Iniciando simulador: $SIMULATOR_ID..."
xcrun simctl boot "$SIMULATOR_ID" 2>/dev/null || true

# Buscar la app compilada
APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/App-*/Build/Products/Debug-iphonesimulator/App.app -maxdepth 0 2>/dev/null | head -n 1)

if [ -z "$APP_PATH" ]; then
  echo "Compilando app para el simulador..."
  cd ios/App && xcodebuild -workspace App.xcworkspace -scheme App -destination "id=$SIMULATOR_ID" build CODE_SIGNING_ALLOWED=NO
  cd ../..
  APP_PATH=$(find ~/Library/Developer/Xcode/DerivedData/App-*/Build/Products/Debug-iphonesimulator/App.app -maxdepth 0 2>/dev/null | head -n 1)
fi

echo "Instalando app en el simulador..."
xcrun simctl install "$SIMULATOR_ID" "$APP_PATH"

echo "Lanzando app..."
xcrun simctl launch "$SIMULATOR_ID" com.ttpCorp.carlosguzman.gasolinamexico || true

echo "Esperando 6 segundos para que cargue la interfaz..."
sleep 6

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
OUTPUT_FILE="screenshots/screenshot_appstore_${TIMESTAMP}.png"
xcrun simctl io "$SIMULATOR_ID" screenshot "$OUTPUT_FILE"

echo "🎉 Captura guardada con éxito en: $OUTPUT_FILE"
echo "Resolución de captura lista para subir a App Store Connect."
