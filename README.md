# StarSpeckCounter

StarSpeckCounter é um projeto Android simples escrito em Kotlin, usando Gradle Kotlin DSL (build.gradle.kts).
Ele demonstra uma estrutura básica de app Android e inclui um fluxo simples de build + deploy automático para dispositivos via ADB.

---

## 🏷️ Badges

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"/>
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white"/>
  <img src="https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"/>
  <img src="https://img.shields.io/badge/License-Soon-lightgrey?style=for-the-badge"/>
  <img src="https://img.shields.io/badge/Build-Debug-blue?style=for-the-badge"/>
</p>

---

## 📁 Estrutura do Projeto

```
.
├── app
│   ├── build.gradle.kts
│   └── src
│       └── main
│           ├── AndroidManifest.xml
│           └── java
│               └── com
│                   └── starspeck
│                       └── counter
│                           └── MainActivity.kt
├── deploy_debug.sh
├── build.gradle.kts
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
└── settings.gradle.kts
```

---

## 🛠️ Requisitos

JDK 17+

Android SDK instalado (com platform-tools e build-tools)

```bash
adb configurado no PATH
```

Permissão para executar scripts (chmod +x deploy_debug.sh)



---

## 🚀 Como Compilar o APK

Compile o APK de debug usando o Gradle Wrapper:

```bash
./gradlew assembleDebug
```

APK resultante:

```bash
app/build/outputs/apk/debug/app-debug.apk
```

O sufixo Debug vem da build variant padrão (debug).
Essa variante habilita logs, desabilita minificação e permite o uso do debugger.


---

## 🛠️ Como Depurar

Ao rodar o app em modo debug, você pode utilizar:

Android Studio → Run / Debug

```bash
adb logcat para visualizar logs:
```

```bash
adb logcat | grep starspeck
```

Attach debugger pelo Android Studio se o APK for debuggable (padrão na variante debug).



---

## ⚡ Script Automático de Deploy (deploy_debug.sh)

Este script compila e instala automaticamente o APK de debug no dispositivo.

## 📌 Conteúdo do script:

```bash
#!/bin/bash

set -e

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

echo "[1/3] Compilando APK..."
./gradlew assembleDebug || { echo "Falha ao compilar"; exit 1; }

echo "[2/3] Verificando dispositivo ADB..."
adb get-state 1>/dev/null 2>/dev/null || {
    echo "Nenhum dispositivo conectado!";
    exit 1;
}

echo "[3/3] Instalando APK..."
adb install -r "$APK_PATH" || {
    echo "Falha ao instalar o APK";
    exit 1;
}

echo "✔ Deploy concluído!"
```

## ▶️ Como usar

```bash
chmod +x deploy_debug.sh
./deploy_debug.sh
```

---

## 📱 Instalação Manual via ADB

Caso prefira instalar manualmente:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk

-r reinstala sem remover dados.
```

---

## 📄 Licença

soon

