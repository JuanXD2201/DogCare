# Registro de cambios — DogCare

Formato: fecha · cambio · motivo · archivos · pruebas · resultado. Este registro empieza el 2026-10-28. El historial anterior sigue en `REPORTE_CAMBIOS_DOGCARE.txt`.

## 2026-10-28

### Entorno de compilación
- **Cambio:** se generó el wrapper de Gradle 9.3.1, se instalaron `cmdline-tools` y `platforms;android-36.1`, y se crearon `local.properties` y `debug.keystore` (ambos ignorados por Git).
- **Motivo:** el proyecto no se podía compilar (sin `gradlew`, SDK 36.1 ausente).
- **Archivos:** `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `local.properties`, `debug.keystore`.
- **Pruebas:** `gradlew assembleDebug testDebugUnitTest` → `BUILD SUCCESSFUL in 22m 54s`.
- **Resultado:** compila, genera el APK debug y pasan los tests unitarios.

### Pago simulado
- **Cambio:** los textos del flujo de pago se rotularon como SIMULACIÓN (decisión del usuario: no hay pasarela real).
- **Motivo:** el código mostraba "Sandbox (Wompi/MercadoPago Test)" sin llamar a ningún proveedor.
- **Archivos:** `presentation/screens/payment/CheckoutScreen.kt`, `data/repository/PaymentGatewayRepository.kt`.
- **Pruebas:** compilación correcta; verificación visual en dispositivo pendiente.

### Detector de urgencias
- **Cambio:** normalización de tildes y mayúsculas, y lista ampliada de señales de urgencia. El test usa ahora el objeto real `EmergencyDetector`.
- **Motivo:** el test anterior copiaba la lista en lugar de probar la clase; la detección era sensible a tildes.
- **Archivos:** `domain/util/EmergencyDetector.kt`, `test/.../EmergencyDetectorTest.kt`.
- **Pruebas:** `testDebugUnitTest` correcto. En dispositivo, "¿Qué hago si mi perro comió chocolate?" muestra la tarjeta de urgencia con "Ver Veterinarias" y "Llamar 123".

### Marketplace multi-vendedor
- **Cambio:** cualquier usuario autenticado publica, edita y elimina sus propios productos (`sellerUid`, `sellerName`). Se eliminó el rol admin de la tienda y la siembra del catálogo inicial. Las categorías quedan fijas y sin escritura desde la app. El formulario guarda `imageUrl`, permite elegir etapa de vida y valida longitudes y valores negativos. Se añadió confirmación antes de eliminar.
- **Motivo:** decisión del usuario; además la siembra insertaba marcas reales con precios y stock inventados.
- **Archivos:** `firestore.rules`, `domain/model/Product.kt`, `data/repository/ProductRepository.kt`, `presentation/screens/store/StoreScreen.kt`, `presentation/screens/profile/ProfileScreen.kt`.
- **Pruebas:** reglas validadas con `firebase deploy --only firestore --dry-run` (compilan). Pruebas del emulador (`firestore.test.js`) escritas pero **no ejecutadas**. En dispositivo, la Tienda muestra el botón "+" y el estado vacío.
- **Pendiente:** las reglas nuevas **no están desplegadas**; en producción siguen las anteriores.

### Chat entre usuarios: bloqueo y reporte
- **Cambio:** bloquear y desbloquear usuarios, reportarlos (colección `reports`, lectura solo para admin), ocultar mensajes de bloqueados y rechazar en las reglas los mensajes de quien fue bloqueado. El envío de mensajes ahora captura errores (antes cerraba la app si las reglas lo rechazaban).
- **Motivo:** la documentación previa afirmaba esta función, pero no existía en código ni en reglas.
- **Archivos:** `firestore.rules`, `data/repository/UserChatRepository.kt`, `presentation/screens/userchat/UserChatScreen.kt`.
- **Pruebas:** compilación correcta. Pruebas en dispositivo con dos cuentas pendientes.
- **Nota:** las reglas deben revisarse antes de un uso masivo.

### Chat con IA
- **Cambio 1:** `gemini-2.5-flash` ya no está disponible para proyectos nuevos; se pasó a `gemini-3.8-flash` (constante `GEMINI_MODEL_NAME`).
- **Cambio 2:** el historial del chat se conserva al cambiar de pestaña (estado en `DogCareApp`, con clave por usuario).
- **Motivo:** en dispositivo la IA devolvía `models/gemini-2.5-flash is no longer available to new users`, y el historial se perdía al cambiar de pestaña.
- **Archivos:** `data/repository/AiAssistantRepository.kt`, `presentation/screens/aichat/AiChatScreen.kt`, `presentation/DogCareApp.kt`.
- **Configuración externa:** el token de depuración de App Check del dispositivo lo registró el usuario en la consola de Firebase. Sin ese paso la IA devuelve `Firebase App Check token is invalid`. Tras reinstalar la app el token cambia y hay que registrarlo de nuevo.
- **Pruebas:** respuesta real de la IA pendiente de verificar tras recompilar.

### Veterinarias
- **Observación:** en dispositivo, la búsqueda por ciudad obtiene clínicas reales de OpenStreetMap, pero es lenta: Overpass agota el tiempo de 6 s y la app usa Nominatim como respaldo.
- **Estado:** sin cambios de código.

### Inicio de sesión
- **Cambio:** el botón "Explorar en Modo Demostración" pasa a "Explorar como invitado (solo lectura)".
- **Archivos:** `presentation/screens/auth/AuthScreen.kt`.

### Reglas de Firestore (desplegadas)
- **Cambio:** reglas nuevas desplegadas en el proyecto Jarvis tras 23 de 23 pruebas en el emulador (`released rules firestore.rules to cloud.firestore`).
- **Pruebas:** `firestore.test.js` en el emulador local; compilación de reglas con `--dry-run`.
- **Rollback:** consola de Firebase → Firestore → Reglas → historial.

### Corrección de cierres de la app (`@DocumentId`)
- **Cambio:** se quitó `@DocumentId` de `Product`, `StoreCategory`, `Order`, `Pet`, `UserProfile`, `Conversation` y `ChatMessage`: esos documentos guardan su propio `id` y Firestore lanzaba `'id' was found from document…`, lo que cerraba la app al listar productos y dejaba sin cargar el rol y el directorio.
- **Verificado en dispositivo:** publicar, listar y borrar producto sin cierre.

### Tanda de correcciones por revisión estática
- Flujos de Firestore: `throw error` → registrar y emitir lista vacía (pedidos, perros, productos, chat).
- Registrar perro y editar perfil: captura de errores, validación de longitudes, avisos al usuario; formulario de perro sin valores de ejemplo y con edad y peso obligatorios; talla en español.
- Sincronización del perfil en `DogCareApp`; el perfil muestra el nombre desde Firestore y `updateUserProfile` actualiza también Firebase Auth.
- Carrito: se vacía al cerrar sesión; invitado no puede pagar; dirección y teléfono ya no vienen prellenados con datos ficticios; el pago pendiente vacía el carrito.
- Etiquetas de pedido "Rechazado" y "Error en el pago"; producto sin campo `active` se considera activo.
- Botón Atrás del sistema cierra las subpantallas (`BackHandler`).
- Chat: los errores al iniciar chat o bloquear se muestran.

### Chat con IA — estado al cierre de la sesión
- `AiAssistantRepository` reescrito: comprueba App Check, prueba varios modelos con 25 s de límite cada uno y devuelve mensajes según la causa (registros con etiqueta `DogCareAI`).
- **Verificado en dispositivo:** App Check correcto; `gemini-3.8-flash` acepta la solicitud pero **no responde en 25 s**; `gemini-2.5-flash-lite` y `gemini-2.0-flash` están retirados (el servidor sugiere `gemini-3.5-flash-lite`).
- **Pendiente:** probar `gemini-3.5-flash-lite` (añadirlo a `MODEL_CANDIDATES`, primero) y confirmar una respuesta real. Hasta entonces el chat con IA **no responde**.

### Verificado en dispositivo con el APK debug (Redmi, Android 15)
Inicio de sesión, calculadora de edad (2 años mediano → 24 años humanos), detector de urgencias con "Llamar 123", publicar/editar/borrar producto, carrito (subtotal + envío $8.500), pago simulado aprobado con comprobante, descuento de stock 5 → 4, historial de pedidos, registro de perro, sincronización de perfil, mapa con clínicas reales de OpenStreetMap (lento).

## 2026-11-02

### Verificación de compilación
- `gradlew assembleDebug testDebugUnitTest lintDebug` → `BUILD SUCCESSFUL in 19m 56s`; 41 pruebas unitarias, 0 fallos; lint 0 errores y 64 advertencias; `app-debug.apk` 29,2 MB.

### Chat con IA
- **Cambio:** `MODEL_CANDIDATES` = `gemini-3.5-flash-lite`, `gemini-3.8-flash` (se quitan los modelos retirados).
- **Archivo:** `data/repository/AiAssistantRepository.kt`.
- **Pruebas:** compilación correcta. Respuesta real **pendiente de verificar en teléfono**.

### Capturas de documentación sin emulador
- **Cambio:** Roborazzi 1.59.0 en las pruebas y `ScreenshotsDocTest` (9 renders de los Composables reales con datos ficticios). `ProductRepository`, `PetRepository`, `OrderRepository` y `UserChatRepository` pasan a `open` junto con sus flujos `observe*`.
- **Resultado:** 9 de 9 pruebas generadas en `app/src/test/screenshots/doc/`. Son renders JVM, no capturas de dispositivo.

### Dependencias
- **Cambio:** se eliminó Room (declarado sin ningún uso en `src/main`).

### Publicación pública
- **Cambio:** `app/google-services.json` y `firebase_applet_config.xml` fuera del repositorio (`.gitignore`); plantillas en `config-examples/`. También se excluyen `debug.keystore`, `local.properties`, `*.jks` y la guía PDF.

### Documentación
- Se reescribieron `arquitectura.md`, `PRUEBAS.md`, `SEGURIDAD.md`, `justificacion_pasarela.md` y `MATRIZ_CUMPLIMIENTO.md` para que coincidan con el código.
