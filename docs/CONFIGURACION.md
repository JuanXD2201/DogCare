# Guía de Configuración y Variables de Entorno - DogCare

## 1. Configuración de Variables de Entorno (`.env` / `.env.example`)
La aplicación utiliza el plugin Gradle **BuildConfig** y el plugin de Secretos para gestionar configuraciones sensibles.

### Archivo `.env.example`
```env
# Gemini API Key (opcional; se obtiene en la consola de Google)
GEMINI_API_KEY=

# Las consultas de geolocalización de mapas utilizan OpenStreetMap / Nominatim / Overpass API (No requiere API Key privada)
