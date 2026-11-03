# Manual de Instalación y Uso - DogCare

## 1. Requisitos del Sistema
* **Android OS:** Android 7.0 (API nivel 24) o superior.
* **Espacio en Disco:** 80 MB.
* **Conectividad:** Conexión a Internet para sincronización con Firestore, Overpass API y Gemini IA.
* **Permisos del Dispositivo:** Ubicación (`ACCESS_FINE_LOCATION`) para geolocalización de veterinarias en OpenStreetMap.

---

## 2. Instalación
1. Abra el proyecto en Android Studio (Hedgehog o superior) con Kotlin 2.2.10.
2. Compile la aplicación ejecutando la tarea de Gradle `:app:assembleDebug`.
3. Instale el APK generado en su dispositivo o en el emulador de Android Studio.

---

## 3. Guía de Uso por Módulos

### 3.1 Módulo de Autenticación y Registro de Perros
* Al abrir la aplicación, inicie sesión con correo/contraseña o mediante el botón **"Continuar con Google"**.
* En la pantalla principal (**Mis Caninos**), presione el botón `+` para agregar sus mascotas indicando nombre, edad, raza y tamaño.
* La aplicación calculará la edad humana equivalente de su perro y mostrará recomendaciones de nutrición y salud geriátrica/cachorro.

### 3.2 Módulo de Mapa de Veterinarias Libre de Billing (osmdroid)
* Acceda a la pestaña **Mapa**.
* Si concede permisos de GPS, la aplicación consultará en tiempo real mediante Overpass API las veterinarias reales en un radio de 5 km.
* Si niega los permisos, puede escribir el nombre de su ciudad (ej. *Bogotá*, *Medellín*, *Cali*) en la barra superior; el sistema utilizará Nominatim para geocodificar las coordenadas de la ciudad de forma gratuita.
* Presione **"Solo Urgencias 24h"** para filtrar hospitales veterinarios con atención nocturna verificada.
* Presione **"Cómo llegar"** para abrir las indicaciones en Google Maps sin requerir API Key.

### 3.3 Módulo de Asistente IA Canino
* Acceda a la pestaña **Asistente IA**.
* Realice preguntas sobre nutrición, adiestramiento o cuidados preventivos para su perro.
* Si escribe síntomas de urgencia (ej. *"mi perro tomó veneno"*, *"está sangrando mucho"*), el sistema activará de inmediato la tarjeta roja de **Atención Veterinaria Inmediata** con acceso rápido a llamadas y al mapa de urgencias.

### 3.4 Módulo de Tienda, Carrito y Pasarela Sandbox
* Explore el catálogo de alimentos y accesorios caninos.
* Agregue ítems al carrito y continúe al Checkout.
* Antes de confirmar el pago, la aplicación revalida automáticamente en Firestore que los precios y el stock no hayan cambiado.
* Utilice los botones preconfigurados de prueba de la pasarela Sandbox Wompi para simular pagos Aprobados, Rechazados o Pendientes.
