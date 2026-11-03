# Instalación y Despliegue - DogCare Android App

## 1. Requisitos del Sistema
- **Android Studio:** Ladybug / Iguana o superior.
- **JDK:** OpenJDK 17.
- **Android SDK:** Compile SDK 35, Min SDK 24.
- **Gradle:** 8.x con soporte Kotlin DSL.

## 2. Comandos de Compilación y Pruebas
1. **Compilar el proyecto:**
   ```bash
   gradle assembleDebug
   ```
2. **Ejecutar pruebas unitarias:**
   ```bash
   gradle :app:testDebugUnitTest
   ```
3. **Desplegar Reglas de Seguridad en Firebase:**
   ```bash
   firebase deploy --only firestore:rules
   ```
