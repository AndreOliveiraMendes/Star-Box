#!/bin/bash

set -e

echo "=== BUILD DEBUG APK ==="
./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "ERRO: Falha ao compilar o app."
    exit 1
fi
echo "✓ Build concluído"

echo "=== CHECANDO ADB ==="
adb start-server > /dev/null 2>&1

DEVICE_COUNT=$(adb devices | grep -w "device" | wc -l)

if [ "$DEVICE_COUNT" -lt 1 ]; then
    echo "ERRO: Nenhum dispositivo conectado!"
    adb devices
    exit 1
fi
echo "✓ Dispositivo detectado"

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "ERRO: APK não encontrado em $APK"
    exit 1
fi

echo "=== INSTALANDO APK ==="
adb install -r "$APK"
if [ $? -ne 0 ]; then
    echo "ERRO ao instalar APK!"
    exit 1
fi

echo "=== OK! APP INSTALADO COM SUCESSO ==="

