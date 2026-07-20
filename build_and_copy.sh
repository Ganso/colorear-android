#!/bin/bash

# Abortar el script si algún comando falla
set -e

echo "Iniciando la compilación del APK..."

# Compilar la aplicación en modo debug
./gradlew assembleDebug

echo "Compilación completada con éxito."

# Obtener fecha y hora actual en formato YYYY-MM-DD_HH-MM-SS
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
APK_NAME="colorear_${TIMESTAMP}.apk"

# Copiar el APK generado a la raíz del proyecto
cp app/build/outputs/apk/debug/app-debug.apk "./${APK_NAME}"

echo "El APK se ha guardado en la raíz del proyecto como: ${APK_NAME}"
