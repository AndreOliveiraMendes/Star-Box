# StarSpeckCounter

StarSpeckCounter é um projeto Android simples, escrito em **Kotlin**, que demonstra uma estrutura básica de aplicativo Android utilizando Gradle Kotlin DSL (`build.gradle.kts`). O projeto inclui uma única Activity (`MainActivity.kt`) e está configurado para ser compilado via linha de comando usando o wrapper do Gradle.

---

## 📁 Estrutura do Projeto

```
.
├── app
│   ├── build.gradle.kts
│   └── src
│       └── main
│           ├── AndroidManifest.xml
│           └── java
│               └── com
│                   └── starspeck
│                       └── counter
│                           └── MainActivity.kt
├── build.gradle.kts
├── gradle
│   └── wrapper
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradle.properties
├── gradlew
└── settings.gradle.kts
```

---

## 🛠️ Requisitos

* **JDK 17+**
* **Android SDK** instalado (com platform-tools e build-tools)
* **Gradle Wrapper** já incluído no projeto

Caso esteja usando Termux ou Linux, garanta que os comandos `adb` e `sdkmanager` estejam configurados corretamente.

---

## 🚀 Como Compilar o APK

Compile o APK de debug usando o Gradle wrapper:

```bash
./gradlew assembleDebug
```

O arquivo resultante ficará disponível em:

```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Como Instalar no Dispositivo

Com um dispositivo conectado via USB ou ADB-over-WiFi:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O parâmetro `-r` permite reinstalar sem remover a versão anterior.

---

## 🧩 Sobre o Código

O projeto atualmente contém:

* **MainActivity.kt**: Activity principal.
* **AndroidManifest.xml**: declaração da Activity e permissões básicas.
* Build scripts baseados em **Gradle Kotlin DSL**.

---

## 📄 Licença

soon

