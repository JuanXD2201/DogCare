# DOCUMENTO TÉCNICO Y GUÍA ACADÉMICA — PROYECTO DOGCARE
**Asignatura:** Optativa II — Corte III  
**Aplicación:** DogCare (Cuidado Integral Canino)  
**Plataforma:** Android (API 24+ / Target SDK 36)  
**Lenguaje y Framework:** Kotlin 2.2, Jetpack Compose, Material Design 3  

---

## 1. Resumen Ejecutivo de la Solución

**DogCare** es una solución móvil integral diseñada para dueños de mascotas caninas. La aplicación expande y enriquece la funcionalidad original de cálculo de equivalencia de edad entre perros y humanos, evolucionando hacia un ecosistema completo que integra:

1. **Cálculo de Edad Canina y Perfiles:** Algoritmo científico no lineal basado en las curvas de la American Veterinary Medical Association (AVMA) diferenciado por tamaño y peso, con recomendaciones nutricionales, de salud y ejercicio según la etapa de vida (Cachorro, Adulto, Senior). Permite registrar y persistir múltiples perros por dueño.
2. **Autenticación Segura y Perfiles:** Integración de Firebase Authentication mediante Jetpack Credential Manager (`GetSignInWithGoogleOption`) y perfiles sincronizados en Cloud Firestore.
3. **Asistente Canino Inteligente con IA:** Chat conversacional con Firebase AI Logic (Gemini 2.5 Flash) dotado de instrucciones de sistema y protocolo estricto de banderas rojas/urgencias médicas, con aviso legal veterinario y acceso directo a veterinarias.
4. **Geolocalización y Directorio Veterinario (Google Maps Platform):** Mapa interactivo mediante Google Maps SDK for Android, geolocalización del usuario, búsqueda manual por ciudad o dirección, filtros de urgencias 24h, cálculo de distancias y navegación guiada abriendo Google Maps oficial.
5. **Tienda de Alimentos Caninos:** Catálogo exclusivo de nutrición canina clasificado en concentrados (alimento seco), comida húmeda y snacks, con control de inventario y panel administrativo para crear, editar y eliminar productos.
6. **Carrito de Compras y Control de Inventario:** Selección reactiva de cantidades con validación en tiempo real contra el stock real de Firestore, desglose de subtotal, costo de envío con umbral de gratuidad ($90.000 COP) y registro de dirección de entrega.
7. **Pasarela de Pagos Sandbox:** Simulación de pagos en entorno Sandbox (Wompi Bancolombia / Mercado Pago Test) con tarjetas de prueba preconfiguradas para validar flujos de Aprobado, Rechazado y Pendiente, emitiendo comprobantes digitales únicos y descontando inventario de forma idempotente.
8. **Chat de Comunidad entre Usuarios:** Mensajería instantánea independiente del chat de IA, permitiendo la comunicación privada en tiempo real entre dueños de perros a través de Firestore.
9. **Base de Datos y Seguridad:** Estructura documental NoSQL en Firestore con reglas de seguridad endurecidas (`firestore.rules`) bajo el principio de mínimo privilegio y aislamiento de datos por UID.
10. **Pruebas Automatizadas:** Suite de pruebas unitarias en JUnit (`DogCareUnitTest.kt`) que validan la matemática del cálculo de edad, la lógica del carrito, la pasarela de pagos y el formato de distancias.

---

## 2. Diagrama de Arquitectura de Software

La aplicación sigue una arquitectura **Clean Architecture + MVVM (Model-View-ViewModel)** reactiva con Flujos de Kotlin (`StateFlow` y `Flow`):

```
+-------------------------------------------------------------------------------+
|                           CAPA DE PRESENTACIÓN (UI)                           |
|  Jetpack Compose + Material 3 + DogCareTheme + Navigation Bars                |
|                                                                               |
|  [AgeCalculatorScreen]   [StoreScreen]   [AiChatScreen]   [VetMapScreen]      |
|  [CartScreen]     [CheckoutScreen]     [UserChatScreen]   [ProfileScreen]     |
+---------------------------------------+---------------------------------------+
                                        | Eventos de UI / Observación de Estado
                                        v
+-------------------------------------------------------------------------------+
|                           CAPA DE DOMINIO (DOMAIN)                            |
|  Casos de uso y modelos de negocio puros (independientes del framework)       |
|                                                                               |
|  - CalculateDogAgeUseCase (Fórmulas AVMA, Tallas y Etapas de Vida)            |
|  - Modelos: Pet, Product, CartItem, Order, VetClinic, AiChatMessage, UserChat |
+---------------------------------------+---------------------------------------+
                                        | Inyección / Invocación
                                        v
+-------------------------------------------------------------------------------+
|                            CAPA DE DATOS (DATA)                               |
|  Repositorios e Implementaciones de Integración                               |
|                                                                               |
|  - AuthRepository (Jetpack Credential Manager + Firebase Auth)                |
|  - PetRepository (Firestore /users/{uid}/pets)                                |
|  - ProductRepository (Firestore /products + Seed Inicial)                     |
|  - CartRepository (Gestión de Estado reactivo en memoria + Validaciones)      |
|  - OrderRepository (Firestore /orders)                                        |
|  - PaymentGatewayRepository (Sandbox Wompi / Mercado Pago Test)               |
|  - AiAssistantRepository (Firebase AI Logic Gemini + Triage Veterinario)       |
|  - VetClinicRepository (Google Maps Platform + Geocoder + Directorio Real)    |
|  - UserChatRepository (Firestore /conversations & /messages)                  |
+---------------------------------------+---------------------------------------+
                                        | Servicios Externos
                                        v
+-------------------------------------------------------------------------------+
|                           SERVICIOS Y CLOUD BACKEND                           |
|  - Google Cloud Platform / Firebase:                                          |
|      * Firebase Authentication (Google Sign-In)                               |
|      * Cloud Firestore (Base de Datos NoSQL multirregión)                     |
|      * Firebase App Check (Protección de API contra abusos)                   |
|      * Firebase AI Logic (Gemini 2.5 Flash)                                   |
|  - Google Maps Platform:                                                      |
|      * Maps SDK for Android & Google Play Services Location                   |
|      * Google Maps Navigation Intent (gratuito)                               |
|  - Pasarela Sandbox: Wompi / Mercado Pago Test Gateway                        |
+-------------------------------------------------------------------------------+
```

---

## 3. Modelo de Base de Datos (Cloud Firestore)

La base de datos utiliza el ID personalizado provisionado: `ai-studio-android-dogcare-10fa4e75-6cda-4ff9-a467-a58e8e73db8e`.

### Colecciones y Estructura Documental

1. **`/users/{userId}`:**
   - `userId`: String (UID de autenticación)
   - `displayName`: String
   - `email`: String
   - `photoUrl`: String?
   - `phone`: String?
   - `role`: "user" | "admin"
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

2. **`/users/{userId}/pets/{petId}`:**
   - `id`: String
   - `userId`: String (coincide con `request.auth.uid`)
   - `name`: String
   - `breed`: String
   - `sizeCategory`: "small" | "medium" | "large" | "giant"
   - `ageYears`: Int
   - `ageMonths`: Int
   - `weightKg`: Double
   - `humanAgeYears`: Double
   - `notes`: String
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

3. **`/products/{productId}`:**
   - `id`: String
   - `name`: String
   - `brand`: String
   - `category`: "dry_food" | "wet_food" | "snacks"
   - `lifeStage`: "puppy" | "adult" | "senior" | "all_stages"
   - `presentation`: String
   - `priceCop`: Double
   - `stock`: Int
   - `description`: String
   - `imageUrl`: String
   - `active`: Boolean
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

4. **`/orders/{orderId}`:**
   - `id`: String
   - `userId`: String (propietario de la orden)
   - `customerName`: String
   - `deliveryAddress`: String
   - `customerPhone`: String
   - `items`: List<Map> (productId, productName, unitPriceCop, quantity, subtotalCop)
   - `subtotalCop`: Double
   - `shippingCop`: Double
   - `totalCop`: Double
   - `paymentStatus`: "PENDING" | "APPROVED" | "DECLINED" | "REJECTED" | "ERROR"
   - `paymentReference`: String
   - `paymentMethod`: String
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

5. **`/payments/{paymentId}` (Colección de Comprobantes de Pago Inmutables):**
   - `id`: String
   - `orderId`: String
   - `userId`: String
   - `amountCop`: Double
   - `provider`: "PASARELA_SANDBOX"
   - `status`: "PENDING" | "APPROVED" | "DECLINED" | "REJECTED" | "ERROR"
   - `paymentReference`: String
   - `createdAt`: Timestamp

6. **`/publicProfiles/{userId}` (Perfiles Públicos de Comunidad):**
   - `userId`: String
   - `displayName`: String
   - `photoUrl`: String
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

> **Nota de Seguridad sobre Confirmación de Pagos en Producción:** En este proyecto de entrega académica, la confirmación del pago en el entorno sandbox se procesa desde el cliente Android mediante escrituras en `/orders` y `/payments` con reglas de inmutabilidad estricta. En una arquitectura de producción comercial, la confirmación de la transacción debe realizarse obligatoriamente mediante un **Webhook en un servidor backend o Cloud Function** que verifique la firma HMAC transmitida por la pasarela de pagos (riesgo aceptado para la entrega académica).

5. **`/conversations/{conversationId}`:**
   - `id`: String
   - `participantUids`: List<String> (mínimo 2 usuarios)
   - `participantNames`: Map<String, String>
   - `lastMessageText`: String
   - `lastSenderUid`: String
   - `lastMessageTimestamp`: Timestamp
   - `createdAt`: Timestamp
   - `updatedAt`: Timestamp

6. **`/conversations/{conversationId}/messages/{messageId}`:**
   - `id`: String
   - `conversationId`: String
   - `senderUid`: String
   - `senderName`: String
   - `text`: String
   - `createdAt`: Timestamp

---

## 4. Descripción y Evidencias de los Módulos

### Módulo 1: Calculadora de Edad Canina y Mis Perros (Fase 1)
- **Lógica:** Implementa la curva no lineal de la AVMA. El primer año equivale a 15 años humanos, el segundo suma 9 años (alcanzando 24 años a los 2 años caninos), y los años posteriores avanzan según el tamaño del perro:
  - Pequeño (<10 kg): +4 años humanos por año.
  - Mediano (10-22 kg): +5 años humanos por año.
  - Grande (23-40 kg): +6 años humanos por año.
  - Gigante (>40 kg): +7.5 años humanos por año.
- **Etapa de vida:** Determina si el perro es Cachorro (<1 año), Adulto o Senior (alcanzado a los 6 años en gigantes, 7 en grandes, 8 en medianos y 10 en pequeños).
- **Consejos específicos:** Asigna automáticamente recomendaciones nutricionales, de vacunación/chequeo y de ejercicio físico acordes a su edad.
- **Persistencia:** Permite guardar el perfil del perro en Firestore y muestra la lista de perros registrados con opción de recálculo y eliminación.

### Módulo 2: Registro, Inicio de Sesión y Perfil (Fase 2)
- **Autenticación moderna:** Google Sign-In con `androidx.credentials:credentials:1.5.0` y `com.google.android.libraries.identity.googleid:googleid:1.1.1`.
- **Aislamiento:** Cada usuario accede exclusivamente a su propia información de mascotas y órdenes mediante reglas de seguridad evaluadas en el servidor.
- **Modo Demostración:** Incluye acceso de exploración que permite al docente o evaluador probar la interfaz inmediatamente sin requerir una cuenta obligatoria inicial.

### Módulo 3: Asistente Canino con IA (Fase 3)
- **Integración:** Firebase AI Logic (`com.google.firebase:firebase-ai`) con modelo Gemini 2.5 Flash y App Check configurado en `MainActivity.onCreate()`.
- **Seguridad Veterinaria:** Prompt de sistema especializado que actúa como educador de bienestar canino y contiene un detector de banderas rojas de emergencia (torsión gástrica, ingestión de chocolate/tóxicos, convulsiones, traumatismos, dificultad respiratoria).
- **Respuesta de Emergencia:** Al detectar un síntoma de alarma médica, emite un aviso destacado en rojo y un botón directo que traslada al usuario al mapa de clínicas veterinarias 24h.

### Módulo 4: Google Maps y Centros Veterinarios (Fase 4)
- **Proveedor obligatorio:** Google Maps Platform oficial mediante `play-services-maps:19.0.0` y `play-services-location:21.3.0`.
- **Permisos de ubicación:** Solicita permisos en tiempo de ejecución (`ACCESS_FINE_LOCATION`) con explicación clara; si es denegado, ofrece búsqueda manual por ciudad o dirección mediante `Geocoder`.
- **Visualización:** Mapa interactivo con marcadores diferenciados (azul para veterinarias estándar y rojo para urgencias 24h).
- **Ficha completa:** Nombre, dirección real, teléfono con marcado directo, horario y distancia calculada en tiempo real.
- **Navegación:** Botón "Cómo llegar" que invoca el Intent nativo de Google Maps (`google.navigation:q=lat,lng`), asegurando una experiencia óptima y sin costos comerciales de enrutamiento por carretera.

### Módulo 5: Tienda de Alimentos Caninos (Fase 5)
- **Especialización canina:** Exclusivamente concentrados, comida húmeda y snacks caninos.
- **Filtros rápidos:** Categoría (Seco, Húmedo, Snacks) y etapa de vida (Cachorro, Adulto, Senior).
- **Semilla inicial:** Auto-poblado seguro de productos certificados en Firestore si la colección se encuentra vacía.
- **Herramientas de administración:** Diálogo para crear nuevos productos, editar precios/stock y eliminar productos del catálogo.

### Módulo 6: Carrito de Compras (Fase 6)
- **Control de stock:** Impide añadir o solicitar más unidades de las disponibles en el inventario de Firestore.
- **Cálculo transparente:** Subtotal por ítem, subtotal global, costo de envío ($8.500 COP) y política de envío gratis para compras superiores a $90.000 COP.
- **Registro de entrega:** Formulario con dirección física y teléfono de contacto para el despacho.

### Módulo 7: Pasarela de Pagos Sandbox (Fase 7)
- **Entorno de pruebas:** Compatibilidad con proveedores líderes en Colombia (Wompi y Mercado Pago Test).
- **Casos de prueba inmediatos:** Botones para probar pagos Aprobados (`4242...`), Rechazados por fondos (`4000...0002`) y En Revisión (`4000...0004`).
- **Comprobante digital:** Genera un voucher con ID de transacción, código de autorización, fecha/hora y número de tarjeta enmascarado.
- **Idempotencia:** Previene duplicidad de pagos y actualiza la orden en Firestore a su estado final.

### Módulo 8: Chat de Comunidad (Fase 8)
- **Independiente de la IA:** Canal de comunicación P2P exclusivo entre usuarios registrados.
- **Directorio de dueños:** Lista de contactos de la comunidad para iniciar nuevas conversaciones.
- **Sincronización en tiempo real:** Uso de snapshot listeners de Firestore para recepción instantánea de mensajes.

---

## 5. Manual de Instalación y Configuración

1. **Clonar / Abrir el proyecto:** Abrir la carpeta raíz en **Android Studio Ladybug (2024.2+)** o superior.
2. **Requisitos de JDK:** Java 17 o Java 21 configurado en Gradle JDK.
3. **Claves de API y Secretos:**
   - Crear un archivo `.env` en la raíz del proyecto (basado en `.env.example`).
   - Configurar `MAPS_API_KEY=AIzaSy...` con la clave de Google Maps Platform autorizada para Android.
   - Configurar `GEMINI_API_KEY=AIzaSy...` con la clave de la API de Gemini.
4. **Compilación y Ejecución:**
   - Ejecutar la tarea de Gradle: `gradle assembleDebug`.
   - El APK resultante se genera en `app/build/outputs/apk/debug/app-debug.apk`.
   - Ejecutar las pruebas unitarias: `gradle :app:testDebugUnitTest`.

---

## 6. Reporte de Pruebas Automatizadas

Se diseñó la clase de prueba `DogCareUnitTest.kt` en `app/src/test/java/com/example/DogCareUnitTest.kt`:
- **testAgeCalculation_firstYear_isPuppy:** Verifica que 1 año canino equivalga a 15 años humanos y etapa Adulto.
- **testAgeCalculation_sixMonths_isPuppy:** Verifica que 6 meses equivalgan a 7.5 años y etapa Cachorro con consejos de DHA.
- **testAgeCalculation_secondYear_reachesTwentyFour:** Verifica que 2 años caninos alcancen exactamente 24 años humanos.
- **testAgeCalculation_differentialBySize_afterTwoYears:** Verifica el multiplicador diferenciado para razas pequeñas (4x) vs gigantes (7.5x).
- **testAgeCalculation_seniorStage_giantVsSmall:** Verifica la transición temprana a Senior en razas gigantes a partir de los 6 años.
- **testCart_addItem_calculatesSubtotalAndShipping:** Verifica el subtotal de productos y aplicación de costo de envío estándar.
- **testCart_freeShipping_thresholdExceeded:** Verifica la gratuidad del envío en compras de $120.000 COP (> $90.000 COP).
- **testCart_exceedingStock_isRejected:** Verifica el rechazo de compras que superen las existencias de inventario.
- **testPaymentGateway_approvedOutcome:** Verifica la emisión de código de autorización en transacciones aprobadas de prueba.
- **testPaymentGateway_rejectedOutcome:** Verifica el código de rechazo `REJ-00` en transacciones simuladas fallidas.
- **testVetClinic_distanceFormatting:** Verifica la precisión en el cálculo y formato de distancias a establecimientos.
