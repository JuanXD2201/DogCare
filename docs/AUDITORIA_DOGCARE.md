# Auditoría DogCare — Fase 1 (inspección inicial)

Fecha: 2026-10-28. Fuente de requisitos: `GUIA II OPTA CORTE III.pdf` (Taller II Corte III, Optativa II, Ing. Jairo Armando Salcedo Aranda), 12 puntos.

> Alcance de esta versión: inspección estática del código y del entorno. **Nada de lo listado como "existente" se ha ejecutado ni probado todavía.** Los documentos previos (`docs/MATRIZ_CUMPLIMIENTO.md`, `docs/AUDITORIA_INICIAL.md`) declaran todo "CUMPLIDO" sin evidencia de ejecución; no se toman como prueba.

## 1. Inventario técnico

| Aspecto | Hallazgo |
|---|---|
| Lenguaje / UI | Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00), Material 3 |
| AGP / Gradle | AGP 9.1.1; wrapper declara Gradle 9.3.1 |
| SDK | minSdk 24, targetSdk/compileSdk 36 (minor 1) |
| Paquete | `com.aistudio.dogcare.kprmdx` (namespace `com.example`) |
| Arquitectura | `data/repository` + `domain/{model,usecase,util}` + `presentation/screens` + `ui/theme`. No hay ViewModels: el estado vive en las pantallas/repositorios |
| Persistencia | Firestore (remoto). Room declarado en dependencias; uso no verificado |
| IA | `firebase-ai` (Gemini vía Firebase AI Logic) en `AiAssistantRepository.kt` |
| Mapas | OSMDroid + Nominatim + Overpass (OpenStreetMap), no Google Maps |
| Pagos | `PaymentGatewayRepository.kt` (66 líneas); `docs/justificacion_pasarela.md` existe |
| Tests | 10 archivos en `src/test`, 1 en `androidTest`; `firestore.test.js` para reglas |
| Git | **No es repositorio Git** (`.git` ausente) |
| APK existente | Ninguno encontrado |

## 2. Bloqueos de entorno (verificados)

| ID | Hallazgo | Impacto |
|---|---|---|
| E1 | No existe `gradlew`, `gradlew.bat` ni `gradle-wrapper.jar`; Gradle no está instalado ni en caché (`~/.gradle/wrapper/dists` vacío) | No se puede compilar ni ejecutar tests |
| E2 | El SDK local solo tiene `platforms/android-37.0`; el proyecto pide `compileSdk 36.1` | Falta instalar `platforms;android-36.1` |
| E3 | No hay `local.properties`; `ANDROID_HOME` vacío (SDK en `%LOCALAPPDATA%\Android\Sdk`) | Hay que definirlo |
| E4 | `adb` fuera del PATH (existe `platform-tools` en el SDK) | Pruebas en dispositivo requieren ruta explícita |
| E5 | `.env`, `debug.keystore` y `my-upload-key.jks` ausentes | Build debug y release no pueden firmar hasta crearlos |
| E6 | Equipo de 4 GB de RAM (referencia histórica) | Emulador + Gradle simultáneos son inviables; preferir dispositivo físico |

## 3. Firebase

| Ítem | Hallazgo |
|---|---|
| `project_id` en `google-services.json` | `gen-lang-client-0991918834` (número `196615165241`). **Confirmado** con `firebase projects:list` (CLI v15.34.1, sesión activa): es el proyecto "Jarvis", mismo ID y número |
| Base Firestore | No es `(default)`: `ai-studio-android-dogcare-10fa4e75-6cda-4ff9-a467-a58e8e73db8e` (`firebase.json`) |
| Paquete registrado | `com.aistudio.dogcare.kprmdx` coincide con `applicationId` |
| Consola (Auth, usuarios, reglas desplegadas, índices) | **NO VERIFICABLE** hasta tener sesión autorizada |

## 4. Seguridad — hallazgos de la lectura de `firestore.rules`

Solo se han leído las líneas 1–60 y la regla de `users`; el resto está pendiente.

| ID | Severidad | Hallazgo |
|---|---|---|
| S1 | Alta | `isAdmin()` consulta `users/{uid}.role`. El cliente no puede escribir `role: admin` (create y update lo bloquean), pero el rol solo existe en Firestore: no hay custom claims ni backend para asignarlo. Hay que definir y documentar el procedimiento (consola o Admin SDK) |
| S2 | Media | Cada `isAdmin()` suma lecturas por evaluación; aceptable a este volumen |
| S3 | Alta (CONFIRMADO) | `firestore.rules` completo leído. `isValidOrder` solo exige `totalCop >= 0` y tipos; no compara `totalCop`/`items` con `products.priceCop`. El dueño puede crear un pedido con `totalCop: 0` y pasarlo de `PENDING` a `APPROVED` con cualquier `paymentReference`. El descuento de stock sí queda atado al pedido (`getAfter`). Una validación real de precio y pago exige una capa de servidor (Cloud Function, requiere plan Blaze = posible costo) o declarar la pasarela como **simulación** y documentar la limitación |
| D1 | Decisión | **Pago simulado** (decisión del usuario, 2026-10-28): sin pasarela real ni Cloud Functions, sin costo. `PaymentGatewayRepository.processSandboxPayment` es una simulación local (no llama a Wompi/Mercado Pago). Textos de `CheckoutScreen.kt` y `PaymentGatewayRepository.kt` reetiquetados como "SIMULACIÓN". Limitación declarada: el cliente decide el estado del pago, por lo que S3 permanece como riesgo aceptado y documentado |
| S6 | Media (CONFIRMADO) | `docs/MATRIZ_CUMPLIMIENTO.md` afirma "bloqueo y reporte de usuarios", pero no hay código ni reglas de bloqueo/reporte (búsqueda de `block`/`report` en `app/src/main/java`: 0 archivos). Funcionalidad inexistente |
| S7 | Baja | `conversations` permite crear una conversación incluyendo cualquier UID; no hay mecanismo para rechazarla |
| S4 | Media | No hay colecciones `inventory` ni `orderItems` en las reglas (la guía exige Inventario y Detalles de pedido) |
| S5 | Por verificar | No se vieron claves en `.kt`. `google-services.json` y `firebase_applet_config.xml` llevan la API key de cliente (normal en Android); debe restringirse por paquete + SHA-1 en la consola |

## 5. Matriz de cumplimiento (puntos de la guía)

Estados: COMPLETO Y VERIFICADO · IMPLEMENTADO PARCIALMENTE · PENDIENTE · FALLIDO · BLOQUEADO POR ACCESO O CREDENCIALES · NO VERIFICABLE.

| ID | Requisito (guía) | Estado actual | Evidencia encontrada | Problema detectado | Solución necesaria | Prioridad | Archivo / módulo | Prueba de aceptación | Estado final |
|---|---|---|---|---|---|---|---|---|---|
| P1 | Conservar cálculo edad perro/humano; mejorar pantalla principal; registrar nombre, edad, características; recomendaciones por etapa; navegación | IMPLEMENTADO PARCIALMENTE | `CalculateDogAgeUseCase.kt`, `AgeCalculatorScreen.kt`, `DogCareApp.kt`, `DogAgeCalculatorTest.kt` | Sin ejecutar. Verificar que no use la regla fija de 7 años y que cubra límites | Revisar fórmula, ejecutar test, probar entradas inválidas | Alta | `domain/usecase`, `screens/agecalc` | Test de bordes + prueba en dispositivo | NO VERIFICABLE |
| P2 | Nombre comercial, logotipo original, icono Android, bienvenida, identidad visual | PENDIENTE de auditar | `ui/theme`, dependencia `core-splashscreen`; `res/mipmap` y `res/drawable` sin revisar | Icono, logo y splash no auditados | Inspeccionar `res/`, crear icono adaptativo y splash si faltan | Media | `res/`, `ui/theme` | Capturas de icono, splash y tema coherente | NO VERIFICABLE |
| P3 | Registro, login/logout, perfil, varios perros, protección | IMPLEMENTADO PARCIALMENTE | `AuthRepository.kt`, `AuthScreen.kt`, `ProfileScreen.kt`, `PetRepository.kt`, reglas `users/pets` | Sin prueba extremo a extremo; consola sin inspeccionar; rol admin sin procedimiento | Prueba de los 10 pasos de la Fase 2.2 con dos cuentas | Crítica | `data/repository/Auth*`, `Pet*`, `firestore.rules` | Registro → login → perfil → perro → logout → login → datos persisten; acceso cruzado rechazado | NO VERIFICABLE |
| P4 | Chat IA: interfaz, respuestas, temas, orientación de urgencias, acceso a urgencias, aviso | IMPLEMENTADO PARCIALMENTE | `AiAssistantRepository.kt`, `AiChatScreen.kt`, `EmergencyDetector.kt` (15 l.), `EmergencyDetectorTest.kt` | Detector de 15 líneas, probablemente por palabras clave; Firebase AI Logic requiere API habilitada y cuotas; sin prueba real | Auditar detector, verificar habilitación de Firebase AI Logic, probar respuesta real | Crítica | `domain/util/EmergencyDetector.kt`, `screens/aichat` | Pregunta normal → respuesta; frase de urgencia → aviso + acceso a veterinarias | NO VERIFICABLE |
| P5 | Geolocalización: permiso, mapa, nombre/teléfono/distancia, indicaciones, urgencias verificadas, búsqueda por ciudad | IMPLEMENTADO PARCIALMENTE | `VetClinicRepository.kt`, `VetMapScreen.kt`, `NominatimClient.kt`, `OverpassParser.kt`, `OverpassAndHaversineTest.kt` | OSM es un directorio verificable, pero teléfono y marca de urgencia dependen de etiquetas OSM que a menudo faltan: mostrar "no disponible" sin inventar. Overpass/Nominatim públicos tienen límites y exigen User-Agent propio | Probar con permiso concedido y denegado; revisar User-Agent y errores | Alta | `VetClinicRepository.kt`, `screens/map` | Mapa con clínicas reales de una ciudad; búsqueda por ciudad sin permiso | NO VERIFICABLE |
| P6 | Marketplace solo alimentos caninos: catálogo, categorías, imágenes, campos, búsqueda, filtros, inventario, CRUD admin | IMPLEMENTADO PARCIALMENTE | `ProductRepository.kt`, `StoreScreen.kt`, `ProductDogValidatorTest.kt`, reglas `products/categories` | Revisar que la semilla del catálogo no presente marcas/precios inventados como reales | Revisar semilla, validador y reglas; probar CRUD con admin y usuario normal | Alta | `ProductRepository.kt`, `StoreScreen.kt`, `firestore.rules` | Usuario normal rechazado por el servidor al escribir `products`; admin sí | NO VERIFICABLE |
| P7 | Carrito, cantidades, totales, envío, dirección, confirmación, historial, validar precios/stock | IMPLEMENTADO PARCIALMENTE | `CartRepository.kt`, `OrderRepository.kt`, `CartScreen.kt`, `CheckoutScreen.kt`, `CartCalculationsTest.kt`, `OrderMarketCorrectionTest.kt` | Validación de precio/stock hecha por el cliente dentro de una transacción; las reglas deben impedir manipulación (S3) | Revisar reglas de `orders`; documentar o crear `inventory`/`orderItems` | Crítica | `OrderRepository.kt`, `firestore.rules` | Pedido con stock insuficiente rechazado; total recalculado contra precios del servidor | NO VERIFICABLE |
| P8 | Pasarela: proveedor justificado, integración, estados, comprobante, sandbox | IMPLEMENTADO PARCIALMENTE | `PaymentGatewayRepository.kt` (66 l.), `docs/justificacion_pasarela.md`, `PaymentStatusStateTest.kt` | 66 líneas sugieren simulación local, no sandbox real. La guía acepta "simular o procesar", pero debe rotularse como simulación | Confirmar qué hace; rotular como simulación; sandbox real solo con credenciales | Alta | `PaymentGatewayRepository.kt`, `CheckoutScreen.kt` | Los tres estados producen el estado de pedido correcto y un comprobante | NO VERIFICABLE |
| P9 | Chat entre usuarios, independiente de la IA, con privacidad | IMPLEMENTADO PARCIALMENTE | `UserChatRepository.kt`, `UserChatScreen.kt`, reglas `conversations/messages`, `publicProfiles` | Sin prueba con dos cuentas; bloqueo/reporte sin verificar | Prueba con dos cuentas; pruebas de reglas en emulador | Alta | `UserChatRepository.kt`, `firestore.rules` | Mensaje de A llega a B; un tercero no lo lee | NO VERIFICABLE |
| P10 | Base de datos con 11 entidades + explicar integración IA y mapas | IMPLEMENTADO PARCIALMENTE | Reglas para `users`, `pets`, `publicProfiles`, `products`, `categories`, `carts`, `orders`, `payments`, `conversations`, `messages`; `firebase-blueprint.json`, `docs/modelo_datos.md` | Faltan como entidades explícitas **inventario** y **detalles de pedido** | Documentar inventario (¿`stock` en `products`?) y detalles (¿arreglo en `orders`?) o crearlos | Alta | `firestore.rules`, `docs/modelo_datos.md` | Diagrama que mapee las 11 entidades de la guía | IMPLEMENTADO PARCIALMENTE |
| P11 | Pruebas de funcionamiento (10 casos) | PENDIENTE | `docs/PRUEBAS.md`, 10 archivos de test | Sin evidencia de ejecución; build imposible hoy (E1–E3) | Resolver E1–E3, ejecutar `testDebugUnitTest`, pruebas manuales | Crítica | `src/test`, `docs/` | Salida real de Gradle y matriz de resultados | PENDIENTE |
| P12 | Documentación y sustentación | PENDIENTE | Hay `.md` en `docs/`; no hay `.docx`, capturas, APK ni presentación | Faltan los entregables finales | Fases 12–14 | Alta | `docs/`, `entregables/` | Archivos presentes y abren bien | PENDIENTE |

## 5b. Hallazgos verificados en dispositivo (2026-10-28, Redmi con Android 15)

Dispositivo físico `f051116f`, APK debug instalado con `adb`. Cuenta de prueba A registrada por el usuario.

| ID | Módulo | Resultado | Detalle |
|---|---|---|---|
| V1 | Inicio de sesión | OK | Pantalla con logo e identidad visual; sesión iniciada con la cuenta A |
| V2 | Calculadora de edad (P1) | OK | 2 años, perro mediano → 24 años humanos, etapa Adulto; coincide con la fórmula del código (curva no lineal, no 7×) |
| V3 | Detector de urgencias (P4) | OK | "¿Qué hago si mi perro comió chocolate?" muestra la tarjeta roja con "Ver Veterinarias" y "Llamar 123" |
| V4 | Chat IA — App Check | CORREGIDO (acción del usuario) | `Firebase App Check token is invalid`: el token de depuración del dispositivo no estaba registrado en la consola. Lo registró el usuario |
| V5 | Chat IA — modelo | CORREGIDO | `gemini-2.5-flash is no longer available to new users` → `gemini-3.8-flash` |
| V6 | Chat IA — respuesta | PENDIENTE | Con `gemini-3.8-flash` la llamada no devolvía respuesta en >3 min aunque la red del celular funciona (ping y DNS correctos). Se añadió un tiempo límite de 90 s y registros `DogCareAI` para diagnosticarlo |
| V7 | Chat IA — historial | CORREGIDO | Se perdía al cambiar de pestaña; ahora lo conserva `DogCareApp` |
| V8 | Veterinarias (P5) | PARCIAL | Mapa OpenStreetMap y búsqueda por ciudad devuelven clínicas reales (Nominatim como respaldo), pero Overpass agota el tiempo de 6 s y la consulta es lenta |
| V9 | Tienda (P6) | CIERRE DE LA APP | Al guardar un producto, la app se cerraba: `'id' was found from document products/…, cannot apply @DocumentId`. Afectaba a `Product`, `StoreCategory`, `Order`, `Pet`, `UserProfile`, `Conversation` y `ChatMessage` (documentos que guardan su propio `id`). Corregido quitando `@DocumentId`; pendiente de verificar en dispositivo |
| V10 | Tienda — formulario | OK | Campos de nombre, marca, categoría, precio, stock, etapa, presentación, descripción, imagen y visibilidad |

## 6. Incoherencias de la documentación previa

- `docs/MATRIZ_CUMPLIMIENTO.md` cita `MarketplaceScreen.kt` y `ChatScreen.kt`; los archivos reales son `StoreScreen.kt` y `AiChatScreen.kt`.
- Afirma "Reglas desplegadas y probadas" y "BUILD SUCCESSFUL" sin registro; hoy no hay Gradle para producirlo.
- Hay documentos duplicados (`DOCUMENTO_TECNICO.md` y `DOCUMENTO_TECNICO_DOGCARE.md`; `MANUAL_USUARIO.md` y `MANUAL_INSTALACION_Y_USO.md`). Consolidar en la Fase 12 sin perder contenido.

## 8. Próximos pasos

1. Resolver E1–E3 (Gradle 9.3.1, `platforms;android-36.1`, `local.properties`) para obtener un build real.
2. Confirmar la identidad del proyecto Firebase (`Jarvis` frente a `gen-lang-client-0991918834`) con sesión autorizada.
3. Revisar `firestore.rules` completo (con `firestore-rules-author`) y ejecutar `firestore.test.js` en el emulador.
4. Decidir el mecanismo del rol admin (campo `role` asignado desde la consola, ya soportado por las reglas, o custom claims con Admin SDK).
5. Prueba extremo a extremo de P3 con dos cuentas.

## 9. Pendientes que requieren al usuario

- Autorizar la descarga de Gradle y del SDK 36.1.
- Sesión de navegador con la consola Firebase o confirmar `firebase login` activo.
- Correo del administrador y de la cuenta de prueba; contraseñas por canal seguro (no se escribirán en archivos).
- Confirmar si se usará dispositivo físico (recomendado por los 4 GB de RAM).
