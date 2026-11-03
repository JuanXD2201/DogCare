# Capturas de la app DogCare

Fecha de generación: 2026-11-02.

**Qué son:** renders generados con Robolectric + Roborazzi (Pixel 8, API 34) a partir de los Composables reales de la app (`presentation/screens/*`) con repositorios de demostración y datos ficticios. **No son capturas de un dispositivo físico ni de un emulador.** El mapa no muestra teselas porque el render no tiene red. Estado de la evidencia: PRUEBA AUTOMATIZADA SUPERADA (render), PENDIENTE DE PRUEBA EN MÓVIL.

| Archivo | Pantalla |
|---|---|
| 01_login.png | Inicio de sesión / registro |
| 02_calculadora_edad.png | Calculadora de edad y registro de perro |
| 03_chat_ia.png | Chat con IA y tarjeta de urgencia |
| 04_veterinarias.png | Búsqueda de veterinarias |
| 05_tienda.png | Marketplace de alimentos caninos |
| 06_carrito.png | Carrito |
| 07_pago_simulado.png | Pago simulado |
| 08_chat_usuarios.png | Chat entre usuarios |
| 09_perfil_invitado.png | Perfil (vista invitado) |

## Cómo regenerarlas
Desde `DogCare`: `.\gradlew.bat testDebugUnitTest --tests "com.example.ScreenshotsDocTest" "-Proborazzi.test.record=true" --console=plain --max-workers=1`. Salida: `app/src/test/screenshots/doc/`.
