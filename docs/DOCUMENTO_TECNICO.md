# Documento Técnico DogCare - Plataforma Integral de Cuidado Canino

**Proyecto:** DogCare (Taller II, Corte III, Optativa II)  
**Paquete Android:** `com.aistudio.dogcare.kprmdx`  
**Base de Datos Firestore:** `ai-studio-android-dogcare-10fa4e75-6cda-4ff9-a467-a58e8e73db8e`  
**Arquitectura:** MVVM + Clean Architecture + Jetpack Compose  

---

## 1. Resumen Ejecutivo
DogCare es una aplicación móvil nativa en Android desarrollada en Kotlin y Jetpack Compose orientada al bienestar integral de los caninos. La plataforma opera sin dependencia de Google Cloud Billing mediante tecnologías de mapas libres (OpenStreetMap / osmdroid + Overpass API + Nominatim), inteligencia artificial para consultas asistenciales con Firebase AI Logic (Gemini 2.5 Flash), comercio electrónico exclusivo para productos caninos con verificación de stock y precios previa a la compra, pasarela de pago simulada Sandbox (estilo Wompi Colombia), y chat entre usuarios registrado en perfiles públicos de Firestore.

---

## 2. Tecnologías y Stack
* **Lenguaje:** Kotlin 2.2.10
* **Interfaz de Usuario:** Jetpack Compose (Material Design 3 con paleta verde esmeralda `#0F766E` y naranja cálido `#F97316`)
* **Base de Datos & Auth:** Firebase Firestore (Base nombrada dedicada) & Firebase Authentication (Google Sign-In + Email/Password)
* **Inteligencia Artificial:** Firebase AI Logic (Gemini API integrado sin claves expuestas en el cliente APK)
* **Mapas & Geolocalización:** `osmdroid-android` 6.1.18 (OpenStreetMap) + Overpass API (`node["amenity"="veterinary"](around:5000)`) + Nominatim API (Búsqueda por ciudad con rate limiting de 1 req/s)
* **Pruebas Unitarias:** Robolectric 4.16.1 + JUnit 4

---

## 3. Módulos del Sistema

### 3.1 Autenticación y Gestión Multimascota
* Soporta registro e inicio de sesión con correo/contraseña y Google Sign-In mediante Jetpack Credential Manager.
* Permite agregar múltiples caninos por usuario especificando nombre, edad (años y meses), raza, tamaño (`small`, `medium`, `large`, `giant`) y características.
* Integra la calculadora de edad canina basada en curvas fisiológicas no lineales AVMA y ofrece recomendaciones nutricionales y de salud personalizadas según la etapa de vida (Cachorro, Adulto, Senior).

### 3.2 Mapa de Veterinarias Libre de Billing (OpenStreetMap)
* Sustituye Google Maps por `osmdroid` para eliminar costos de licencias.
* Utiliza Overpass API para consultar veterinarias reales dentro de un radio de 5 km alrededor de las coordenadas del usuario.
* Muestra distintivo de urgencia **"24H VERIFICADO"** únicamente cuando las etiquetas de OSM indican `emergency=yes` o `opening_hours=24/7`. Caso contrario, se indica "Sin verificar".
* Integra botón "Cómo llegar" mediante Intent a Google Maps (`https://www.google.com/maps/dir/?api=1&destination=lat,lng`) y enlace directo al nodo en OpenStreetMap.
* Ante ausencia de GPS o negativa de permisos, habilita geocodificación de ciudades con Nominatim API.

### 3.3 Asistente Canino IA con Detector de Urgencias
* Configurado mediante Prompt de Sistema restringido exclusivamente a la salud preventiva y cuidado canino en español, sin emitir diagnósticos médicos.
* Incluye un detector automático de palabras clave de emergencia (envenenamiento, sangrado, convulsiones, asfixia, etc.) que despliega una tarjeta de alerta inmediata con botones directos para llamar al 123 o filtrar clínicas de urgencia en el mapa.
* Muestra el aviso legal permanente: *"La IA no sustituye la valoración de un médico veterinario"*.

### 3.4 Tienda y Carrito con Relectura Previa
* Catálogo de productos filtrado exclusivamente para perros (`species == 'dog'`).
* Panel de administración restringido (`role == 'admin'`) para crear, editar y eliminar productos.
* **Relectura Transaccional:** Antes de confirmar un pedido, la aplicación relee el stock y precio actual desde Firestore. Si el producto sufrió variaciones de precio o agotamiento de stock, la transacción se cancela y se alerta al usuario.

### 3.5 Pasarela de Pagos Sandbox
* Simulador estilo Wompi/MercadoPago Test con tarjetas preconfiguradas para probar respuestas `APPROVED`, `DECLINED` y `PENDING`.
* Emisión de comprobantes inmutables en `/payments` vinculados al pedido en `/orders`.

### 3.6 Chat entre Usuarios
* Directorio de contactos alimentado desde la colección pública `/publicProfiles/{uid}`.
* Canales P2P independientes del chat de IA, con mensajería en tiempo real mediante Snapshot Listeners de Firestore.

---

## 4. Matriz de Seguridad en Firestore
Las reglas definidas en `firestore.rules` aseguran:
1. Privacidad de `/users/{uid}` accesible solo por el dueño o administrador.
2. Perfiles públicos `/publicProfiles/{uid}` con campos restringidos (`userId`, `displayName`, `photoUrl`, `createdAt`, `updatedAt`).
3. Reducción de stock atómica en `/products` condicionada a que la orden referenciada en `lastOrderId` sea creada en la misma escritura (`!exists` + `existsAfter`) y coincida con el mapa de `quantities`.
4. Creación inmutable de registros en `/payments`.
