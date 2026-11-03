# DogCare

Aplicación Android para el cuidado de perros: calculadora de edad canina, asistente de IA con orientación de urgencias, veterinarias reales cercanas, marketplace de alimento canino con carrito y pago simulado, y chat entre usuarios.

Proyecto del Taller II (Corte III) de **Desarrollo de Aplicaciones para Dispositivos Móviles — Optativa II**, Universidad de San Buenaventura.

**Autores:** Juan Esteban Olaya Muñoz · Nicolas Quiroz Gomez · Juan Sebastian Garzon Suarez

## Funciones

| Módulo | Qué hace |
|---|---|
| Autenticación y perfil | Registro, acceso con correo o Google, recuperación de contraseña, perfil |
| Perros y edad | Registro de perros y edad humana aproximada con recomendaciones por etapa |
| Asistente de IA | Gemini mediante Firebase AI Logic; tarjeta de urgencia con acceso a veterinarias y al 123 |
| Veterinarias | Clínicas reales de OpenStreetMap (Overpass, con Nominatim de respaldo), distancia e indicaciones |
| Marketplace | Alimento para perros (concentrado, húmedo, snacks); cada usuario publica sus productos |
| Carrito y pago | Totales, envío, pedidos y pago **simulado** (aprobado, rechazado, pendiente) |
| Chat de usuarios | Mensajes privados, bloqueo y reporte |

## Tecnologías

Kotlin 2.2.10 · Jetpack Compose · Material 3 · Firebase (Authentication, Firestore, AI Logic, App Check) · OSMDroid · OkHttp · JUnit, Robolectric y Roborazzi. Android 7.0 (API 24) o superior.

## Estructura

```
app/src/main/java/com/example/
  data/        repositorios y utilidades (Firestore, IA, mapas, pago simulado)
  domain/      modelos, cálculo de edad y detector de urgencias
  presentation/ pantallas y navegación (Compose)
  ui/theme/    paleta verde esmeralda y naranja
app/src/test/  pruebas unitarias y renders de pantallas
firestore.rules, firestore.test.js, firebase.json
config-examples/   plantillas de configuración de Firebase (sin secretos)
docs/              documentos técnicos del repositorio
DOCUMENTACION_DOGCARE/   SRS, arquitectura, diagramas, manuales, pruebas, presentación y matrices
```

## Configuración

La configuración de Firebase **no se versiona**. Para compilar:

1. Crear un proyecto Firebase y registrar la app `com.aistudio.dogcare.kprmdx`.
2. Copiar `google-services.json` a `app/` (plantilla en `config-examples/google-services.json.example`).
3. Crear `app/src/main/res/values/firebase_applet_config.xml` con `firestore_database_id` y `recaptcha_site_key` (plantilla en `config-examples/`).
4. Habilitar Authentication, Cloud Firestore y Firebase AI Logic; desplegar `firestore.rules`.
5. Crear `local.properties` con `sdk.dir`.

## Compilar y probar

```powershell
.\gradlew.bat assembleDebug --console=plain --max-workers=1
.\gradlew.bat testDebugUnitTest --console=plain --max-workers=1
.\gradlew.bat lintDebug --console=plain --max-workers=1
```

Renders de las pantallas para la documentación (no son capturas de dispositivo):

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.ScreenshotsDocTest" "-Proborazzi.test.record=true" --console=plain --max-workers=1
```

## Estado verificado (2026-11-02)

- Compilación correcta; APK debug de 28,8 MB.
- 50 pruebas automatizadas superadas (41 unitarias y 9 renders), 0 fallidas.
- Lint: 0 errores y 65 avisos.
- Reglas de Firestore: 23 de 23 casos en el emulador (sesión del 2026-10-28).
- Pendiente en teléfono: respuesta real de la IA y chat con dos cuentas.

## Limitaciones

- El pago es una simulación: no hay cobros reales ni validación de servidor sobre precios y estados.
- La IA no sustituye la valoración de un médico veterinario.
- Los teléfonos y la marca de urgencia de las clínicas dependen de las etiquetas de OpenStreetMap.

## Documentación

Ver `DOCUMENTACION_DOGCARE/` (SRS, arquitectura, diseño, pruebas, manuales, diagramas, presentación y matriz maestra).
